package com.sakan.support.tags;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;

/**
 * Marks a test class or test method as an integration test ({@code @Tag("integration")}),
 * typically backed by Testcontainers/PostgreSQL.
 *
 * <p>Run only integration tests with {@code ./mvnw -B test -Dgroups=integration},
 * or skip them with {@code ./mvnw -B test -DexcludedGroups=integration}.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Tag("integration")
public @interface IntegrationTest {
}
