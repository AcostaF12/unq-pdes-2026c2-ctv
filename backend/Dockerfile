FROM eclipse-temurin:25.0.5_8-jdk AS build
WORKDIR /app

# Wrapper + build scripts first so dependencies live in their own cached layer.
# gradle.properties carries the repository retries for HTTP 429 from Maven Central.
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Download plugins and dependency jars (not only their metadata, as `dependencies`
# does) before copying the sources: this layer is only rebuilt when the build
# scripts or the version catalog change.
RUN ./gradlew resolveDependencies --no-daemon

# Compile without network access: a jar missing from the layer above fails here
# instead of being downloaded again on every build.
COPY src ./src
RUN --network=none ./gradlew bootJar --no-daemon --offline

FROM eclipse-temurin:25.0.5_8-jre AS runtime
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --no-create-home --shell /usr/sbin/nologin app
COPY --from=build /app/build/libs/*.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
