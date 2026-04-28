package com.inventory.system.service.courier;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CourierProviderRegistry {

    private final Map<String, CourierProvider> providersByCode;

    public CourierProviderRegistry(List<CourierProvider> providers) {
        this.providersByCode = providers.stream()
                .collect(Collectors.toUnmodifiableMap(p -> p.providerCode().toUpperCase(), Function.identity()));
    }

    public CourierProvider resolve(String providerCode) {
        if (providerCode == null) {
            throw new CourierProviderException("Courier provider code is required");
        }
        CourierProvider provider = providersByCode.get(providerCode.toUpperCase());
        if (provider == null) {
            throw new CourierProviderException("No courier provider registered for code: " + providerCode);
        }
        return provider;
    }

    public List<String> listProviderCodes() {
        return providersByCode.keySet().stream().sorted().toList();
    }
}
