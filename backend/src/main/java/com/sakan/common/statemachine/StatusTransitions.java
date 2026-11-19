package com.sakan.common.statemachine;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.sakan.common.exception.InvalidStatusTransitionException;

/**
 * Lightweight generic status transition validator.
 *
 * <p>Lives in {@code com.sakan.common.statemachine} with zero imports from feature modules so it
 * respects Sakan's modular monolith boundary.
 */
// TODO(SCRUM-31): replace with M4's merged SKN-10 StatusTransitions framework if its API differs.
public final class StatusTransitions<S extends Enum<S>> {

    private final Map<S, Set<S>> allowed;

    private StatusTransitions(Map<S, Set<S>> allowed) {
        this.allowed = Map.copyOf(allowed);
    }

    public static <S extends Enum<S>> StatusTransitions<S> of(Map<S, Set<S>> allowedTransitions) {
        Objects.requireNonNull(allowedTransitions, "allowedTransitions must not be null");
        return new StatusTransitions<>(allowedTransitions);
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
