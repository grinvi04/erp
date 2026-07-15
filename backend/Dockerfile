# ── 빌드 스테이지 ─────────────────────────────────────────────────────
FROM amazoncorretto:21-al2023-jdk AS build
WORKDIR /app
RUN dnf install -y findutils && dnf clean all

COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon -q

COPY src ./src
RUN ./gradlew bootJar --no-daemon -q

# ── 런타임 스테이지 ───────────────────────────────────────────────────
FROM amazoncorretto:21-al2023-headless AS run
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
USER 10001:10001
ENTRYPOINT ["java", "-jar", "app.jar"]
