package com.sakan.property.search;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.sakan.common.exception.GlobalExceptionHandler;
import com.sakan.property.domain.Property;
import com.sakan.property.repository.PropertyRepository;
import com.sakan.property.web.PropertyController;
import com.sakan.support.AbstractIntegrationTest;
import com.sakan.support.TestUserFixtures;
import com.sakan.support.tags.IntegrationTest;
import com.sakan.support.tags.RegressionTest;

import jakarta.persistence.EntityManager;

@RegressionTest
@IntegrationTest
@Transactional
class PropertySearchIntegrationTest extends AbstractIntegrationTest {

    public static final double TRIGRAM_SIMILARITY_THRESHOLD = 0.3;

    @Autowired
    private PropertyController propertyController;

    private MockMvc mockMvc;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Property enSmouha;
    private Property arSmouha;
    private Property arDiacritics;
    private Property descOlympic;
    private Property descGarden;
    private Property typoChalet;
    private Property draftSmouha;
    private Property archivedSmouha;

    @BeforeEach
    void setUp() {
        TestUserFixtures.ensureUsers(jdbcTemplate, 100L, 101L, 102L, 103L, 104L, 105L);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(propertyController)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        propertyRepository.deleteAll();

        enSmouha = entityManager.merge(PropertySearchTestFixtures.publishedEnglishSmouha());
        arSmouha = entityManager.merge(PropertySearchTestFixtures.publishedArabicSmouha());
        arDiacritics = entityManager.merge(PropertySearchTestFixtures.publishedArabicDiacriticsAndVariants());
        descOlympic = entityManager.merge(PropertySearchTestFixtures.publishedDescriptionOnlyOlympic());
        descGarden = entityManager.merge(PropertySearchTestFixtures.publishedArabicDescriptionOnlyGarden());
        typoChalet = entityManager.merge(PropertySearchTestFixtures.publishedTypoCompoundTarget());
        draftSmouha = entityManager.merge(PropertySearchTestFixtures.draftSmouha());
        archivedSmouha = entityManager.merge(PropertySearchTestFixtures.archivedSmouha());

        entityManager.flush();
    }

    @Test
    @DisplayName("SCRUM-88: English title search returns matching published listings")
    void testSearchEnglishTitleMatch() throws Exception {
        mockMvc.perform(get("/api/properties")
                        .param("q", "Smouha")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(enSmouha.getId().intValue())))
                .andExpect(jsonPath("$.content[0].title", containsString("Smouha")));
    }

    @Test
    @DisplayName("SCRUM-88: Arabic title search returns matching published listings")
    void testSearchArabicTitleMatch() throws Exception {
        mockMvc.perform(get("/api/properties")
                        .param("q", "فاخرة")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(arSmouha.getId().intValue())))
                .andExpect(jsonPath("$.content[0].title", containsString("فاخرة")));
    }

    @Test
    @DisplayName("SCRUM-88: English search finds property when match is only in description")
    void testSearchEnglishMatchInDescriptionOnly() throws Exception {
        mockMvc.perform(get("/api/properties")
                        .param("q", "olympic")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(descOlympic.getId().intValue())))
                .andExpect(jsonPath("$.content[0].description", containsString("olympic")));
    }

    @Test
    @DisplayName("SCRUM-88: Arabic search finds property when match is only in description")
    void testSearchArabicMatchInDescriptionOnly() throws Exception {
        mockMvc.perform(get("/api/properties")
                        .param("q", "حديقة واسعة")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(descGarden.getId().intValue())))
                .andExpect(jsonPath("$.content[0].description", containsString("حديقة واسعة")));
    }

    @Test
    @DisplayName("SCRUM-88: Search combined with filters, pagination, and sorting works simultaneously")
    void testSearchCombinedWithFiltersPaginationAndSort() throws Exception {
        mockMvc.perform(get("/api/properties")
                        .param("q", "سموحة")
                        .param("purpose", "SALE")
                        .param("minPrice", "2000000")
                        .param("maxPrice", "3500000")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "price,desc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(arSmouha.getId().intValue())))
                .andExpect(jsonPath("$.content[0].title", containsString("سموحة")));
    }

    @Test
    @DisplayName("SCRUM-88: Typo tolerance via pg_trgm similarity matches title with typos (threshold >= 0.3)")
    void testSearchTypoToleranceTrigramSimilarity() throws Exception {
        mockMvc.perform(get("/api/properties")
                        .param("q", "Aleksandria")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(typoChalet.getId().intValue())))
                .andExpect(jsonPath("$.content[0].title", containsString("Alexandria")));

        mockMvc.perform(get("/api/properties")
                        .param("q", "سموحه")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(arSmouha.getId().intValue())))
                .andExpect(jsonPath("$.content[0].title", containsString("سموحة")));
    }

    @Test
    @DisplayName("SCRUM-88: Blank or empty query string behaves as no search filter")
    void testSearchBlankOrEmptyQueryBehavesLikeNoSearch() throws Exception {
        mockMvc.perform(get("/api/properties")
                        .param("q", "   ")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(6)));
    }

    @Test
    @DisplayName("SCRUM-88: English search is case-insensitive")
    void testSearchCaseInsensitivity() throws Exception {
        mockMvc.perform(get("/api/properties")
                        .param("q", "sMoUhA")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(enSmouha.getId().intValue())));
    }

    @Test
    @DisplayName("SCRUM-88: Arabic search normalizes diacritics (tashkeel) and Alef hamza variants")
    void testSearchArabicDiacriticsAndAlefNormalization() throws Exception {
        mockMvc.perform(get("/api/properties")
                        .param("q", "الاسكندرية")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(arDiacritics.getId().intValue())));

        mockMvc.perform(get("/api/properties")
                        .param("q", "شَقَّةٌ")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(arSmouha.getId().intValue())));
    }

    @Test
    @DisplayName("SCRUM-88: Search returns only PUBLISHED listings; never DRAFT or ARCHIVED")
    void testSearchOnlyPublishedReturned() throws Exception {
        mockMvc.perform(get("/api/properties")
                        .param("q", "مسودة")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)))
                .andExpect(jsonPath("$.content", hasSize(0)));

        mockMvc.perform(get("/api/properties")
                        .param("q", "Smouha")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].status", everyItem(is("PUBLISHED"))))
                .andExpect(jsonPath("$.content[*].id", not(hasItem(draftSmouha.getId().intValue()))))
                .andExpect(jsonPath("$.content[*].id", not(hasItem(archivedSmouha.getId().intValue()))));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "' OR 1=1 --",
            "\" OR \"\"=\"",
            "'; DROP TABLE property; --",
            "1'; SELECT * FROM property WHERE '1'='1"
    })
    @DisplayName("SCRUM-88: Malicious SQL injection payloads in search parameter return empty or safe results without 500 error")
    void testSearchSqlInjectionHandledSafely(String maliciousPayload) throws Exception {
        mockMvc.perform(get("/api/properties")
                        .param("q", maliciousPayload)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    @DisplayName("SCRUM-88: Search query exceeding 100 characters returns 400 Bad Request ProblemDetail")
    void testSearchOverLongQueryReturns400() throws Exception {
        String overLongQuery = "a".repeat(101);
        mockMvc.perform(get("/api/properties")
                        .param("q", overLongQuery)
                        .accept(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));
    }
}
