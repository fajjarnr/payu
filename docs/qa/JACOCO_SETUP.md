# JaCoCo Code Coverage Configuration for PayU Platform

This document describes the JaCoCo (Java Code Coverage) configuration
for the PayU Digital Banking Platform to enforce quality metrics.

## Coverage Goals

| Metric Type | Minimum | Target | Critical Path |
|------------|---------|--------|---------------|
| **Line Coverage** | 80% | 85% | 95% |
| **Branch Coverage** | 70% | 75% | 90% |
| **Method Coverage** | 75% | 80% | 95% |
| **Class Coverage** | 80% | 85% | 95% |

## Maven Configuration

Add to each service's `pom.xml`:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <!-- Prepare agent for unit tests -->
        <execution>
            <id>prepare-agent</id>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>

        <!-- Generate report after tests -->
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>

        <!-- Enforce coverage thresholds -->
        <execution>
            <id>check</id>
            <phase>test</phase>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                            <limit>
                                <counter>BRANCH</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.70</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
                <!-- Fail build if coverage check fails -->
                <haltOnFailure>true</haltOnFailure>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Exclusions

The following should be excluded from coverage:

1. **Generated Code**: Lombok-generated getters/setters, constructors
2. **Configuration Classes**: POJOs with only getters/setters
3. **DTOs**: Data Transfer Objects
4. **Entities**: JPA entities (tested via integration tests)
5. **Exception Classes**: Simple exception classes

```xml
<configuration>
    <excludes>
        <!-- Lombok generated -->
        <exclude>**/config/**</exclude>
        <exclude>**/dto/**</exclude>
        <exclude>**/entity/**</exclude>
        <exclude>**/model/**</exclude>
    </excludes>
</configuration>
```

## Running Coverage

```bash
# Run tests with coverage
mvn clean test jacoco:report

# View report
open target/site/jacoco/index.html

# Aggregate coverage for all modules
mvn clean verify jacoco:aggregate
```

## CI/CD Integration

Add to `.github/workflows/test.yml`:

```yaml
- name: Run tests with coverage
  run: mvn clean test jacoco:report

- name: Upload coverage to Codecov
  uses: codecov/codecov-action@v3
  with:
    files: backend/**/target/site/jacoco/jacoco.xml
    flags: unittests
    name: codecov-umbrella
```
