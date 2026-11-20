package com.sakan.common.statemachine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sakan.common.exception.InvalidStatusTransitionException;
import com.sakan.property.domain.PropertyStatus;
import com.sakan.viewing.domain.ViewingStatus;

class StatusTransitionsTest {

    private static final Set<String> PROPERTY_ALLOWED = Set.of(
            "DRAFT->PUBLISHED", "DRAFT->ARCHIVED",
            "PUBLISHED->RENTED", "PUBLISHED->SOLD", "PUBLISHED->ARCHIVED",
            "RENTED->PUBLISHED", "RENTED->ARCHIVED", "SOLD->ARCHIVED");

    private static final Set<String> VIEWING_ALLOWED = Set.of(
            "REQUESTED->CONFIRMED", "REQUESTED->CANCELLED",
            "CONFIRMED->CANCELLED", "CONFIRMED->COMPLETED");

    static Stream<Arguments> everyPropertyStatusPair() {
        return Stream.of(PropertyStatus.values())
                .flatMap(from -> Stream.of(PropertyStatus.values())
                        .map(to -> Arguments.of(from, to, PROPERTY_ALLOWED.contains(from + "->" + to))));
    }

    static Stream<Arguments> everyViewingStatusPair() {
        return Stream.of(ViewingStatus.values())
                .flatMap(from -> Stream.of(ViewingStatus.values())
                        .map(to -> Arguments.of(from, to, VIEWING_ALLOWED.contains(from + "->" + to))));
    }

    @ParameterizedTest(name = "Property {0} -> {1} allowed: {2}")
    @MethodSource("everyPropertyStatusPair")
    void propertyTransitionsMatchTheAllowedMap(PropertyStatus from, PropertyStatus to, boolean expected) {
        StatusTransitions<PropertyStatus> transitions =
                StatusTransitions.from(PropertyStatus.class, PropertyStatus::allowedTargets);

        assertThat(transitions.isAllowed(from, to)).isEqualTo(expected);
        assertTransitionAssertion(transitions, from, to, expected);
    }

    @ParameterizedTest(name = "Viewing {0} -> {1} allowed: {2}")
    @MethodSource("everyViewingStatusPair")
    void viewingTransitionsMatchTheAllowedMap(ViewingStatus from, ViewingStatus to, boolean expected) {
        StatusTransitions<ViewingStatus> transitions =
                StatusTransitions.from(ViewingStatus.class, ViewingStatus::allowedTargets);

        assertThat(transitions.isAllowed(from, to)).isEqualTo(expected);
        assertTransitionAssertion(transitions, from, to, expected);
    }

    @Test
    void ofTakesAnImmutableSnapshotOfTheAllowedTransitionMap() {
        Map<PropertyStatus, Set<PropertyStatus>> rules = new EnumMap<>(PropertyStatus.class);
        Set<PropertyStatus> draftTargets = new HashSet<>(Set.of(PropertyStatus.PUBLISHED));
        rules.put(PropertyStatus.DRAFT, draftTargets);
        StatusTransitions<PropertyStatus> transitions = StatusTransitions.of(rules);

        draftTargets.add(PropertyStatus.ARCHIVED);
        rules.put(PropertyStatus.PUBLISHED, Set.of(PropertyStatus.SOLD));

        assertThat(transitions.isAllowed(PropertyStatus.DRAFT, PropertyStatus.PUBLISHED)).isTrue();
        assertThat(transitions.isAllowed(PropertyStatus.DRAFT, PropertyStatus.ARCHIVED)).isFalse();
        assertThat(transitions.isAllowed(PropertyStatus.PUBLISHED, PropertyStatus.SOLD)).isFalse();
    }

    private static <S extends Enum<S>> void assertTransitionAssertion(
            StatusTransitions<S> transitions, S from, S to, boolean expected) {
        if (expected) {
            assertThatCode(() -> transitions.assertAllowed(from, to)).doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> transitions.assertAllowed(from, to))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }
}
