package com.sakan.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class DemoDatasetGeneratorTest {

    private static final List<Long> PROPERTY_TYPE_IDS = List.of(1L, 2L, 3L, 4L, 5L, 6L);
    private static final List<Long> LOCATION_IDS = List.of(11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L);

    @Mock
    private JdbcTemplate jdbc;

    @Test
    void generatesFiveThousandListingsAcrossEveryTypeAndLocation() {
        when(jdbc.queryForObject(contains("FROM users"), eq(Long.class))).thenReturn(100L);
        when(jdbc.queryForObject(contains("FROM property WHERE title LIKE"), eq(Long.class))).thenReturn(0L);
        when(jdbc.queryForList(contains("FROM property_type"), eq(Long.class))).thenReturn(PROPERTY_TYPE_IDS);
        when(jdbc.queryForList(contains("FROM location"), eq(Long.class))).thenReturn(LOCATION_IDS);
        when(jdbc.batchUpdate(anyString(), anyList())).thenAnswer(invocation -> {
            List<?> rows = invocation.getArgument(1);
            return new int[rows.size()];
        });

        new DemoDatasetGenerator(jdbc).generateIfNeeded();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Object[]>> batch = ArgumentCaptor.forClass(List.class);
        verify(jdbc).batchUpdate(anyString(), batch.capture());
        assertThat(batch.getValue()).hasSize(5000);
        assertThat(batch.getValue().stream().map(row -> row[4]).collect(Collectors.toSet()))
                .containsExactlyInAnyOrderElementsOf(PROPERTY_TYPE_IDS);
        assertThat(batch.getValue().stream().map(row -> row[5]).collect(Collectors.toSet()))
                .containsExactlyInAnyOrderElementsOf(LOCATION_IDS);
        assertThat(batch.getValue().stream().map(row -> row[1]).collect(Collectors.toSet())).hasSize(5000);
    }

    @Test
    void doesNotInsertAnotherBatchWhenDemoListingsAlreadyExist() {
        when(jdbc.queryForObject(contains("FROM property WHERE title LIKE"), eq(Long.class))).thenReturn(5000L);

        new DemoDatasetGenerator(jdbc).generateIfNeeded();

        verify(jdbc).queryForObject(contains("FROM property WHERE title LIKE"), eq(Long.class));
        verifyNoMoreInteractions(jdbc);
    }

    @Test
    void demoGeneratorBeanIsNotRegisteredWithTheTestProfile() {
        new ApplicationContextRunner()
                .withUserConfiguration(DemoProfileConfiguration.class)
                .withPropertyValues("spring.profiles.active=test")
                .run(context -> assertThat(context).doesNotHaveBean(DemoDatasetGenerator.class));
    }

    @Configuration(proxyBeanMethods = false)
    @Import(DemoDatasetGenerator.class)
    static class DemoProfileConfiguration {}
}
