package com.debangshu.dedupe.matching;

import java.util.Locale;

/**
 * American Soundex: encodes a name by how it sounds, as a letter + 3 digits ("Robert" and "Rupert" are
 * both R163, "Sharma" and "Sarma" are both S650). Used for blocking: names that sound alike end up in
 * the same block even with a typo, so they still get compared.
 *
 * Rules: keep the first letter; map the other consonants to digits (similar sounds share a digit);
 * drop vowels; a run of the same digit counts once; H and W don't break such a run, vowels do.
 */
public final class Soundex {

    private Soundex() {
    }

    public static String encode(String name) {
        String letters = name.toUpperCase(Locale.ROOT).replaceAll("[^A-Z]", "");
        if (letters.isEmpty()) {
            return "";
        }
        StringBuilder code = new StringBuilder(4).append(letters.charAt(0));
        char previous = digitFor(letters.charAt(0));
        for (int i = 1; i < letters.length() && code.length() < 4; i++) {
            char letter = letters.charAt(i);
            char digit = digitFor(letter);
            if (digit != '0' && digit != previous) {
                code.append(digit);
            }
            if (letter != 'H' && letter != 'W') {
                previous = digit;
            }
        }
        while (code.length() < 4) {
            code.append('0');
        }
        return code.toString();
    }

    private static char digitFor(char letter) {
        return switch (letter) {
            case 'B', 'F', 'P', 'V' -> '1';
            case 'C', 'G', 'J', 'K', 'Q', 'S', 'X', 'Z' -> '2';
            case 'D', 'T' -> '3';
            case 'L' -> '4';
            case 'M', 'N' -> '5';
            case 'R' -> '6';
            default -> '0'; // vowels, H, W, Y
        };
    }
}
