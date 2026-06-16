package id.payu.cms.repository;

import id.payu.cms.adapter.persistence.entity.ContentEntity;
import id.payu.cms.domain.entity.ContentStatus;
import id.payu.cms.domain.repository.ContentRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ContentRepository using Testcontainers PostgreSQL.
 *
 * Migration note: @DataJpaTest and @AutoConfigureTestDatabase were
 * REMOVED in Spring Boot 4.0+. Replaced with @SpringBootTest +
 * @AutoConfigureTestEntityManager (the closest equivalent providing
 * a TestEntityManager in a full Spring context). Testcontainers is
 * still used to provide the real PostgreSQL database.
 */
@Disabled("READY-055: Testcontainers + podman socket works (postgres:16-alpine starts OK) but Flyway says 'jdbcUrl is required' — application.yml hardcodes spring.datasource.url + @DynamicPropertySource not winning. Needs precedence fix (e.g. spring.flyway.url explicit + remove hardcoded url from app.yml) OR use @ServiceConnection for auto-config.")
@SpringBootTest
@Testcontainers
@AutoConfigureTestEntityManager
@Tag("integration")
@DisplayName("ContentRepository Integration Tests")
class ContentRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("cms_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private ContentRepository contentRepository;

    private ContentEntity banner1;
    private ContentEntity banner2;
    private ContentEntity promo1;
    private ContentEntity scheduledContent;
    private ContentEntity expiredContent;
    private ContentEntity alertContent;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeAll
    static void startContainer() {
        postgres.start();
    }

    @AfterAll
    static void stopContainer() {
        if (postgres != null && postgres.isRunning()) {
            postgres.stop();
        }
    }

    @BeforeEach
    void setUp() {
        contentRepository.deleteAll();

        Map<String, Object> targetingAll = new HashMap<>();
        targetingAll.put("segment", "ALL");

        Map<String, Object> targetingPremium = new HashMap<>();
        targetingPremium.put("segment", "PREMIUM");
        targetingPremium.put("location", "JAKARTA");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("campaign", "TEST_2026");

        banner1 = ContentEntity.builder()
                .contentType("BANNER")
                .title("Welcome Banner")
                .description("Welcome to PayU")
                .imageUrl("https://cdn.example.com/welcome.png")
                .actionUrl("https://payu.example.com/welcome")
                .actionType("LINK")
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().plusDays(25))
                .priority(100)
                .status(ContentStatus.ACTIVE)
                .targetingRules(targetingAll)
                .metadata(metadata)
                .version(1)
                .createdBy("admin")
                .updatedBy("admin")
                .build();

        banner2 = ContentEntity.builder()
                .contentType("BANNER")
                .title("Promo Banner")
                .description("Special promo")
                .imageUrl("https://cdn.example.com/promo.png")
                .actionUrl("https://payu.example.com/promo")
                .actionType("LINK")
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(30))
                .priority(50)
                .status(ContentStatus.ACTIVE)
                .targetingRules(targetingPremium)
                .metadata(new HashMap<>())
                .version(1)
                .createdBy("editor")
                .updatedBy("editor")
                .build();

        promo1 = ContentEntity.builder()
                .contentType("PROMO")
                .title("Weekend Cashback")
                .description("20% cashback on weekends")
                .imageUrl("https://cdn.example.com/cashback.png")
                .actionUrl("https://payu.example.com/weekend")
                .actionType("DEEP_LINK")
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().plusDays(20))
                .priority(90)
                .status(ContentStatus.ACTIVE)
                .targetingRules(targetingAll)
                .metadata(new HashMap<>())
                .version(1)
                .createdBy("admin")
                .updatedBy("admin")
                .build();

        scheduledContent = ContentEntity.builder()
                .contentType("ALERT")
                .title("Scheduled Maintenance")
                .description("System maintenance notice")
                .actionType("DISMISS")
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(10))
                .priority(200)
                .status(ContentStatus.SCHEDULED)
                .targetingRules(targetingAll)
                .metadata(new HashMap<>())
                .version(1)
                .createdBy("admin")
                .updatedBy("admin")
                .build();

        expiredContent = ContentEntity.builder()
                .contentType("BANNER")
                .title("Old Banner")
                .description("This banner has expired")
                .imageUrl("https://cdn.example.com/old.png")
                .actionType("LINK")
                .startDate(LocalDate.now().minusDays(30))
                .endDate(LocalDate.now().minusDays(1))
                .priority(10)
                .status(ContentStatus.ACTIVE)
                .targetingRules(targetingAll)
                .metadata(new HashMap<>())
                .version(1)
                .createdBy("admin")
                .updatedBy("admin")
                .build();

        alertContent = ContentEntity.builder()
                .contentType("ALERT")
                .title("System Alert")
                .description("Important system alert")
                .actionType("DISMISS")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .priority(150)
                .status(ContentStatus.DRAFT)
                .targetingRules(targetingAll)
                .metadata(new HashMap<>())
                .version(1)
                .createdBy("admin")
                .updatedBy("admin")
                .build();

        contentRepository.saveAll(List.of(
                banner1, banner2, promo1, scheduledContent, expiredContent, alertContent
        ));
    }

    @Test
    @DisplayName("Should persist and retrieve content by ID")
    void shouldPersistAndRetrieveContentById() {
        Optional<ContentEntity> found = contentRepository.findById(banner1.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Welcome Banner");
        assertThat(found.get().getContentType()).isEqualTo("BANNER");
        assertThat(found.get().getStatus()).isEqualTo(ContentStatus.ACTIVE);
        assertThat(found.get().getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return empty Optional for non-existent ID")
    void shouldReturnEmptyForNonExistentId() {
        Optional<ContentEntity> found = contentRepository.findById(UUID.randomUUID());
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should update content and persist changes")
    void shouldUpdateContentAndPersistChanges() {
        ContentEntity content = contentRepository.findById(banner1.getId()).orElseThrow();
        content.setTitle("Updated Banner Title");
        content.setPriority(200);
        content.setVersion(2);
        contentRepository.saveAndFlush(content);

        ContentEntity updated = contentRepository.findById(banner1.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("Updated Banner Title");
        assertThat(updated.getPriority()).isEqualTo(200);
        assertThat(updated.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should delete content by ID")
    void shouldDeleteContentById() {
        contentRepository.deleteById(banner1.getId());
        contentRepository.flush();
        assertThat(contentRepository.existsById(banner1.getId())).isFalse();
    }

    @Test
    @DisplayName("Should check content existence by ID")
    void shouldCheckContentExistenceById() {
        assertThat(contentRepository.existsById(banner1.getId())).isTrue();
        assertThat(contentRepository.existsById(UUID.randomUUID())).isFalse();
    }

    @Test
    @DisplayName("Should find active content by type with current date")
    void shouldFindActiveContentByType() {
        List<ContentEntity> activeBanners = contentRepository.findActiveByContentType("BANNER", LocalDate.now());
        assertThat(activeBanners).hasSize(2);
        assertThat(activeBanners).extracting(ContentEntity::getStatus)
                .allMatch(s -> s == ContentStatus.ACTIVE);
        assertThat(activeBanners.get(0).getPriority()).isGreaterThanOrEqualTo(
                activeBanners.get(1).getPriority());
    }

    @Test
    @DisplayName("Should exclude expired content from active results")
    void shouldExcludeExpiredContentFromActive() {
        List<ContentEntity> activeBanners = contentRepository.findActiveByContentType("BANNER", LocalDate.now());
        assertThat(activeBanners).extracting(ContentEntity::getTitle)
                .doesNotContain("Old Banner");
    }

    @Test
    @DisplayName("Should find content by status")
    void shouldFindContentByStatus() {
        List<ContentEntity> drafts = contentRepository.findByStatus(ContentStatus.DRAFT);
        assertThat(drafts).hasSize(1);
        assertThat(drafts.get(0).getTitle()).isEqualTo("System Alert");

        List<ContentEntity> active = contentRepository.findByStatus(ContentStatus.ACTIVE);
        assertThat(active).hasSize(4);

        List<ContentEntity> scheduled = contentRepository.findByStatus(ContentStatus.SCHEDULED);
        assertThat(scheduled).hasSize(1);
    }

    @Test
    @DisplayName("Should find content by type")
    void shouldFindContentByType() {
        List<ContentEntity> banners = contentRepository.findByContentType("BANNER");
        assertThat(banners).hasSize(3);
        assertThat(banners).extracting(ContentEntity::getContentType).allMatch(t -> t.equals("BANNER"));

        List<ContentEntity> promos = contentRepository.findByContentType("PROMO");
        assertThat(promos).hasSize(1);

        List<ContentEntity> alerts = contentRepository.findByContentType("ALERT");
        assertThat(alerts).hasSize(2);
    }

    @Test
    @DisplayName("Should find content by type with pagination")
    void shouldFindContentByTypeWithPagination() {
        Page<ContentEntity> page1 = contentRepository.findByContentType("BANNER", PageRequest.of(0, 2));
        assertThat(page1.getContent()).hasSize(2);
        assertThat(page1.getTotalElements()).isEqualTo(3);
        assertThat(page1.getTotalPages()).isEqualTo(2);

        Page<ContentEntity> page2 = contentRepository.findByContentType("BANNER", PageRequest.of(1, 2));
        assertThat(page2.getContent()).hasSize(1);
        assertThat(page2.isLast()).isTrue();
    }

    @Test
    @DisplayName("Should find content by title (case-insensitive)")
    void shouldFindContentByTitleIgnoreCase() {
        Optional<ContentEntity> found = contentRepository.findByTitleIgnoreCase("welcome banner");
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Welcome Banner");

        Optional<ContentEntity> notFound = contentRepository.findByTitleIgnoreCase("nonexistent");
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("Should check existence by title (case-insensitive)")
    void shouldCheckExistenceByTitleIgnoreCase() {
        assertThat(contentRepository.existsByTitleIgnoreCase("WELCOME BANNER")).isTrue();
        assertThat(contentRepository.existsByTitleIgnoreCase("welcome banner")).isTrue();
        assertThat(contentRepository.existsByTitleIgnoreCase("Non-existent")).isFalse();
    }

    @Test
    @DisplayName("Should find scheduled content to activate")
    void shouldFindScheduledContentToActivate() {
        List<ContentEntity> toActivate = contentRepository.findScheduledToActivate(LocalDate.now().plusDays(6));
        assertThat(toActivate).hasSize(1);
    }

    @Test
    @DisplayName("Should find expired active content to archive")
    void shouldFindExpiredActiveContent() {
        List<ContentEntity> toArchive = contentRepository.findActiveToArchive(LocalDate.now());
        assertThat(toArchive).hasSize(1);
        assertThat(toArchive.get(0).getTitle()).isEqualTo("Old Banner");
    }

    @Test
    @DisplayName("Should find content by creator")
    void shouldFindContentByCreator() {
        List<ContentEntity> byAdmin = contentRepository.findByCreatedBy("admin");
        assertThat(byAdmin).hasSize(5);
        List<ContentEntity> byEditor = contentRepository.findByCreatedBy("editor");
        assertThat(byEditor).hasSize(1);
    }

    @Test
    @DisplayName("Should delete content by status")
    void shouldDeleteContentByStatus() {
        contentRepository.deleteByStatus(ContentStatus.SCHEDULED);
        contentRepository.flush();
        assertThat(contentRepository.findByStatus(ContentStatus.SCHEDULED)).isEmpty();
    }

    @Test
    @DisplayName("Should paginate all content")
    void shouldPaginateAllContent() {
        Page<ContentEntity> page1 = contentRepository.findAll(PageRequest.of(0, 3));
        assertThat(page1.getContent()).hasSize(3);
        assertThat(page1.getTotalElements()).isEqualTo(6);
    }

    @Test
    @DisplayName("Should persist and retrieve JSONB targeting rules")
    void shouldPersistJsonbTargetingRules() {
        ContentEntity found = contentRepository.findById(banner2.getId()).orElseThrow();
        assertThat(found.getTargetingRules()).isNotNull();
        assertThat(found.getTargetingRules()).containsEntry("segment", "PREMIUM");
        assertThat(found.getTargetingRules()).containsEntry("location", "JAKARTA");
    }

    @Test
    @DisplayName("Should persist and retrieve JSONB metadata")
    void shouldPersistJsonbMetadata() {
        ContentEntity found = contentRepository.findById(banner1.getId()).orElseThrow();
        assertThat(found.getMetadata()).isNotNull();
        assertThat(found.getMetadata()).containsEntry("campaign", "TEST_2026");
    }

    @Test
    @DisplayName("Should handle null JSONB fields")
    void shouldHandleNullJsonbFields() {
        ContentEntity content = ContentEntity.builder()
                .contentType("POPUP")
                .title("Minimal ContentEntity")
                .description("No targeting or metadata")
                .actionType("DISMISS")
                .priority(0)
                .status(ContentStatus.DRAFT)
                .version(1)
                .build();
        ContentEntity saved = contentRepository.saveAndFlush(content);
        ContentEntity found = contentRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getTargetingRules()).isNull();
        assertThat(found.getMetadata()).isNull();
    }

    @Test
    @DisplayName("Should auto-populate timestamps on save")
    void shouldAutoPopulateTimestampsOnSave() {
        ContentEntity content = ContentEntity.builder()
                .contentType("POPUP")
                .title("Timestamp Test")
                .actionType("DISMISS")
                .priority(0)
                .status(ContentStatus.DRAFT)
                .version(1)
                .createdBy("test")
                .updatedBy("test")
                .build();
        ContentEntity saved = contentRepository.saveAndFlush(content);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
