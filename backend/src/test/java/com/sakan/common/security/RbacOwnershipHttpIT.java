package com.sakan.common.security;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.sakan.common.config.JwtProperties;
import com.sakan.common.domain.Currency;
import com.sakan.property.domain.Property;
import com.sakan.property.domain.PropertyPurpose;
import com.sakan.property.domain.PropertyStatus;
import com.sakan.property.repository.PropertyRepository;
import com.sakan.support.AbstractIntegrationTest;
import com.sakan.support.tags.RegressionTest;
import com.sakan.user.domain.AuthProvider;
import com.sakan.user.domain.Role;
import com.sakan.user.domain.RoleName;
import com.sakan.user.domain.User;
import com.sakan.user.repository.RoleRepository;
import com.sakan.user.repository.UserRepository;

/**
 * SCRUM-65 / NFR-03: ownership is enforced in the service layer, proven through the whole HTTP chain
 * (real JWT, real filter chain, real service, real database).
 */
@RegressionTest
@AutoConfigureMockMvc
@Transactional
class RbacOwnershipHttpIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "Passw0rd1";
    private static final String PHONE = "+201012345678";

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtProperties jwtProperties;

    private User owner;
    private User otherOwner;
    private User agent;
    private User buyer;
    private User admin;
    private Property property;

    @BeforeEach
    void setUp() {
        owner = createUser("own_owner", RoleName.OWNER);
        otherOwner = createUser("own_other_owner", RoleName.OWNER);
        agent = createUser("own_agent", RoleName.AGENT);
        buyer = createUser("own_buyer", RoleName.BUYER_TENANT);
        admin = createUser("own_admin", RoleName.ADMIN);

        Property p = new Property();
        p.setOwnerId(owner.getId());
        p.setAgentId(agent.getId());
        p.setTitle("Ownership test flat");
        p.setPurpose(PropertyPurpose.values()[0]);
        p.setPropertyTypeId(1L);
        p.setLocationId(1L);
        p.setPrice(new BigDecimal("1000.00"));
        p.setCurrency(Currency.values()[0]);
        p.setBedrooms(2);
        p.setBathrooms(1);
        p.setAreaSqm(new BigDecimal("90.00"));
        p.setStatus(PropertyStatus.DRAFT);
        property = propertyRepository.saveAndFlush(p);
    }

    // ---------- helpers ----------

    private User createUser(String username, RoleName... roleNames) {
        Set<Role> roles = Arrays.stream(roleNames)
                .map(n -> roleRepository.findByName(n).orElseThrow())
                .collect(Collectors.toCollection(HashSet::new));
        return userRepository.save(User.builder()
                .username(username).email(username + "@sakan.com")
                .passwordHash(passwordEncoder.encode(PASSWORD)).phone(PHONE)
                .authProvider(AuthProvider.LOCAL).enabled(true)
                .roles(roles).build());
    }

    private String tokenFor(User user, String role) {
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(jwtProperties.secret().getBytes(UTF_8)));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(user.getId())).issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(Duration.ofHours(1)))
                .claim("username", user.getUsername()).claim("roles", List.of(role)).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private String updateBody() {
        return "{\"title\":\"Changed title\",\"description\":\"d\",\"purpose\":\"" + PropertyPurpose.values()[0].name()
                + "\",\"propertyTypeId\":1,\"locationId\":1,\"price\":1000.00,\"currency\":\""
                + Currency.values()[0].name() + "\",\"bedrooms\":2,\"bathrooms\":1,\"areaSqm\":90.00}";
    }

    private int update(User caller, String role) throws Exception {
        return mvc.perform(put("/api/properties/" + property.getId())
                        .header("Authorization", "Bearer " + tokenFor(caller, role))
                        .contentType(MediaType.APPLICATION_JSON).content(updateBody()))
                .andReturn().getResponse().getStatus();
    }

    private int postAction(String action, User caller, String role) throws Exception {
        return mvc.perform(post("/api/properties/" + property.getId() + "/" + action)
                        .header("Authorization", "Bearer " + tokenFor(caller, role))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn().getResponse().getStatus();
    }

    // ---------- ownership ----------

    @Test
    @DisplayName("O01 another OWNER cannot update, publish or archive someone else's property (403)")
    void otherOwner_cannotManage() throws Exception {
        assertThat(update(otherOwner, "OWNER")).isEqualTo(403);
        assertThat(postAction("publish", otherOwner, "OWNER")).isEqualTo(403);
        assertThat(postAction("archive", otherOwner, "OWNER")).isEqualTo(403);
    }

    @Test
    @DisplayName("O02 the owner passes the role and ownership checks")
    void owner_canUpdate() throws Exception {
        assertThat(update(owner, "OWNER")).isEqualTo(200);
    }

    @Test
    @DisplayName("O03 the assigned AGENT passes the role and ownership checks")
    void assignedAgent_passesChecks() throws Exception {
        assertThat(update(agent, "AGENT")).isNotIn(401, 403);
    }

    @Test
    @DisplayName("O04 BUYER_TENANT is blocked by role before ownership is even looked at (403)")
    void buyer_blockedByRole() throws Exception {
        assertThat(update(buyer, "BUYER_TENANT")).isEqualTo(403);
        assertThat(postAction("publish", buyer, "BUYER_TENANT")).isEqualTo(403);
    }

    @Test
    @DisplayName("O05 CURRENT BEHAVIOUR: ADMIN is not in the OWNER/AGENT rule, so ADMIN cannot manage listings (403)")
    void admin_cannotManageListings() throws Exception {
        assertThat(update(admin, "ADMIN")).isEqualTo(403);
        assertThat(postAction("archive", admin, "ADMIN")).isEqualTo(403);
    }

    @Test
    @DisplayName("O06 another OWNER cannot delete images of someone else's property (403)")
    void otherOwner_cannotDeleteImages() throws Exception {
        int status = mvc.perform(delete("/api/properties/" + property.getId() + "/images/1")
                        .header("Authorization", "Bearer " + tokenFor(otherOwner, "OWNER")))
                .andReturn().getResponse().getStatus();
        assertThat(status).isEqualTo(403);
    }

    // ---------- draft visibility ----------

    @Test
    @DisplayName("O07 images of a DRAFT property are hidden (404) from anonymous callers and other owners")
    void draftImages_hiddenFromOutsiders() throws Exception {
        String url = "/api/properties/" + property.getId() + "/images";

        assertThat(mvc.perform(get(url)).andReturn().getResponse().getStatus()).isEqualTo(404);
        assertThat(mvc.perform(get(url).header("Authorization", "Bearer " + tokenFor(otherOwner, "OWNER")))
                .andReturn().getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("O08 images of a DRAFT property are visible to its owner and its agent")
    void draftImages_visibleToManagers() throws Exception {
        String url = "/api/properties/" + property.getId() + "/images";

        assertThat(mvc.perform(get(url).header("Authorization", "Bearer " + tokenFor(owner, "OWNER")))
                .andReturn().getResponse().getStatus()).isEqualTo(200);
        assertThat(mvc.perform(get(url).header("Authorization", "Bearer " + tokenFor(agent, "AGENT")))
                .andReturn().getResponse().getStatus()).isEqualTo(200);
    }
}