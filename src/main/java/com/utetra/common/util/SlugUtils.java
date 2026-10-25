package com.utetra.common.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Doi ten tieng Viet thanh slug dung tren URL.
 *
 *   "Trà sữa trân châu"  ->  "tra-sua-tran-chau"
 */
public final class SlugUtils {

    private SlugUtils() {
    }

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String noAccent = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('\u0111', 'd')
                .replace('\u0110', 'D');
        return noAccent.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-");
    }
}
