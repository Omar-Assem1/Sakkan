package com.sakan.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.sakan.TestcontainersConfiguration;
import com.sakan.support.tags.IntegrationTest;

/**
 * Base class for integration tests that need the full Spring context and a real database.
 *
 * <p><b>How to use:</b> extend this class instead of repeating
 * {@code @SpringBootTest @Import(TestcontainersConfiguration.class)} on every test.
 *
 * <pre>{@code
 * @Transactional            // roll back every test, so tests do not see each other's data
 * class PropertyRepositoryIT extends AbstractIntegrationTest {
 *
 *     @Autowired PropertyRepository repository;
 *
 *     @Test
 *     void savesProperty() { ... }
 * }
 * }</pre>
 *
 * <ul>
 *   <li><b>One context, one container:</b> all subclasses share the same cached Spring context
 *       and the same PostGIS container ({@code postgis/postgis}, same image as docker-compose.yml),
 *       so the database starts once per test run, not once per class.</li>
 *   <li><b>Keep the context identical:</b> avoid {@code @MockitoBean}/{@code @MockBean},
 *       {@code @TestPropertySource}, {@code @ActiveProfiles} or {@code @DirtiesContext} on subclasses
 *       unless you really need them. Every different combination creates a NEW Spring context and a NEW
 *       container, which makes the suite much slower.</li>
 *   <li><b>Clean data:</b> put {@code @Transactional} on the test class so each test is rolled back.</li>
 *   <li><b>Never your local DB:</b> the datasource comes from the container via {@code @ServiceConnection},
 *       so integration tests never touch a developer's local PostgreSQL.</li>
 *   <li>Requires Docker to be running. Tagged {@code integration}, so it can be run or skipped with
 *       {@code -Dgroups=integration} / {@code -DexcludedGroups=integration}.</li>
 * </ul>
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@IntegrationTest
public abstract class AbstractIntegrationTest {
}
