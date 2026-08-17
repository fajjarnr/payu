package id.payu.auth.adapter.persistence;

import id.payu.auth.adapter.persistence.entity.UserKnownDeviceEntity;
import id.payu.auth.adapter.persistence.entity.UserKnownIpEntity;
import id.payu.auth.adapter.persistence.entity.UserRiskProfileEntity;
import id.payu.auth.adapter.persistence.repository.UserRiskProfileRepository;
import id.payu.auth.domain.model.UserRiskProfile;
import id.payu.auth.domain.port.out.RiskProfileRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RiskProfileRepositoryAdapter implements RiskProfileRepositoryPort {

    private final UserRiskProfileRepository repository;

    @Override
    public Optional<UserRiskProfile> findByUsername(String username) {
        return repository.findById(username).map(this::toDomain);
    }

    @Override
    public UserRiskProfile save(UserRiskProfile domain) {
        UserRiskProfileEntity entity = repository.findById(domain.getUsername())
                .orElseGet(() -> {
                    UserRiskProfileEntity newEntity = new UserRiskProfileEntity();
                    newEntity.setUsername(domain.getUsername());
                    return newEntity;
                });

        entity.setFailedAttempts(domain.getFailedAttempts());

        if (domain.getKnownDevices() != null) {
            for (String deviceId : domain.getKnownDevices()) {
                boolean exists = entity.getKnownDevices().stream()
                        .anyMatch(d -> deviceId.equals(d.getDeviceId()));
                if (!exists) {
                    entity.addKnownDevice(deviceId);
                }
            }
        }

        if (domain.getKnownIps() != null) {
            for (String ip : domain.getKnownIps()) {
                boolean exists = entity.getKnownIps().stream()
                        .anyMatch(i -> ip.equals(i.getIpAddress()));
                if (!exists) {
                    entity.addKnownIp(ip);
                }
            }
        }

        UserRiskProfileEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    private UserRiskProfile toDomain(UserRiskProfileEntity entity) {
        if (entity == null) return null;
        Set<String> devices = entity.getKnownDevices() != null
                ? entity.getKnownDevices().stream().map(UserKnownDeviceEntity::getDeviceId).collect(Collectors.toSet())
                : Set.of();
        Set<String> ips = entity.getKnownIps() != null
                ? entity.getKnownIps().stream().map(UserKnownIpEntity::getIpAddress).collect(Collectors.toSet())
                : Set.of();

        return UserRiskProfile.builder()
                .username(entity.getUsername())
                .failedAttempts(entity.getFailedAttempts())
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .knownDevices(new java.util.HashSet<>(devices))
                .knownIps(new java.util.HashSet<>(ips))
                .build();
    }
}
