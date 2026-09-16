# Steg 1: bygg med Gradle (wrapper i repot hämtar rätt Gradle-version)
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon --version
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# Steg 2: slimmad körtidsbild
FROM eclipse-temurin:21-jre
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
WORKDIR /app
RUN useradd --system --uid 1001 app
COPY --from=build /src/build/libs/app.jar /app/app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
