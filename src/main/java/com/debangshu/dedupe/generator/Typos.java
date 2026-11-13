package com.debangshu.dedupe.generator;

import java.util.Random;

/** The four classic typing mistakes: a missing, a swapped, a wrong or a doubled letter. */
final class Typos {

    private static final String KEYBOARD_ROWS = "qwertyuiop|asdfghjkl|zxcvbnm";

    private Typos() {
    }

    static String typo(String word, Random random) {
        if (word.length() < 3) {
            return word;
        }
        // mostly after the first letter: people rarely get the first letter of a name wrong
        int position = random.nextDouble() < 0.1 ? 0 : 1 + random.nextInt(word.length() - 2);
        StringBuilder text = new StringBuilder(word);
        switch (random.nextInt(4)) {
            case 0 -> text.deleteCharAt(position);
            case 1 -> {
                char current = text.charAt(position);
                text.setCharAt(position, text.charAt(position + 1));
                text.setCharAt(position + 1, current);
            }
            case 2 -> text.setCharAt(position, neighbourKey(text.charAt(position), random));
            default -> text.insert(position, text.charAt(position));
        }
        return text.toString();
    }

    /** A key next to this one on a QWERTY keyboard, keeping upper/lower case. */
    private static char neighbourKey(char letter, Random random) {
        char lower = Character.toLowerCase(letter);
        int index = KEYBOARD_ROWS.indexOf(lower);
        if (index < 0) {
            return letter;
        }
        int neighbour = random.nextBoolean() ? index - 1 : index + 1;
        if (neighbour < 0 || neighbour >= KEYBOARD_ROWS.length() || KEYBOARD_ROWS.charAt(neighbour) == '|') {
            neighbour = index + (neighbour < index ? 1 : -1);
        }
        char replacement = KEYBOARD_ROWS.charAt(neighbour);
        return Character.isUpperCase(letter) ? Character.toUpperCase(replacement) : replacement;
    }
}
