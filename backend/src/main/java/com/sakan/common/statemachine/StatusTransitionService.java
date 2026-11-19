package com.sakan.common.statemachine;

import java.time.Instant;
import java.util.Objects;
import java.util.function.Consumer;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.sakan.common.event.StatusChangedEvent;

/** Shared validation and event publication for status changes across feature modules. */
@Component
public class StatusTransitionService {
    private final ApplicationEventPublisher events;

    public StatusTransitionService(ApplicationEventPublisher events) {
        this.events = Objects.requireNonNull(events);
    }

    public <S extends Enum<S>> void transition(
            String entityType,
            Object entityId,
            S previous,
            S target,
            String actor,
            StatusTransitions<S> transitions,
            Consumer<S> applyStatus) {
        Objects.requireNonNull(transitions, "transitions must not be null").assertAllowed(previous, target);
        Objects.requireNonNull(applyStatus, "applyStatus must not be null").accept(target);
        publish(entityType, entityId, previous, target, actor);
    }

    /** Publish the first status once a newly created entity has been persisted and has an identifier. */
    public <S extends Enum<S>> void publishInitialStatus(String entityType, Object entityId, S target, String actor) {
        Objects.requireNonNull(target, "target status must not be null");
        publish(entityType, entityId, null, target, actor);
    }

    private void publish(String entityType, Object entityId, Enum<?> previous, Enum<?> target, String actor) {
        Objects.requireNonNull(entityType, "entityType must not be null");
        Objects.requireNonNull(entityId, "entityId must not be null");
        events.publishEvent(new StatusChangedEvent(
                entityType,
                entityId.toString(),
                previous == null ? null : previous.name(),
                target.name(),
                actor == null || actor.isBlank() ? "system" : actor,
                Instant.now()));
    }
}
