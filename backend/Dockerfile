FROM eclipse-temurin:21-jdk-alpine AS build

RUN apk add --no-cache bash curl tar

WORKDIR /workspace

# Copy build metadata first so Docker can reuse the dependency layer when only
# application source files change.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw \
    && ./mvnw --batch-mode -DskipTests dependency:go-offline

COPY src/ src/
RUN ./mvnw --batch-mode -DskipTests package \
    && JAR_FILE="$(find target -maxdepth 1 -type f -name '*.jar' ! -name '*.original' -print -quit)" \
    && test -n "$JAR_FILE" \
    && cp "$JAR_FILE" /workspace/csports.jar

FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl tzdata \
    && addgroup --system csports \
    && adduser --system --ingroup csports --home /app csports

ENV TZ=Africa/Cairo
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=Africa/Cairo -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

WORKDIR /app

COPY --from=build --chown=csports:csports /workspace/csports.jar /app/csports.jar

USER csports

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5 \
    CMD curl --fail --silent http://localhost:8080/actuator/health/readiness > /dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/csports.jar"]
