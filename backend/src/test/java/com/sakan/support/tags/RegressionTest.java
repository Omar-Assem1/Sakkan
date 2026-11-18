package com.sakan.support.tags;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;

/**
 * Marks a test class or test method as part of the Milestone regression suite
 * ({@code @Tag("regression")}).
 *
 * <p>Run only regression tests from Maven with:
 * <pre>{@code ./mvnw -B test -Dgroups=regression}</pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Tag("regression")
public @interface RegressionTest {
}
