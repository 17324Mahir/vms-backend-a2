package com.starkgroup.vms.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VehicleNumberNormalizerTest {

    @Test
    @DisplayName("Should return null when input is null")
    void shouldReturnNullWhenInputIsNull() {
        assertNull(VehicleNumberNormalizer.normalize(null));
    }

    @Test
    @DisplayName("Should trim whitespace and convert lowercase letters to uppercase")
    void shouldTrimWhitespaceAndConvertToUppercase() {
        assertEquals("AB-123", VehicleNumberNormalizer.normalize(" ab-123 "));
        assertEquals("SG-2024-X", VehicleNumberNormalizer.normalize("  sg-2024-x   "));
    }

    @Test
    @DisplayName("Should preserve internal spaces, hyphens, and punctuation")
    void shouldPreserveInternalSpacesAndPunctuation() {
        assertEquals("AB 123 CD", VehicleNumberNormalizer.normalize("  ab 123 cd "));
        assertEquals("SG-VMS#42", VehicleNumberNormalizer.normalize(" sg-vms#42  "));
    }

    @Test
    @DisplayName("Should preserve non-Latin characters while uppercasing")
    void shouldPreserveNonLatinCharacters() {
        assertEquals("DHAKA-GA-11", VehicleNumberNormalizer.normalize(" dhaka-ga-11 "));
    }
}
