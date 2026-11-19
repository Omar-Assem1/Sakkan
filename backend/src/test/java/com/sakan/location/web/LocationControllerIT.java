package com.sakan.location.web;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.sakan.support.AbstractIntegrationTest;

@AutoConfigureMockMvc
@Transactional
class LocationControllerIT extends AbstractIntegrationTest {

    private static final String BASE_URL = "/api/locations";
    private static final String ACCEPT_LANGUAGE = "Accept-Language";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Public governorate endpoint returns the seeded roots with English names by default")
    void governorates_defaultToEnglishAndArePublic() throws Exception {
        mvc.perform(get(BASE_URL + "/governorates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Alexandria", "Cairo", "Giza")))
                .andExpect(jsonPath("$[*].type", everyItem(is("GOVERNORATE"))))
                .andExpect(jsonPath("$[*].parentId", everyItem(is((Object) null))));
    }

    @Test
    @DisplayName("Accept-Language selects Arabic names for governorates")
    void governorates_localizeNamesToArabic() throws Exception {
        mvc.perform(get(BASE_URL + "/governorates").header(ACCEPT_LANGUAGE, "ar-EG, en;q=0.8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", containsInAnyOrder(
                        "\u0627\u0644\u0642\u0627\u0647\u0631\u0629",
                        "\u0627\u0644\u0625\u0633\u0643\u0646\u062f\u0631\u064a\u0629",
                        "\u0627\u0644\u062c\u064a\u0632\u0629")));
    }

    @Test
    @DisplayName("Cities are returned only for the requested governorate")
    void cities_filterByGovernorateParent() throws Exception {
        long cairoId = locationId("Cairo", "GOVERNORATE");

        mvc.perform(get(BASE_URL + "/cities").param("parentId", String.valueOf(cairoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("New Cairo", "Nasr City", "Heliopolis")))
                .andExpect(jsonPath("$[*].type", everyItem(is("CITY"))))
                .andExpect(jsonPath("$[*].parentId", everyItem(is((int) cairoId))));
    }

    @Test
    @DisplayName("Districts are returned only for the requested city")
    void districts_filterByCityParent() throws Exception {
        long newCairoId = locationId("New Cairo", "CITY");

        mvc.perform(get(BASE_URL + "/districts").param("parentId", String.valueOf(newCairoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("First Settlement", "Fifth Settlement")))
                .andExpect(jsonPath("$[*].type", everyItem(is("DISTRICT"))))
                .andExpect(jsonPath("$[*].parentId", everyItem(is((int) newCairoId))));
    }

    @Test
    @DisplayName("Compounds are returned only for the requested district")
    void compounds_filterByDistrictParent() throws Exception {
        long fifthSettlementId = locationId("Fifth Settlement", "DISTRICT");

        mvc.perform(get(BASE_URL + "/compounds").param("parentId", String.valueOf(fifthSettlementId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Madinaty", "Al Rehab", "Mountain View iCity")))
                .andExpect(jsonPath("$[*].type", everyItem(is("COMPOUND"))))
                .andExpect(jsonPath("$[*].parentId", everyItem(is((int) fifthSettlementId))));
    }

    @Test
    @DisplayName("A parent of the wrong type or a missing parent returns no children")
    void children_wrongOrUnknownParent_returnsEmpty() throws Exception {
        long districtId = locationId("Fifth Settlement", "DISTRICT");

        mvc.perform(get(BASE_URL + "/cities").param("parentId", String.valueOf(districtId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(get(BASE_URL + "/districts").param("parentId", "999999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Child endpoints require a parent ID")
    void childEndpoints_requireParentId() throws Exception {
        mvc.perform(get(BASE_URL + "/cities"))
                .andExpect(status().isBadRequest());

        mvc.perform(get(BASE_URL + "/districts"))
                .andExpect(status().isBadRequest());

        mvc.perform(get(BASE_URL + "/compounds"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Unsupported Accept-Language values fall back to English")
    void unsupportedLanguage_fallsBackToEnglish() throws Exception {
        mvc.perform(get(BASE_URL + "/governorates").header(ACCEPT_LANGUAGE, "fr-FR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Cairo")));
    }

    private long locationId(String name, String type) {
        Long id = jdbc.queryForObject(
                "SELECT id FROM location WHERE name_en = ? AND location_type = ?",
                Long.class,
                name,
                type);
        return id;
    }
}
