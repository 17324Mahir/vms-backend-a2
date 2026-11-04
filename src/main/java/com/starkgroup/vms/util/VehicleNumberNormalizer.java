package com.starkgroup.vms.util;

import java.util.Locale;

public final class VehicleNumberNormalizer {

    private VehicleNumberNormalizer() {
    }

    /**
     * Normalizes a vehicle number by trimming leading and trailing whitespace
     * and converting letters to uppercase using Locale.ROOT, while preserving
     * internal spaces, punctuation, hyphens, and non-Latin characters.
     *
     * @param vehicleNumber raw input vehicle number
     * @return normalized vehicle number, or null if input is null
     */
    public static String normalize(String vehicleNumber) {
        if (vehicleNumber == null) {
            return null;
        }
        return vehicleNumber.trim().toUpperCase(Locale.ROOT);
    }
}
