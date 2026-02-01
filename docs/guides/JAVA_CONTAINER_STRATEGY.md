# 🐳 Java Containerization Strategy: The "Decoupled Build" Pattern

> **Architectural Decision Record (ADR-Container-01)**
> **Status**: Adopted for all Backend Services
> **Effective Date**: February 2026

---

## 🧐 Executive Summary

We have shifted our containerization strategy for Java services from **Multi-Stage Dockerfile Builds** to a **Decoupled Build Pattern** (Pre-built JAR).

**Why?**
Building Java applications (`mvn package`) inside a container is extremely resource-intensive and cache-unfriendly. By moving the compilation step *outside* the container build (to the Host or CI Runner), we achieve **10x faster builds** and cleaner caching strategies.

---

## 🆚 Comparison: The Two Approaches

### Way 1: Multi-Stage Build (The "Self-Contained" Way)
*Traditional method where the Dockerfile handles everything.*

```dockerfile
# Stage 1: Build (Heavy!)
FROM maven:3.9-eclipse-temurin-21 AS builder
COPY . .
RUN mvn package  # <--- Downloads internet, no cache, slow!

# Stage 2: Run
FROM openjdk:21-slim
COPY --from=builder /app/target/*.jar app.jar
CMD ["java", "-jar", "app.jar"]
```

| Pros | Cons |
| :--- | :--- |
| Zero prerequisites on host (just Docker) | **Extremely Slow**: Re-downloads dependencies on every build unless complex caching is setup. |
| Consistent build environment | **Resource Heavy**: CI runner needs massive RAM to run Maven inside Docker. |
| | **Cache Hell**: Difficult to leverage CI shared volumes modules (`~/.m2`). |

### Way 2: Decoupled Build (The "PayU Standard" Way)
*Our chosen method: Build Artifact first, then Package Image.*

```dockerfile
# Runtime Stage Only
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24-2

# Assumption: You already ran 'mvn package' on the host/runner
COPY target/*.jar /deployments/app.jar

CMD ["java", "-jar", "/deployments/app.jar"]
```

| Pros | Cons |
| :--- | :--- |
| **Blazing Fast**: Image build takes seconds, not minutes. | Requires 2 steps in pipeline (Build -> Docker Build). |
| **Efficient Caching**: CI runners can use shared `~/.m2` Cache volumes naturally. | Requires Maven installed on Host/Runner. |
| **Smaller Attack Surface**: No build tools (Maven/GCC) ever touch the image layers. | |

---

## 🛠️ Implementation Guide

### 1. The Standard Containerfile
Ensure your `Containerfile` or `Dockerfile` looks like this (optimized for Red Hat UBI9):

```dockerfile
####
# Runtime stage - Using minimal UBI9 OpenJDK 21 runtime
# Note: Expects pre-built JAR from host (mvn package must be run first)
####
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24-2

# ... Metadata Labels ...

USER 185
WORKDIR /deployments

# 🚀 THE MAGIC LINE: Copy pre-built artifact
COPY --chown=185 target/*.jar /deployments/app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -jar /deployments/app.jar"]
```

### 2. Local Development Workflow
When running on your machine:

```bash
# Step 1: Compile locally (Fast, uses your local .m2 cache)
mvn package -DskipTests

# Step 2: Package Container (Instant)
podman build -t my-service:latest .
```

---

## 🔄 CI/CD Pipeline Examples

How to implement this in modern CI/CD tools.

### 🐙 GitHub Actions

Separate the jobs or steps. Use `upload-artifact` to pass the JAR to the build step.

```yaml
jobs:
  build-java:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          cache: 'maven' # Automatic caching!
      
      # Step 1: Build JAR
      - run: mvn clean package -DskipTests
      
      # Persist JAR for next job
      - uses: actions/upload-artifact@v4
        with:
          name: app-jar
          path: target/*.jar

  build-container:
    needs: build-java
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/download-artifact@v4
        with:
          name: app-jar
          path: target/
          
      # Step 2: Build Image (JAR is already in target/)
      - run: docker build -t my-app .
      - run: docker push my-app
```

### ⚓ Tekton / OpenShift Pipelines

In Tekton, we use a shared **Workspace** (PVC) to pass files between Tasks.

```yaml
apiVersion: tekton.dev/v1beta1
kind: Pipeline
spec:
  workspaces:
    - name: source-dir # Shared volume
  tasks:
    # Task 1: Maven Build (Writes to source-dir/target)
    - name: maven-build
      taskRef:
        name: maven
      workspaces:
        - name: source
          workspace: source-dir
      params:
        - name: GOALS
          value: ["package", "-DskipTests"]

    # Task 2: Buildah/Podman (Reads from source-dir/target)
    - name: build-image
      runAfter: [maven-build]
      taskRef:
        name: buildah
      workspaces:
        - name: source
          workspace: source-dir
      params:
        - name: DOCKERFILE
          value: ./Containerfile
        # Buildah sees the "target/" folder created by previous task
```

### 🦊 GitLab CI

Use `artifacts` to pass the JAR.

```yaml
stages:
  - build
  - package

maven-build:
  stage: build
  image: maven:3.9-eclipse-temurin-21
  script:
    - mvn package -DskipTests
  cache:
    paths:
      - .m2/repository
  artifacts:
    paths:
      - target/*.jar

docker-build:
  stage: package
  image: docker:latest
  script:
    # target/*.jar is automatically restored by GitLab here
    - docker build -t my-app .
```
