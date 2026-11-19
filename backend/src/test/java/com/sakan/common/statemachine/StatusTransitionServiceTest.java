package com.sakan.common.statemachine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.sakan.common.event.StatusChangedEvent;
import com.sakan.common.exception.InvalidStatusTransitionException;
import com.sakan.property.domain.PropertyStatus;

@ExtendWith(MockitoExtension.class)
class StatusTransitionServiceTest {
    @Mock ApplicationEventPublisher publisher;

    private final StatusTransitions<PropertyStatus> transitions = StatusTransitions.of(Map.of(
            PropertyStatus.DRAFT, EnumSet.of(PropertyStatus.PUBLISHED),
            PropertyStatus.PUBLISHED, EnumSet.of(PropertyStatus.ARCHIVED)));

    @Test
    void validTransitionAppliesTargetAndPublishesContextOnce() {
        StatusTransitionService service = new StatusTransitionService(publisher);
        AtomicReference<PropertyStatus> status = new AtomicReference<>(PropertyStatus.DRAFT);

        service.transition("Property", 42L, status.get(), PropertyStatus.PUBLISHED,
                "user:9", transitions, status::set);

        assertThat(status.get()).isEqualTo(PropertyStatus.PUBLISHED);
        ArgumentCaptor<StatusChangedEvent> event = ArgumentCaptor.forClass(StatusChangedEvent.class);
        verify(publisher).publishEvent(event.capture());
        assertThat(event.getValue().entityType()).isEqualTo("Property");
        assertThat(event.getValue().entityId()).isEqualTo("42");
        assertThat(event.getValue().previousStatus()).isEqualTo("DRAFT");
        assertThat(event.getValue().newStatus()).isEqualTo("PUBLISHED");
        assertThat(event.getValue().actor()).isEqualTo("user:9");
        assertThat(event.getValue().occurredAt()).isNotNull();
    }

    @Test
    void invalidTransitionDoesNotApplyOrPublish() {
        StatusTransitionService service = new StatusTransitionService(publisher);
        AtomicReference<PropertyStatus> status = new AtomicReference<>(PropertyStatus.ARCHIVED);

        assertThatThrownBy(() -> service.transition("Property", 42L, status.get(), PropertyStatus.DRAFT,
                "user:9", transitions, status::set)).isInstanceOf(InvalidStatusTransitionException.class);

        assertThat(status.get()).isEqualTo(PropertyStatus.ARCHIVED);
        verifyNoInteractions(publisher);
    }
}
