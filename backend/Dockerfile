# ── 빌드 스테이지 ─────────────────────────────────────────────────────
FROM amazoncorretto:24-al2023-jdk@sha256:4392325b718f643e2350800f9b1cbfcb6701d4cee11dd55c57acc01051f2c5b7 AS build
WORKDIR /app
RUN dnf install -y findutils && dnf clean all

COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon -q

COPY src ./src
RUN ./gradlew bootJar --no-daemon -q

# ── 런타임 스테이지 ───────────────────────────────────────────────────
FROM amazoncorretto:24-al2023-headless@sha256:b701d42c766b6c52e89d4228e460beebc05ce8eeddc2196a7447da1e5b5c0ad1 AS run
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
USER 10001:10001
ENTRYPOINT ["java", "-jar", "app.jar"]
