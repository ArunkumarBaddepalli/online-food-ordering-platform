package com.app.fooddelivery.service;

import org.springframework.stereotype.Component;

/**
 * Text matching for the restaurant search.
 *
 * Plain substring matching fails the moment somebody mistypes: "buger" is not
 * inside "burger", so a single wrong letter returns nothing at all. This adds a
 * tolerance for near-misses, measured word by word.
 */
@Component
public class SearchMatcher {

    /** Straight substring match, case-insensitive. */
    public boolean containsIgnoreCase(String text, String term) {
        return text != null && term != null
                && text.toLowerCase().contains(term.toLowerCase());
    }

    /**
     * True when any word in the text is the term, or close enough to it that a
     * typo is the likely explanation.
     */
    public boolean matchesLoosely(String text, String term) {
        if (text == null || term == null || term.isBlank()) {
            return false;
        }
        if (containsIgnoreCase(text, term)) {
            return true;
        }

        String needle = term.toLowerCase().trim();
        int allowed = allowedEdits(needle);
        if (allowed == 0) {
            return false;
        }

        for (String word : text.toLowerCase().split("[^a-z0-9]+")) {
            if (word.isEmpty()) {
                continue;
            }
            // Compare against the start of longer words too, so "buger"
            // matches "burgers" rather than being beaten by the trailing s.
            String candidate = word.length() > needle.length() + allowed
                    ? word.substring(0, Math.min(word.length(), needle.length() + allowed))
                    : word;

            if (editDistance(needle, candidate) <= allowed) {
                return true;
            }
        }
        return false;
    }

    /**
     * How many typos to forgive, by term length.
     *
     * Short terms get none: at three letters almost everything is within one
     * edit of everything else. Two edits only for long words, otherwise "pizza"
     * starts matching "pasta", which is worse than finding nothing.
     */
    private int allowedEdits(String term) {
        if (term.length() < 4) {
            return 0;
        }
        return term.length() <= 7 ? 1 : 2;
    }

    /**
     * Damerau-Levenshtein distance, counting a swap of two neighbouring letters
     * as one mistake rather than two.
     *
     * That matters: "thia" for "thai" is among the commonest ways to mistype a
     * word, and plain Levenshtein charges it double, which is enough to push a
     * perfectly obvious search past the tolerance.
     */
    int editDistance(String a, String b) {
        int[][] d = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            d[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            d[0][j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;

                d[i][j] = Math.min(Math.min(
                        d[i - 1][j] + 1,        // deletion
                        d[i][j - 1] + 1),       // insertion
                        d[i - 1][j - 1] + cost); // substitution

                boolean swapped = i > 1 && j > 1
                        && a.charAt(i - 1) == b.charAt(j - 2)
                        && a.charAt(i - 2) == b.charAt(j - 1);
                if (swapped) {
                    d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
                }
            }
        }

        return d[a.length()][b.length()];
    }
}
