package com.sakan.support.tags;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;

import com.sakan.support.AbstractIntegrationTest;

/**
 * Verifies that {@link RegressionTest}, {@link IntegrationTest}, and {@link UnitTest}
 * compose JUnit 5 {@code @Tag} annotations and are inherited by subclasses, and that
 * the Milestone 1 property browse suite carries {@code @RegressionTest}.
 */
@UnitTest
class TestTagAnnotationsTest {

    @Test
    @DisplayName("@RegressionTest, @IntegrationTest, and @UnitTest expose expected JUnit 5 @Tag values")
    void metaAnnotationsCarryExpectedJUnitTags() throws ClassNotFoundException {
        assertThat(tagsOf(SampleUnitTagged.class)).containsExactly("unit");
        assertThat(tagsOf(SampleRegressionTagged.class)).containsExactly("regression");
        assertThat(tagsOf(AbstractIntegrationTest.class)).containsExactly("integration");
        // Subclasses of AbstractIntegrationTest inherit @IntegrationTest via @Inherited
        assertThat(tagsOf("com.sakan.support.AbstractIntegrationTestSmokeTest")).contains("integration");
    }

    @Test
    @DisplayName("Milestone 1 property browse suite is tagged with @RegressionTest")
    void milestone1PropertyBrowseTestsAreTaggedRegression() throws ClassNotFoundException {
        assertThat(tagsOf("com.sakan.property.web.PropertyBrowseControllerTest")).contains("regression");
        assertThat(tagsOf("com.sakan.property.service.PropertyBrowseServiceTest")).contains("regression");
        assertThat(tagsOf("com.sakan.property.service.PropertySortValidatorTest")).contains("regression");
        assertThat(tagsOf("com.sakan.property.web.PropertySecurityTest")).contains("regression");
        assertThat(tagsOf("com.sakan.property.repository.PropertyBrowseRepositoryTest"))
                .contains("regression", "integration");
        assertThat(tagsOf("com.sakan.SakanApplicationTests")).contains("integration");
    }

    private static Set<String> tagsOf(String className) throws ClassNotFoundException {
        return tagsOf(Class.forName(className));
    }

    private static Set<String> tagsOf(Class<?> testClass) {
        return AnnotatedElementUtils.findMergedRepeatableAnnotations(testClass, Tag.class)
                .stream()
                .map(Tag::value)
                .collect(Collectors.toSet());
    }

    @UnitTest
    private static class SampleUnitTagged {
    }

    @RegressionTest
    private static class SampleRegressionTagged {
    }
}
