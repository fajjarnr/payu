package id.payu.mapper.config;

import org.mapstruct.MapperConfig;
import org.mapstruct.MappingInheritanceStrategy;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * Shared MapStruct configuration for PayU mappers.
 *
 * <p>This configuration provides sensible defaults for all mappers in the PayU platform:
 * <ul>
 *   <li>Spring component model for dependency injection</li>
 *   <li>Null-safe mapping with null checks</li>
 *   <li>Ignore unmapped target properties (to avoid breaking changes)</li>
 *   <li>Inherit inverse mappings automatically</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * @Mapper(config = MappingConfig.class)
 * public interface WalletMapper extends BaseMapper<WalletEntity, Wallet> {
 *     // Custom mappings
 * }
 * </pre>
 *
 * @see org.mapstruct.MapperConfig
 * @since IMP-069
 */
@MapperConfig(
        componentModel = "spring",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.WARN,
        unmappedSourcePolicy = ReportingPolicy.WARN,
        mappingInheritanceStrategy = MappingInheritanceStrategy.AUTO_INHERIT_FROM_CONFIG
)
public interface MappingConfig {
    // Shared configuration for all mappers
}
