# ── 빌드 스테이지 ─────────────────────────────────────────────────────
FROM amazoncorretto:27-al2023-jdk@sha256:0e4efd9e1cea5ff517e97e23c526a626fbf57ca272d12ff26c6b8e402832f9f3 AS build
WORKDIR /app
RUN dnf install -y findutils && dnf clean all

COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon -q

COPY src ./src
RUN ./gradlew bootJar --no-daemon -q

# ── 런타임 스테이지 ───────────────────────────────────────────────────
FROM amazoncorretto:27-al2023-headless@sha256:9c4bc2122b86ad16e7f7059e1bc388571acae8ed5ce9be4c764f74ba3bed2f5a AS run
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
USER 10001:10001
ENTRYPOINT ["java", "-jar", "app.jar"]
