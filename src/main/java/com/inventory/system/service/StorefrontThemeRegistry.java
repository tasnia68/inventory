package com.inventory.system.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.system.payload.StorefrontThemeManifestDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Loads storefront theme manifests from classpath:storefront/themes/&#42;/theme.manifest.json
 * at application startup and exposes them as the source of truth for the admin theme editor schema.
 *
 * Themes are bundled with the build (folder-based templates); this registry simply discovers them
 * and serves their declared settings/section/block/preset definitions to the rest of the system.
 */
@Component
@RequiredArgsConstructor
public class StorefrontThemeRegistry {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(StorefrontThemeRegistry.class);

    private static final String MANIFEST_PATTERN = "classpath:storefront/themes/*/theme.manifest.json";

    private final ObjectMapper objectMapper;

    private final Map<String, StorefrontThemeManifestDto> manifests = new LinkedHashMap<>();

    @PostConstruct
    public void loadManifests() {
        manifests.clear();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources(MANIFEST_PATTERN);
            for (Resource resource : resources) {
                try (InputStream in = resource.getInputStream()) {
                    StorefrontThemeManifestDto manifest = objectMapper.readValue(in, StorefrontThemeManifestDto.class);
                    if (manifest.getKey() == null || manifest.getKey().isBlank()) {
                        logger.warn("Theme manifest {} is missing 'key' field; skipping", resource.getFilename());
                        continue;
                    }
                    manifests.put(manifest.getKey(), manifest);
                    logger.info("Loaded storefront theme manifest: {} v{}", manifest.getKey(), manifest.getVersion());
                } catch (Exception ex) {
                    logger.error("Failed to load storefront theme manifest from {}: {}", resource.getDescription(), ex.getMessage(), ex);
                }
            }
        } catch (Exception ex) {
            logger.error("Failed to scan storefront theme manifests: {}", ex.getMessage(), ex);
        }
        if (manifests.isEmpty()) {
            logger.warn("No storefront theme manifests found at {}", MANIFEST_PATTERN);
        }
    }

    public List<StorefrontThemeManifestDto> listAll() {
        return new ArrayList<>(manifests.values());
    }

    public Optional<StorefrontThemeManifestDto> findByKey(String key) {
        if (key == null) return Optional.empty();
        return Optional.ofNullable(manifests.get(key));
    }

    public boolean hasTheme(String key) {
        return key != null && manifests.containsKey(key);
    }

    /**
     * Build the legacy `schema` map (templateTree / settingsGroups / sectionDefinitions /
     * blockDefinitions / templatePresets) that the admin theme editor expects, sourced from
     * the registered manifest for the given templateKey.
     *
     * Returns Optional.empty() if no manifest is registered for the key; callers should fall
     * back to their inline default to preserve behavior for un-migrated themes.
     */
    public Optional<Map<String, Object>> buildSchemaForKey(String templateKey, String schemaVersion) {
        return findByKey(templateKey).map(manifest -> {
            StorefrontThemeManifestDto merged = resolveWithInheritance(manifest);
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put("version", schemaVersion);
            schema.put("templateKey", manifest.getKey());
            schema.put("themeVersion", manifest.getVersion());
            schema.put("templateTree", merged.getTemplateTree() != null ? merged.getTemplateTree() : Collections.emptyList());
            schema.put("settingsGroups", merged.getSettingsGroups() != null ? merged.getSettingsGroups() : Collections.emptyList());
            schema.put("sectionDefinitions", merged.getSectionDefinitions() != null ? merged.getSectionDefinitions() : Collections.emptyMap());
            schema.put("blockDefinitions", merged.getBlockDefinitions() != null ? merged.getBlockDefinitions() : Collections.emptyMap());
            schema.put("templatePresets", merged.getTemplatePresets() != null ? merged.getTemplatePresets() : Collections.emptyMap());
            return schema;
        });
    }

    /**
     * Resolve a manifest's `extends` chain by overlaying child fields on top of
     * parent. Maps merge per-key (child overrides), lists are taken from child
     * if non-empty else parent. Cycles are guarded; missing parents are logged
     * and silently skipped (child stands alone).
     */
    StorefrontThemeManifestDto resolveWithInheritance(StorefrontThemeManifestDto child) {
        if (child.getExtendsKey() == null || child.getExtendsKey().isBlank()) return child;
        return resolveChain(child, new java.util.LinkedHashSet<>());
    }

    private StorefrontThemeManifestDto resolveChain(StorefrontThemeManifestDto current, java.util.Set<String> visited) {
        if (current.getKey() != null && !visited.add(current.getKey())) {
            logger.warn("Theme manifest extends-cycle detected at {}; halting resolution", current.getKey());
            return current;
        }
        String parentKey = current.getExtendsKey();
        if (parentKey == null || parentKey.isBlank()) return current;
        StorefrontThemeManifestDto parent = manifests.get(parentKey);
        if (parent == null) {
            logger.warn("Theme manifest {} extends '{}' but parent not registered; ignoring extends", current.getKey(), parentKey);
            return current;
        }
        StorefrontThemeManifestDto resolvedParent = resolveChain(parent, visited);
        StorefrontThemeManifestDto merged = new StorefrontThemeManifestDto();
        merged.setKey(current.getKey());
        merged.setName(current.getName());
        merged.setVersion(current.getVersion());
        merged.setDescription(current.getDescription() != null ? current.getDescription() : resolvedParent.getDescription());
        merged.setAuthor(current.getAuthor() != null ? current.getAuthor() : resolvedParent.getAuthor());
        merged.setScreenshot(current.getScreenshot() != null ? current.getScreenshot() : resolvedParent.getScreenshot());
        merged.setTags(current.getTags() != null && !current.getTags().isEmpty() ? current.getTags() : resolvedParent.getTags());
        merged.setSupportedSectionGroups(current.getSupportedSectionGroups() != null && !current.getSupportedSectionGroups().isEmpty()
                ? current.getSupportedSectionGroups() : resolvedParent.getSupportedSectionGroups());
        merged.setTemplateTree(current.getTemplateTree() != null && !current.getTemplateTree().isEmpty()
                ? current.getTemplateTree() : resolvedParent.getTemplateTree());
        merged.setSettingsGroups(current.getSettingsGroups() != null && !current.getSettingsGroups().isEmpty()
                ? current.getSettingsGroups() : resolvedParent.getSettingsGroups());
        merged.setDefaultSettings(mergeMap(resolvedParent.getDefaultSettings(), current.getDefaultSettings()));
        merged.setSectionDefinitions(mergeMap(resolvedParent.getSectionDefinitions(), current.getSectionDefinitions()));
        merged.setBlockDefinitions(mergeMap(resolvedParent.getBlockDefinitions(), current.getBlockDefinitions()));
        merged.setTemplatePresets(mergeMap(resolvedParent.getTemplatePresets(), current.getTemplatePresets()));
        merged.setMigrations(current.getMigrations() != null && !current.getMigrations().isEmpty()
                ? current.getMigrations() : resolvedParent.getMigrations());
        return merged;
    }

    private Map<String, Object> mergeMap(Map<String, Object> parent, Map<String, Object> child) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (parent != null) result.putAll(parent);
        if (child != null) result.putAll(child); // child wins per-key
        return result;
    }
}
