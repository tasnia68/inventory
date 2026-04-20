package com.inventory.system.service;

import com.inventory.system.common.entity.StorefrontDomain;
import com.inventory.system.common.entity.Tenant;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.config.tenant.TenantContext;
import com.inventory.system.payload.StorefrontDomainContextDto;
import com.inventory.system.payload.StorefrontDomainDto;
import com.inventory.system.payload.StorefrontDomainRequest;
import com.inventory.system.repository.StorefrontDomainRepository;
import com.inventory.system.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StorefrontDomainServiceImpl implements StorefrontDomainService {

    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "::1");

    private final StorefrontDomainRepository storefrontDomainRepository;
    private final TenantRepository tenantRepository;

    @Value("${app.storefront.domains.platform-base-domain:localhost}")
    private String platformBaseDomain;

    @Value("${app.storefront.domains.admin-domain:localhost}")
    private String adminDomain;

    @Value("${app.storefront.domains.verification-target:localhost}")
    private String verificationTarget;

    @Override
    @Transactional(readOnly = true)
    public StorefrontDomainContextDto getDomainContextForCurrentTenant() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return new StorefrontDomainContextDto(null, null, null, null, verificationTarget, List.of());
        }

        Optional<Tenant> tenant = resolveCurrentTenant();
        if (tenant.isEmpty()) {
            return new StorefrontDomainContextDto(null, null, null, null, verificationTarget, List.of());
        }

        String fallbackHost = buildFallbackHost(tenant.get());
        String fallbackUrl = fallbackHost != null ? "https://" + fallbackHost : null;
        List<StorefrontDomainDto> domains = storefrontDomainRepository.findByTenantIdOrderByPrimaryDescHostnameAsc(tenant.get().getId())
                .stream()
                .map(this::toDto)
                .toList();
        StorefrontDomainDto primary = domains.stream()
                .filter(StorefrontDomainDto::isPrimary)
                .findFirst()
                .orElse(null);

        return new StorefrontDomainContextDto(
                fallbackHost,
                fallbackUrl,
                primary != null ? primary.getHostname() : fallbackHost,
                primary != null ? "https://" + primary.getHostname() : fallbackUrl,
                verificationTarget,
                domains
        );
    }

    @Override
    @Transactional
    public StorefrontDomainDto addDomain(StorefrontDomainRequest request) {
        Tenant tenant = resolveCurrentTenant()
                .orElseThrow(() -> new BadRequestException("Tenant context is required to add a storefront domain"));
        String hostname = normalizeHostname(request.getHostname());
        validateManagedDomain(hostname);

        storefrontDomainRepository.findFirstByHostnameIgnoreCase(hostname).ifPresent(existing -> {
            if (!existing.getTenant().getId().equals(tenant.getId())) {
                throw new BadRequestException("This domain is already assigned to another tenant");
            }
            throw new BadRequestException("This domain is already configured");
        });

        StorefrontDomain domain = new StorefrontDomain();
        domain.setTenant(tenant);
        domain.setHostname(hostname);
        domain.setPrimary(false);
        domain.setActive(false);
        domain.setVerificationStatus(StorefrontDomain.VerificationStatus.PENDING);
        domain.setTlsStatus(StorefrontDomain.TlsStatus.PENDING);
        domain.setLastError(null);

        return toDto(storefrontDomainRepository.save(domain));
    }

    @Override
    @Transactional
    public StorefrontDomainDto verifyDomain(UUID domainId) {
        StorefrontDomain domain = getTenantDomain(domainId);
        domain.setVerificationCheckedAt(LocalDateTime.now());

        try {
            if (!resolvesToVerificationTarget(domain.getHostname())) {
                domain.setVerificationStatus(StorefrontDomain.VerificationStatus.FAILED);
                domain.setTlsStatus(StorefrontDomain.TlsStatus.FAILED);
                domain.setLastError("The domain does not resolve to the configured storefront target yet.");
                return toDto(storefrontDomainRepository.save(domain));
            }

            domain.setVerificationStatus(StorefrontDomain.VerificationStatus.VERIFIED);
            domain.setLastError(null);
            domain.setTlsStatus(checkHttpsReady(domain.getHostname())
                    ? StorefrontDomain.TlsStatus.ISSUED
                    : StorefrontDomain.TlsStatus.READY);
            return toDto(storefrontDomainRepository.save(domain));
        } catch (Exception exception) {
            domain.setVerificationStatus(StorefrontDomain.VerificationStatus.FAILED);
            domain.setTlsStatus(StorefrontDomain.TlsStatus.FAILED);
            domain.setLastError(exception.getMessage());
            return toDto(storefrontDomainRepository.save(domain));
        }
    }

    @Override
    @Transactional
    public StorefrontDomainDto activateDomain(UUID domainId) {
        StorefrontDomain domain = getTenantDomain(domainId);
        if (domain.getVerificationStatus() != StorefrontDomain.VerificationStatus.VERIFIED) {
            throw new BadRequestException("Verify the domain before activating it");
        }

        List<StorefrontDomain> domains = storefrontDomainRepository.findByTenantIdOrderByPrimaryDescHostnameAsc(domain.getTenant().getId());
        for (StorefrontDomain existing : domains) {
            existing.setPrimary(false);
            existing.setActive(false);
        }

        domain.setPrimary(true);
        domain.setActive(true);
        domain.setActivatedAt(LocalDateTime.now());
        if (domain.getTlsStatus() == StorefrontDomain.TlsStatus.PENDING) {
            domain.setTlsStatus(StorefrontDomain.TlsStatus.READY);
        }

        storefrontDomainRepository.saveAll(domains);
        return toDto(storefrontDomainRepository.save(domain));
    }

    @Override
    @Transactional
    public void removeDomain(UUID domainId) {
        StorefrontDomain domain = getTenantDomain(domainId);
        storefrontDomainRepository.delete(domain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> resolveTenantIdForOrigin(String origin) {
        String hostname = normalizeOriginHost(origin);
        if (hostname == null) {
            return Optional.empty();
        }
        return resolveTenantIdForHost(hostname);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> resolveTenantIdForHost(String host) {
        String hostname = normalizeHostOnly(host);
        if (hostname == null) {
            return Optional.empty();
        }

        Optional<StorefrontDomain> customDomain = storefrontDomainRepository.findFirstByHostnameIgnoreCaseAndActiveTrue(hostname);
        if (customDomain.isPresent()) {
            return Optional.of(customDomain.get().getTenant().getId().toString());
        }

        String baseDomain = normalizedPlatformBaseDomain();
        if (baseDomain != null && hostname.endsWith("." + baseDomain)) {
            String subdomain = hostname.substring(0, hostname.length() - baseDomain.length() - 1);
            if (!subdomain.isBlank()) {
                return tenantRepository.findBySubdomain(subdomain)
                        .map(Tenant::getId)
                        .map(UUID::toString);
            }
        }

        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDomainAllowedForCaddy(String host) {
        String hostname = normalizeHostOnly(host);
        if (hostname == null) {
            return false;
        }
        if (hostname.equals(normalizeHostOnly(adminDomain))) {
            return true;
        }
        return resolveTenantIdForHost(hostname).isPresent();
    }

    @Override
    public boolean isLocalDevelopmentHost(String host) {
        String hostname = normalizeHostOnly(host);
        return hostname != null && LOCAL_HOSTS.contains(hostname);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getPrimaryStorefrontUrlForCurrentTenant() {
        StorefrontDomainContextDto context = getDomainContextForCurrentTenant();
        return Optional.ofNullable(context.getPrimaryUrl());
    }

    private Optional<Tenant> resolveCurrentTenant() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return Optional.empty();
        }
        try {
            return tenantRepository.findById(UUID.fromString(tenantId));
        } catch (IllegalArgumentException exception) {
            return tenantRepository.findBySubdomainIgnoreCase(tenantId.trim());
        }
    }

    private StorefrontDomain getTenantDomain(UUID domainId) {
        StorefrontDomain domain = storefrontDomainRepository.findById(domainId)
                .orElseThrow(() -> new ResourceNotFoundException("Storefront domain", "id", domainId));
        Optional<Tenant> currentTenant = resolveCurrentTenant();
        if (currentTenant.isEmpty() || !domain.getTenant().getId().equals(currentTenant.get().getId())) {
            throw new ResourceNotFoundException("Storefront domain", "id", domainId);
        }
        return domain;
    }

    private String buildFallbackHost(Tenant tenant) {
        String baseDomain = normalizedPlatformBaseDomain();
        if (tenant == null || tenant.getSubdomain() == null || tenant.getSubdomain().isBlank() || baseDomain == null) {
            return null;
        }
        return tenant.getSubdomain().trim().toLowerCase(Locale.ROOT) + "." + baseDomain;
    }

    private String normalizeHostname(String hostname) {
        String normalized = normalizeHostOnly(hostname);
        if (normalized == null) {
            throw new BadRequestException("A valid hostname is required");
        }
        return normalized;
    }

    private String normalizeHostOnly(String host) {
        if (host == null || host.isBlank()) {
            return null;
        }
        String normalized = host.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            normalized = URI.create(normalized).getHost();
        }
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        int portSeparator = normalized.indexOf(':');
        if (portSeparator >= 0) {
            normalized = normalized.substring(0, portSeparator);
        }
        return normalized;
    }

    private String normalizeOriginHost(String origin) {
        if (origin == null || origin.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(origin.trim());
            return normalizeHostOnly(uri.getHost());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private String normalizedPlatformBaseDomain() {
        String baseDomain = normalizeHostOnly(platformBaseDomain);
        return baseDomain == null || LOCAL_HOSTS.contains(baseDomain) ? baseDomain : baseDomain;
    }

    private void validateManagedDomain(String hostname) {
        if (LOCAL_HOSTS.contains(hostname)) {
            throw new BadRequestException("Localhost cannot be added as a managed storefront domain");
        }
        if (hostname.equals(normalizeHostOnly(adminDomain))) {
            throw new BadRequestException("The admin domain is reserved and cannot be assigned to a storefront");
        }
        String baseDomain = normalizedPlatformBaseDomain();
        if (baseDomain != null && (hostname.equals(baseDomain) || hostname.endsWith("." + baseDomain))) {
            throw new BadRequestException("Platform-managed fallback hosts are derived automatically and should not be added as custom domains");
        }
    }

    private boolean resolvesToVerificationTarget(String hostname) throws Exception {
        Set<String> hostnameIps = resolveIps(hostname);
        Set<String> targetIps = resolveIps(verificationTarget);
        hostnameIps.retainAll(targetIps);
        return !hostnameIps.isEmpty();
    }

    private Set<String> resolveIps(String hostname) throws Exception {
        return Arrays.stream(InetAddress.getAllByName(normalizeHostOnly(hostname)))
                .map(InetAddress::getHostAddress)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean checkHttpsReady(String hostname) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://" + hostname))
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() < 500;
        } catch (Exception exception) {
            return false;
        }
    }

    private StorefrontDomainDto toDto(StorefrontDomain domain) {
        return new StorefrontDomainDto(
                domain.getId(),
                domain.getHostname(),
                domain.isPrimary(),
                domain.isActive(),
                domain.getVerificationStatus().name(),
                domain.getTlsStatus().name(),
                domain.getVerificationCheckedAt(),
                domain.getActivatedAt(),
                domain.getLastError()
        );
    }
}
