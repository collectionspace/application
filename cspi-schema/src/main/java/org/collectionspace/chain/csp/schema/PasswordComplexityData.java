package org.collectionspace.chain.csp.schema;

import java.util.Optional;

import org.collectionspace.chain.csp.config.ReadOnlySection;

public class PasswordComplexityData {
    final boolean enabled;

    final Integer minLength;
    final boolean requireLowerCase;
    final boolean requireUpperCase;
    final boolean requireDigit;
    final boolean requireSpecial;

    public PasswordComplexityData(ReadOnlySection section) {
        enabled = Boolean.parseBoolean((String) section.getValue("/enabled"));

        minLength = parseIntFromSection(section, "/min-length");
        requireLowerCase = Boolean.parseBoolean((String) section.getValue("/require-lower-case"));
        requireUpperCase = Boolean.parseBoolean((String) section.getValue("/require-upper-case"));
        requireDigit = Boolean.parseBoolean((String) section.getValue("/require-digit"));
        requireSpecial = Boolean.parseBoolean((String) section.getValue("/require-special"));
    }

    private Integer parseIntFromSection(ReadOnlySection section, String path) {
        final var asString = (String) section.getValue(path);
        return asString == null ? null : Integer.parseInt(asString);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean requireLowerCase() {
        return requireLowerCase;
    }

    public boolean requireUpperCase() {
        return requireUpperCase;
    }

    public boolean requireDigit() {
        return requireDigit;
    }

    public boolean requireSpecial() {
        return requireSpecial;
    }

    public Optional<Integer> getMinLength() {
        return Optional.ofNullable(minLength);
    }

}
