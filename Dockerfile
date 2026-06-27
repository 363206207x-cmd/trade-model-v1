FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src

RUN chmod +x ./mvnw && ./mvnw -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy AS runtime

WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --home-dir /app app

COPY --from=build /workspace/target/*.jar /app/app.jar

ENV SERVER_PORT=8081
EXPOSE 8081

USER app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
