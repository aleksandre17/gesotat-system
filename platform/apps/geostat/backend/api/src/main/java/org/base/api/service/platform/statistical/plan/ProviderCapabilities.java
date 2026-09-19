package org.base.api.service.platform.statistical.plan;

import java.util.Locale;
import java.util.Set;

/**
 * What one authoring provider can physically hold. An adapter declares its own limits; the compiler only
 * negotiates against the declaration and never assumes a provider (register Q35).
 */
public record ProviderCapabilities(String providerCode, int maxColumnsPerTable, int maxIdentifierLength,
                                   int maxUniqueIndexFields, int maxDecimalPrecision, boolean exactDecimal,
                                   Set<String> reservedWords) {
    public ProviderCapabilities {
        if (maxColumnsPerTable < 2 || maxIdentifierLength < 16 || maxUniqueIndexFields < 1)
            throw new IllegalArgumentException("capability limits are implausible");
        reservedWords = reservedWords.stream().map(w -> w.toUpperCase(Locale.ROOT)).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public boolean isReserved(String identifier) { return reservedWords.contains(identifier.toUpperCase(Locale.ROOT)); }

    /** Lookup port; the production implementation reads the provider capability registry. */
    public interface Catalog {
        java.util.Optional<ProviderCapabilities> find(String sourceProfile);
    }
}
