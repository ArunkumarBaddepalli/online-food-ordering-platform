package com.app.fooddelivery.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchMatcherTest {

    private final SearchMatcher matcher = new SearchMatcher();

    @Test
    @DisplayName("A single mistyped letter still finds the restaurant")
    void forgivesOneTypo() {
        assertThat(matcher.matchesLoosely("Burger Hub", "Buger")).isTrue();
        assertThat(matcher.matchesLoosely("Pizza Palace", "Piza")).isTrue();
        assertThat(matcher.matchesLoosely("Sushi Express", "Sushii")).isTrue();
        assertThat(matcher.matchesLoosely("Thai Basil", "Thia")).isTrue();
    }

    @Test
    @DisplayName("A correct spelling still matches, obviously")
    void exactStillWorks() {
        assertThat(matcher.matchesLoosely("Burger Hub", "burger")).isTrue();
        assertThat(matcher.matchesLoosely("Burger Hub", "hub")).isTrue();
        assertThat(matcher.containsIgnoreCase("Burger Hub", "BURGER")).isTrue();
    }

    @Test
    @DisplayName("Trailing letters do not defeat it")
    void matchesLongerWords() {
        assertThat(matcher.matchesLoosely("Loaded Burgers", "buger")).isTrue();
        assertThat(matcher.matchesLoosely("Pancakes", "pancake")).isTrue();
    }

    @Test
    @DisplayName("Genuinely different words are not dragged in")
    void doesNotMatchDifferentWords() {
        // Two edits apart, and both real menu words — forgiving this would make
        // searching for one dish return another.
        assertThat(matcher.matchesLoosely("Pasta Paradise", "pizza")).isFalse();
        assertThat(matcher.matchesLoosely("Vegan Delight", "burger")).isFalse();
        assertThat(matcher.matchesLoosely("Thai Basil", "sushi")).isFalse();
    }

    @Test
    @DisplayName("Very short terms are matched exactly only")
    void shortTermsAreStrict() {
        // At three letters nearly everything is one edit from everything else.
        assertThat(matcher.matchesLoosely("Burger Hub", "hib")).isFalse();
        assertThat(matcher.matchesLoosely("Burger Hub", "hub")).isTrue();
    }

    @Test
    @DisplayName("Longer terms forgive two typos")
    void longerTermsAreMoreForgiving() {
        assertThat(matcher.matchesLoosely("Mediterranean Grill", "Meditteranean")).isTrue();
        assertThat(matcher.matchesLoosely("Margherita Pizza", "Margarita")).isTrue();
    }

    @Test
    @DisplayName("Nothing matches nothing, without throwing")
    void handlesEmptyInput() {
        assertThat(matcher.matchesLoosely(null, "burger")).isFalse();
        assertThat(matcher.matchesLoosely("Burger Hub", null)).isFalse();
        assertThat(matcher.matchesLoosely("Burger Hub", "  ")).isFalse();
        assertThat(matcher.containsIgnoreCase(null, "x")).isFalse();
    }

    @Test
    @DisplayName("Edit distance counts what it should")
    void editDistanceIsCorrect() {
        assertThat(matcher.editDistance("buger", "burger")).isEqualTo(1);   // dropped letter
        assertThat(matcher.editDistance("thia", "thai")).isEqualTo(1);      // swapped letters
        assertThat(matcher.editDistance("pizza", "pasta")).isEqualTo(3);    // genuinely different
        assertThat(matcher.editDistance("same", "same")).isZero();
        assertThat(matcher.editDistance("", "abc")).isEqualTo(3);
    }
}
