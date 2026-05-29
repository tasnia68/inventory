package com.inventory.system.service.impl;

import com.inventory.system.common.entity.Customer;
import com.inventory.system.common.entity.Discount;
import com.inventory.system.common.entity.ReferralAttribution;
import com.inventory.system.common.entity.ReferralAttributionStatus;
import com.inventory.system.common.entity.ReferralCode;
import com.inventory.system.common.entity.ReferralCodeStatus;
import com.inventory.system.common.entity.ReferralProgram;
import com.inventory.system.common.entity.ReferralProgramStatus;
import com.inventory.system.common.exception.BadRequestException;
import com.inventory.system.common.exception.ResourceNotFoundException;
import com.inventory.system.config.tenant.TenantContext;
import com.inventory.system.payload.ApplyReferralRequest;
import com.inventory.system.payload.ReferralAttributionDto;
import com.inventory.system.payload.ReferralCodeDto;
import com.inventory.system.payload.ReferralProgramDto;
import com.inventory.system.payload.UpsertReferralProgramRequest;
import com.inventory.system.repository.CustomerRepository;
import com.inventory.system.repository.DiscountRepository;
import com.inventory.system.repository.ReferralAttributionRepository;
import com.inventory.system.repository.ReferralCodeRepository;
import com.inventory.system.repository.ReferralProgramRepository;
import com.inventory.system.service.ReferralService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReferralServiceImpl implements ReferralService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final ReferralProgramRepository programRepository;
    private final ReferralCodeRepository codeRepository;
    private final ReferralAttributionRepository attributionRepository;
    private final DiscountRepository discountRepository;
    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public ReferralProgramDto upsertProgram(UpsertReferralProgramRequest req) {
        String tenantId = TenantContext.requireTenantId();
        ReferralProgram program = programRepository.findFirstByTenantId(tenantId).orElseGet(ReferralProgram::new);
        program.setName(req.name());
        program.setStatus(req.status() != null ? req.status() : ReferralProgramStatus.PAUSED);
        program.setReferrerDiscount(resolveDiscount(req.referrerDiscountId()));
        program.setRefereeDiscount(resolveDiscount(req.refereeDiscountId()));
        program.setRewardTrigger(req.rewardTrigger());
        program.setMinRefereeOrderAmount(req.minRefereeOrderAmount());
        program.setRefereeNthOrder(req.refereeNthOrder());
        program.setMaxReferralsPerCustomer(req.maxReferralsPerCustomer());
        program.setDescription(req.description());
        program = programRepository.save(program);
        return toDto(program);
    }

    @Override
    @Transactional(readOnly = true)
    public ReferralProgramDto getProgram() {
        String tenantId = TenantContext.requireTenantId();
        return programRepository.findFirstByTenantId(tenantId).map(this::toDto).orElse(null);
    }

    @Override
    @Transactional
    public ReferralCodeDto getOrCreateCodeForCustomer(UUID customerId) {
        String tenantId = TenantContext.requireTenantId();
        ReferralProgram program = programRepository.findFirstByTenantId(tenantId)
                .orElseThrow(() -> new BadRequestException("Referral program not configured"));
        if (program.getStatus() != ReferralProgramStatus.ACTIVE) {
            throw new BadRequestException("Referral program is not active");
        }
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        return codeRepository.findByProgramIdAndCustomerId(program.getId(), customerId)
                .map(this::toCodeDto)
                .orElseGet(() -> {
                    ReferralCode code = new ReferralCode();
                    code.setProgram(program);
                    code.setCustomer(customer);
                    code.setCode(generateUniqueCode());
                    code.setStatus(ReferralCodeStatus.ACTIVE);
                    return toCodeDto(codeRepository.save(code));
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferralCodeDto> getCodesForCustomer(UUID customerId) {
        return codeRepository.findByCustomerId(customerId).stream().map(this::toCodeDto).toList();
    }

    @Override
    @Transactional
    public ReferralAttributionDto attribute(ApplyReferralRequest request) {
        ReferralCode code = codeRepository.findByCodeIgnoreCase(request.referralCode())
                .orElseThrow(() -> new ResourceNotFoundException("Referral code not found"));
        if (code.getStatus() != ReferralCodeStatus.ACTIVE) {
            throw new BadRequestException("Referral code not active");
        }
        if (request.refereeCustomerId() == null) {
            throw new BadRequestException("Referee customer required");
        }
        if (code.getCustomer().getId().equals(request.refereeCustomerId())) {
            throw new BadRequestException("Cannot refer yourself");
        }

        ReferralProgram program = code.getProgram();
        if (program.getMaxReferralsPerCustomer() != null) {
            long existing = attributionRepository.countByReferralCodeId(code.getId());
            if (existing >= program.getMaxReferralsPerCustomer()) {
                throw new BadRequestException("Referral cap reached for this code");
            }
        }

        ReferralAttribution existing = attributionRepository.findByRefereeCustomerId(request.refereeCustomerId())
                .orElse(null);
        if (existing != null) {
            return toAttributionDto(existing);
        }

        Customer referee = customerRepository.findById(request.refereeCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Referee customer not found"));

        ReferralAttribution attribution = new ReferralAttribution();
        attribution.setReferralCode(code);
        attribution.setRefereeCustomer(referee);
        attribution.setStatus(ReferralAttributionStatus.PENDING);
        attribution = attributionRepository.save(attribution);

        code.setRefereesCount(code.getRefereesCount() + 1);
        codeRepository.save(code);

        return toAttributionDto(attribution);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferralAttributionDto> listAttributionsForCode(UUID referralCodeId) {
        return attributionRepository.findByReferralCodeId(referralCodeId).stream()
                .map(this::toAttributionDto).toList();
    }

    // ---------------- helpers ----------------

    private Discount resolveDiscount(UUID id) {
        if (id == null) return null;
        return discountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found: " + id));
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder("REF-");
            for (int i = 0; i < 8; i++) sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            String code = sb.toString();
            if (codeRepository.findByCodeIgnoreCase(code).isEmpty()) return code;
        }
        throw new BadRequestException("Could not generate unique referral code");
    }

    private ReferralProgramDto toDto(ReferralProgram p) {
        return new ReferralProgramDto(
                p.getId(), p.getName(), p.getStatus(),
                p.getReferrerDiscount() != null ? p.getReferrerDiscount().getId() : null,
                p.getRefereeDiscount() != null ? p.getRefereeDiscount().getId() : null,
                p.getRewardTrigger(),
                p.getMinRefereeOrderAmount(),
                p.getRefereeNthOrder(),
                p.getMaxReferralsPerCustomer(),
                p.getDescription()
        );
    }

    private ReferralCodeDto toCodeDto(ReferralCode c) {
        return new ReferralCodeDto(c.getId(), c.getProgram().getId(), c.getCustomer().getId(),
                c.getCode(), c.getStatus(), c.getRefereesCount(), c.getRewardsPaidCount());
    }

    private ReferralAttributionDto toAttributionDto(ReferralAttribution a) {
        return new ReferralAttributionDto(
                a.getId(), a.getReferralCode().getId(), a.getRefereeCustomer().getId(),
                a.getRefereeOrder() != null ? a.getRefereeOrder().getId() : null,
                a.getStatus(), a.getQualifiedAt(), a.getRewardedAt(),
                a.getReferrerRewardAmount(), a.getRefereeRewardAmount(), a.getNotes()
        );
    }
}
