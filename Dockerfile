FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw \
    && ./mvnw \
        --batch-mode \
        --no-transfer-progress \
        dependency:go-offline

COPY src/ src/

RUN ./mvnw \
    --batch-mode \
    --no-transfer-progress \
    -DskipTests \
    package


FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install \
        --yes \
        --no-install-recommends \
        curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system escala24 \
    && useradd \
        --system \
        --gid escala24 \
        --no-create-home \
        escala24

WORKDIR /app

COPY --from=build \
    --chown=escala24:escala24 \
    /workspace/target/*.jar \
    app.jar

USER escala24

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]