package id.payu.partner.adapter.persistence.repository;

import id.payu.partner.adapter.persistence.entity.PartnerEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartnerRepository extends JpaRepository<PartnerEntity, Long> {
    
    Optional<PartnerEntity> findByEmail(String email);

    Optional<PartnerEntity> findByClientId(String clientId);
    Optional<PartnerEntity> findByPartnerCode(String partnerCode);

    List<PartnerEntity> findByStatus(id.payu.partner.domain.PartnerStatus status);

    /**
     * PARTNER-PROD-002: lock a batch of partners whose client_secret or api_key
     * is still legacy plaintext (not yet ENC(...) ciphertext) so the scheduled
     * backfill can re-encrypt them. JPQL (not native) so the converter runs on
     * read and the values held by the entities are the plaintext to re-encrypt.
     */
    @Query(value = """
            SELECT p FROM PartnerEntity p
            WHERE (p.clientSecret IS NOT NULL AND p.clientSecret <> '' AND p.clientSecret NOT LIKE 'ENC%')
               OR (p.apiKey IS NOT NULL AND p.apiKey <> '' AND p.apiKey NOT LIKE 'ENC%')
            ORDER BY p.id
            """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<PartnerEntity> lockNextPlaintextCredentialBatch(Pageable pageable);

    /**
     * PARTNER-PROD-002: force-rewrite legacy plaintext credentials through the
     * converter. A bulk JPQL update always executes SQL and applies the
     * {@code EncryptedStringConverter} when binding the plaintext parameters, so
     * the column is rewritten to ENC(...) even though the in-memory value is
     * unchanged (a normal entity save would be skipped by Hibernate's dirty check).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PartnerEntity p SET p.clientSecret = :clientSecret, p.apiKey = :apiKey WHERE p.id = :id")
    int rewriteEncryptedCredentials(@Param("id") Long id,
                                    @Param("clientSecret") String clientSecret,
                                    @Param("apiKey") String apiKey);
}
