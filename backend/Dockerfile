FROM eclipse-temurin:25.0.5_8-jdk AS build
WORKDIR /app

# Wrapper + build scripts first so dependencies live in their own cached layer.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Resolve plugins and dependencies before copying the sources: this layer is
# only rebuilt when the build scripts or the version catalog change.
RUN ./gradlew dependencies --no-daemon > /dev/null

COPY src ./src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:25.0.5_8-jre AS runtime
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --no-create-home --shell /usr/sbin/nologin app
COPY --from=build /app/build/libs/*.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
