package com.sakan.common.statemachine;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import com.sakan.common.exception.InvalidStatusTransitionException;

/**
 * Lightweight generic status transition validator.
 *
 * <p>Lives in {@code com.sakan.common.statemachine} with zero imports from feature modules so it
 * respects Sakan's modular monolith boundary.
 */
public final class StatusTransitions<S extends Enum<S>> {

    private final Map<S, Set<S>> allowed;

    private StatusTransitions(Map<S, Set<S>> allowed) {
        this.allowed = Map.copyOf(allowed);
    }

    public static <S extends Enum<S>> StatusTransitions<S> of(Map<S, Set<S>> allowedTransitions) {
        Objects.requireNonNull(allowedTransitions, "allowedTransitions must not be null");
        Map<S, Set<S>> rules = new HashMap<>();
        allowedTransitions.forEach((status, targets) -> rules.put(
                Objects.requireNonNull(status, "status must not be null"),
                Set.copyOf(Objects.requireNonNull(targets, "allowed targets must not be null"))));
        return new StatusTransitions<>(rules);
    }

    /** Build transitions from an enum-owned policy without duplicating its allowed-target rules. */
    public static <S extends Enum<S>> StatusTransitions<S> from(
            Class<S> statusType, Function<S, Set<S>> allowedTargets) {
        Objects.requireNonNull(statusType, "statusType must not be null");
        Objects.requireNonNull(allowedTargets, "allowedTargets must not be null");
        Map<S, Set<S>> rules = new EnumMap<>(statusType);
        for (S status : statusType.getEnumConstants()) {
            rules.put(status, Set.copyOf(Objects.requireNonNull(allowedTargets.apply(status))));
        }
        return new StatusTransitions<>(rules);
    }

    public boolean isAllowed(S current, S target) {
        if (current == null || target == null) {
            return false;
        }
        return allowed.getOrDefault(current, Set.of()).contains(target);
    }

    public void assertAllowed(S current, S target) {
        if (!isAllowed(current, target)) {
            throw new InvalidStatusTransitionException(
                    "Invalid status transition from " + current + " to " + target);
        }
    }
}
