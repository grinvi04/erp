# ── 빌드 스테이지 ─────────────────────────────────────────────────────
FROM amazoncorretto:26-al2023-jdk@sha256:2f180ed0eeb1258ce4c96edda216b7d47a36d1a4d7bc19d06fa1db0646c3d7f0 AS build
WORKDIR /app
RUN dnf install -y findutils && dnf clean all

COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon -q

COPY src ./src
RUN ./gradlew bootJar --no-daemon -q

# ── 런타임 스테이지 ───────────────────────────────────────────────────
FROM amazoncorretto:26-al2023-headless@sha256:23a497be708eefbaa004f82fc163e059f68b9cae96fdfde42aac68aaeade1b0e AS run
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
USER 10001:10001
ENTRYPOINT ["java", "-jar", "app.jar"]
