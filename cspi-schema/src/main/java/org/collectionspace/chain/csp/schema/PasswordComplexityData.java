package org.collectionspace.chain.csp.schema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.collectionspace.chain.csp.config.ReadOnlySection;

public class PasswordComplexityData {
    final boolean enabled;
    final boolean allowWhitespace;

    final Integer minLength;
    final Integer minLowerCase;
    final Integer minUpperCase;
    final Integer minDigits;
    final Integer minSpecial;

    final Integer minCharacterClasses;
    final List<String> characterClasses;

    final List<IllegalSequence> illegalSequences = new ArrayList<>();

    public PasswordComplexityData(ReadOnlySection section) {
        enabled = Boolean.parseBoolean((String) section.getValue("/enabled"));
        allowWhitespace = Boolean.parseBoolean((String) section.getValue("/allow-whitespace"));

        minLength = parseIntFromSection(section, "/min-length");
        minLowerCase = parseIntFromSection(section, "/character-rules/min-lower-case");
        minUpperCase = parseIntFromSection(section, "/character-rules/min-upper-case");
        minDigits = parseIntFromSection(section, "/character-rules/min-digit");
        minSpecial = parseIntFromSection(section, "/character-rules/min-special");

        minCharacterClasses = parseIntFromSection(section, "/character-class-rules/min-required");
        var characterClassesStr = (String) section.getValue("/character-class-rules/character-classes");
        characterClasses = characterClassesStr != null ? Arrays.asList(characterClassesStr.split(","))
                                                       : new ArrayList<>();
    }

    private Integer parseIntFromSection(ReadOnlySection section, String path) {
        final var asString = (String) section.getValue(path);
        return asString == null ? null : Integer.parseInt(asString);
    }

    public void addIllegalSequence(IllegalSequence sequence) {
        illegalSequences.add(sequence);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAllowWhitespace() {
        return allowWhitespace;
    }

    public Optional<Integer> getMinLength() {
        return Optional.ofNullable(minLength);
    }

    public Optional<Integer> getMinLowerCase() {
        return Optional.ofNullable(minLowerCase);
    }

    public Optional<Integer> getMinUpperCase() {
        return Optional.ofNullable(minUpperCase);
    }

    public Optional<Integer> getMinDigits() {
        return Optional.ofNullable(minDigits);
    }

    public Optional<Integer> getMinSpecial() {
        return Optional.ofNullable(minSpecial);
    }

    public Optional<Integer> getMinCharacterClasses() {
        return Optional.ofNullable(minCharacterClasses);
    }

    public List<String> getCharacterClasses() {
        return characterClasses;
    }

    public List<IllegalSequence> getIllegalSequences() {
        return illegalSequences;
    }

    public static class IllegalSequence {
        final String characterClass;
        final int length;

        public IllegalSequence(ReadOnlySection section) {
            this.characterClass = (String) section.getValue("/character-class");

            String lengthStr = (String) section.getValue("/length");
            this.length = lengthStr != null ? Integer.parseInt(lengthStr) : 6;
        }

        public String getCharacterClass() {
            return characterClass;
        }

        public int getLength() {
            return length;
        }
    }
}
