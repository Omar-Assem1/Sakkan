package com.sakan.property.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Every ordered pair of statuses is checked: the allowed ones below, and all the rest must be illegal. */
class PropertyStatusTest {

    private static final Set<String> ALLOWED = Set.of(
            "DRAFT->PUBLISHED",
            "DRAFT->ARCHIVED",
            "PUBLISHED->RENTED",
            "PUBLISHED->SOLD",
            "PUBLISHED->ARCHIVED",
            "RENTED->PUBLISHED",
            "RENTED->ARCHIVED",
            "SOLD->ARCHIVED");

    static Stream<Arguments> everyPair() {
        Stream.Builder<Arguments> pairs = Stream.builder();
        for (PropertyStatus from : PropertyStatus.values()) {
            for (PropertyStatus to : PropertyStatus.values()) {
                pairs.add(Arguments.of(from, to, ALLOWED.contains(from + "->" + to)));
            }
        }
        return pairs.build();
    }

    @ParameterizedTest(name = "{0} -> {1} allowed: {2}")
    @MethodSource("everyPair")
    void transitionIsAllowedOnlyWhenListed(PropertyStatus from, PropertyStatus to, boolean expected) {
        assertThat(from.canTransitionTo(to)).isEqualTo(expected);
    }

    @Test
    void archivedIsTerminal() {
        assertThat(PropertyStatus.ARCHIVED.allowedTargets()).isEmpty();
    }

    @Test
    void onlyDraftAndPublishedAreEditable() {
        assertThat(PropertyStatus.DRAFT.isEditable()).isTrue();
        assertThat(PropertyStatus.PUBLISHED.isEditable()).isTrue();
        assertThat(PropertyStatus.RENTED.isEditable()).isFalse();
        assertThat(PropertyStatus.SOLD.isEditable()).isFalse();
        assertThat(PropertyStatus.ARCHIVED.isEditable()).isFalse();
    }
}
