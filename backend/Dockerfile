# ── 빌드 스테이지 ─────────────────────────────────────────────────────
FROM amazoncorretto:21-al2023-jdk@sha256:eaa4fce2c7729b2961a9ced07550b634e531e595a88784df2ebb7604581c2ca5 AS build
WORKDIR /app
RUN dnf install -y findutils && dnf clean all

COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon -q

COPY src ./src
RUN ./gradlew bootJar --no-daemon -q

# ── 런타임 스테이지 ───────────────────────────────────────────────────
FROM amazoncorretto:21-al2023-headless@sha256:9dc479b7cedfe1465f1a38189aaa549c829cd9a013f4fc60dc535491001189e4 AS run
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
USER 10001:10001
ENTRYPOINT ["java", "-jar", "app.jar"]
