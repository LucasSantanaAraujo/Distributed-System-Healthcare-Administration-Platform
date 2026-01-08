# Etapa 1: build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# (melhor para cache) copia primeiro o pom e resolve deps
COPY pom.xml .
RUN mvn -DskipTests dependency:go-offline

# agora copia o código e compila
COPY src ./src
RUN mvn -DskipTests clean package

# Etapa 2: run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# copia o jar gerado
COPY --from=build /app/target/*.jar /app/app.jar

# copia o javaagent (dentro do contexto!)
COPY otel/opentelemetry-javaagent.jar /otel/opentelemetry-javaagent.jar

EXPOSE 8080
ENTRYPOINT ["java","-javaagent:/otel/opentelemetry-javaagent.jar","-jar","/app/app.jar"]
