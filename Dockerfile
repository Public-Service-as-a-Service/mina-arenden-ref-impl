# Steg 1: bygg med Gradle (wrapper i repot hämtar rätt Gradle-version).
# Testerna körs inte här utan i CI (.github/workflows/ci.yml), där bilden bara byggs efter godkända tester.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon --version
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# Steg 2: slimmad körtidsbild
FROM eclipse-temurin:25-jre
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
WORKDIR /app
# curl installeras uttryckligen för hälsokontrollen, så att den inte beror på vad basbilden råkar innehålla.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && useradd --system --uid 1001 app
COPY --from=build /src/build/libs/app.jar /app/app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
