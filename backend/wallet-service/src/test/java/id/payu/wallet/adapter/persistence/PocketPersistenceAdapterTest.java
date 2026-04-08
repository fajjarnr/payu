package id.payu.wallet.adapter.persistence;

import id.payu.wallet.adapter.persistence.entity.PocketEntity;
import id.payu.wallet.adapter.persistence.repository.PocketJpaRepository;
import id.payu.wallet.domain.model.Pocket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(PocketPersistenceAdapter.class)
class PocketPersistenceAdapterTest {

    @Autowired
    private PocketPersistenceAdapter pocketPersistenceAdapter;

    @Autowired
    private PocketJpaRepository pocketJpaRepository;

    @Test
    @DisplayName("Should persist a pocket created by the application service")
    void shouldPersistPocketCreatedByService() {
        Pocket pocket = Pocket.builder()
                .accountId("ACC-POCKET-SAVE")
                .name("Travel Pocket")
                .description("Created through adapter.save")
                .currency("IDR")
                .balance(BigDecimal.ZERO)
                .status(Pocket.PocketStatus.ACTIVE)
                .build();

        Pocket saved = pocketPersistenceAdapter.save(pocket);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAccountId()).isEqualTo("ACC-POCKET-SAVE");
        assertThat(saved.getName()).isEqualTo("Travel Pocket");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(pocketJpaRepository.findById(saved.getId())).isPresent();
    }

    @Test
    @DisplayName("Should return all pockets for an account")
    void shouldReturnAllPocketsForAccount() {
        pocketJpaRepository.save(newPocketEntity("ACC-POCKET-LIST", "Pocket A"));
        pocketJpaRepository.save(newPocketEntity("ACC-POCKET-LIST", "Pocket B"));

        List<Pocket> pockets = pocketPersistenceAdapter.findByAccountId("ACC-POCKET-LIST");

        assertThat(pockets)
                .extracting(Pocket::getName)
            .containsExactlyInAnyOrder("Pocket A", "Pocket B");
    }

    private PocketEntity newPocketEntity(String accountId, String name) {
        PocketEntity entity = new PocketEntity();
        entity.setAccountId(accountId);
        entity.setName(name);
        entity.setDescription("Test pocket");
        entity.setCurrency("IDR");
        entity.setBalance(BigDecimal.ZERO);
        entity.setStatus(Pocket.PocketStatus.ACTIVE);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }
}