# Etapa 1: compilar con Maven
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
RUN mvn -B -q package -DskipTests

# Etapa 2: imagen final, solo con el JRE y el .jar
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --create-home app && mkdir data && chown app data
COPY --from=build /app/target/gestor-tareas-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
