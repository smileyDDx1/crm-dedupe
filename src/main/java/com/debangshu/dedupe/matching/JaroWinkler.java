package com.debangshu.dedupe.matching;

/**
 * Jaro-Winkler similarity: 1.0 = identical, 0.0 = nothing in common.
 *
 * Why this and not Levenshtein (edit distance)? It was designed for comparing people's names in
 * census records: it handles swapped letters ("Sharma" / "Shrama") well, and the Winkler part gives
 * extra weight to a matching start, which fits names because typos are rarer in the first letters.
 * It also returns a score between 0 and 1 directly, which is easy to combine with other fields.
 */
public final class JaroWinkler {

    private static final double PREFIX_SCALE = 0.1;
    private static final int MAX_PREFIX = 4;

    private JaroWinkler() {
    }

    public static double similarity(String s1, String s2) {
        if (s1.equals(s2)) {
            return 1.0;
        }
        if (s1.isEmpty() || s2.isEmpty()) {
            return 0.0;
        }
        double jaro = jaro(s1, s2);
        int prefix = 0;
        int maxPrefix = Math.min(MAX_PREFIX, Math.min(s1.length(), s2.length()));
        while (prefix < maxPrefix && s1.charAt(prefix) == s2.charAt(prefix)) {
            prefix++;
        }
        return jaro + prefix * PREFIX_SCALE * (1 - jaro);
    }

    static double jaro(String s1, String s2) {
        int length1 = s1.length();
        int length2 = s2.length();
        // two characters "match" if they are equal and not further apart than this
        int window = Math.max(0, Math.max(length1, length2) / 2 - 1);
        boolean[] matched1 = new boolean[length1];
        boolean[] matched2 = new boolean[length2];

        int matches = 0;
        for (int i = 0; i < length1; i++) {
            int from = Math.max(0, i - window);
            int to = Math.min(length2, i + window + 1);
            for (int j = from; j < to; j++) {
                if (!matched2[j] && s1.charAt(i) == s2.charAt(j)) {
                    matched1[i] = true;
                    matched2[j] = true;
                    matches++;
                    break;
                }
            }
        }
        if (matches == 0) {
            return 0.0;
        }

        // matched characters that appear in a different order count as half a transposition each
        int outOfOrder = 0;
        int k = 0;
        for (int i = 0; i < length1; i++) {
            if (!matched1[i]) {
                continue;
            }
            while (!matched2[k]) {
                k++;
            }
            if (s1.charAt(i) != s2.charAt(k)) {
                outOfOrder++;
            }
            k++;
        }
        double m = matches;
        return (m / length1 + m / length2 + (m - outOfOrder / 2.0) / m) / 3.0;
    }
}
