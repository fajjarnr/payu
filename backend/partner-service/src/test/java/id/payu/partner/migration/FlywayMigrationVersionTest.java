package id.payu.partner.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationVersionTest {

    private static final Pattern MIGRATION = Pattern.compile("^V(\\d+)__.*\\.sql$");

    @Test
    void migrationVersionsAreUnique() throws IOException, URISyntaxException {
        Path migrationDirectory = Path.of(
                getClass().getClassLoader().getResource("db/migration").toURI());

        Map<String, List<String>> migrationsByVersion;
        try (var files = Files.list(migrationDirectory)) {
            migrationsByVersion = files
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .map(MIGRATION::matcher)
                    .filter(Matcher::matches)
                    .collect(Collectors.groupingBy(
                            matcher -> matcher.group(1),
                            Collectors.mapping(Matcher::toString, Collectors.toList())));
        }

        Map<String, List<String>> duplicates = migrationsByVersion.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        assertThat(duplicates)
                .as("Flyway migration versions must be unique")
                .isEmpty();
    }
}
