# ==============================================================================
# MULTI-STAGE DOCKERFILE — SPRING BOOT BACKEND (MINIMARKET POOL)
# ==============================================================================

# ETAPA 1: Construcción (Build) con Maven y JDK 17
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copiar pom.xml y descargar dependencias para aprovechar caché de capas Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fuente y compilar artefacto JAR sin ejecutar tests en el build
COPY src ./src
RUN mvn clean package -DskipTests

# ETAPA 2: Entorno de ejecución ligero (JRE 17 Alpine)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Crear usuario sin privilegios root por seguridad
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copiar archivo JAR empaquetado desde la etapa de compilación
COPY --from=build /app/target/*.jar app.jar

# Variables de entorno por defecto
ENV SERVER_PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod

# Exponer puerto del servicio
EXPOSE 8080

# Comprobación de estado mediante Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Comando de inicio del microservicio
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
