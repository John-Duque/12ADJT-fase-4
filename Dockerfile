# syntax=docker/dockerfile:1

# Imagem de uma das funções, escolhida por MODULE (pasta em functions/) e PORT. Exemplo:
#   docker build --build-arg MODULE=feedback-ingestion --build-arg PORT=8081 -t 12adjt-fase-4/feedback-ingestion .
# Testes do módulo dentro do container: adicione --target test.

# ---------- build: compila a função e o domain ----------
FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /build

# Primeiro só os pom.xml: a camada fica em cache enquanto as dependências não mudam
COPY pom.xml ./
COPY shared/feedback-domain/pom.xml shared/feedback-domain/
COPY functions/feedback-ingestion/pom.xml functions/feedback-ingestion/
COPY functions/critical-notifier/pom.xml functions/critical-notifier/
COPY functions/weekly-report/pom.xml functions/weekly-report/

COPY shared/feedback-domain/src shared/feedback-domain/src
# ${MODULE:?...} interrompe o build com a mensagem quando MODULE não é informado
COPY ["functions/${MODULE:?Informe a função com --build-arg MODULE=<pasta em functions/>}/src", "functions/${MODULE}/src"]

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -pl functions/${MODULE} -am package -DskipTests

# ---------- test: roda os testes do módulo (só com --target test) ----------
FROM build AS test
ARG MODULE
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -pl functions/${MODULE} -am verify

# ---------- runtime: só a JRE e o fast-jar do Quarkus ----------
FROM eclipse-temurin:21-jre-alpine AS runtime
ARG MODULE
ARG PORT=8080

RUN addgroup -S -g 185 app && adduser -S -u 185 -G app app
WORKDIR /deployments

# Fast-jar em 4 camadas: dependências mudam pouco, o código da aplicação muda sempre
COPY --from=build --chown=app:app /build/functions/${MODULE}/target/quarkus-app/lib/ ./lib/
COPY --from=build --chown=app:app /build/functions/${MODULE}/target/quarkus-app/*.jar ./
COPY --from=build --chown=app:app /build/functions/${MODULE}/target/quarkus-app/app/ ./app/
COPY --from=build --chown=app:app /build/functions/${MODULE}/target/quarkus-app/quarkus/ ./quarkus/

ENV QUARKUS_HTTP_PORT=${PORT} \
    TZ=America/Sao_Paulo \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

USER app
EXPOSE ${PORT}

HEALTHCHECK --interval=30s --timeout=3s --start-period=15s --retries=3 \
    CMD wget -qO- http://127.0.0.1:${QUARKUS_HTTP_PORT}/q/health/live || exit 1

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar /deployments/quarkus-run.jar"]
