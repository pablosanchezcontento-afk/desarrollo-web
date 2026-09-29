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
ENV SPRING_PROFILES_ACTIVE=prod
VOLUME /app/data
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
  CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/8080 && printf 'GET /actuator/health HTTP/1.0\\r\\n\\r\\n' >&3 && grep -q UP <&3"]
ENTRYPOINT ["java", "-jar", "app.jar"]
