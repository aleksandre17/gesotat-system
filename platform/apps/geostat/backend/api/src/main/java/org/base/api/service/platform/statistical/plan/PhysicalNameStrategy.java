package org.base.api.service.platform.statistical.plan;

import org.base.api.service.platform.statistical.canonical.CanonicalJson;

import java.util.Locale;
import java.util.Set;

/**
 * Deterministic, collision-safe physical identifiers. A name is kept verbatim when the provider accepts it;
 * otherwise it is shortened and suffixed with a digest of the canonical identity, so regeneration is stable
 * and two canonical identities never share a physical name. Physical names never feed back into identity.
 */
public final class PhysicalNameStrategy {
    private static final int SUFFIX = 9; // "_" + 8 hex

    private PhysicalNameStrategy() { }

    /** @param taken upper-cased names already used in the same physical scope; the result is added to it */
    public static String allocate(String preferred, String canonicalIdentity, Set<String> taken, ProviderCapabilities caps) {
        String name = preferred;
        if (name.length() > caps.maxIdentifierLength() || caps.isReserved(name) || taken.contains(upper(name)))
            name = suffixed(preferred, canonicalIdentity, caps);
        if (!taken.add(upper(name)))
            throw new IllegalStateException("physical name collision cannot be resolved for " + canonicalIdentity);
        return name;
    }

    private static String suffixed(String preferred, String canonicalIdentity, ProviderCapabilities caps) {
        String hash = CanonicalJson.digest("geostat.stat-physical-name.v1", canonicalIdentity).substring(0, 8);
        int keep = Math.min(preferred.length(), caps.maxIdentifierLength() - SUFFIX);
        return preferred.substring(0, keep) + "_" + hash;
    }

    private static String upper(String value) { return value.toUpperCase(Locale.ROOT); }
}
