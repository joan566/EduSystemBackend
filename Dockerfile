# Imagen de producción de EduSistem (Render u otro host con Docker).
#
#   docker build -t edusistem-backend .
#   docker run -p 8080:8080 -e DB_URL=... -e DB_USERNAME=... -e DB_PASSWORD=... -e JWT_SECRET=... \
#       -e STORAGE_PATH=/var/data/storage -e STORAGE_ENCRYPTION_KEY=... -v edusistem-data:/var/data edusistem-backend
#
# La configuración llega por variables de entorno (ver README); la imagen no contiene .env ni secretos.

# --- Build ---------------------------------------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Dependencias en una capa propia: solo se vuelven a descargar si cambia el pom.xml
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Los tests no se ejecutan aquí: el PostgreSQL embebido que usan no arranca como root. Se ejecutan antes (mvn test).
RUN mvn -B -q clean package -DskipTests

# --- Runtime -------------------------------------------------------------------------------------------------------
# JRE sobre Ubuntu (no alpine): la app no necesita binarios externos, solo Java 21.
FROM eclipse-temurin:21-jre

RUN groupadd --system app && useradd --system --gid app --home-dir /app --no-create-home app
WORKDIR /app
COPY --from=build /build/target/*.jar /app/app.jar

# Heap al 75 % de la RAM del contenedor (por defecto la JVM usa solo el 25 %) y reinicio limpio ante un OOM.
# Se puede sobrescribir con la variable JAVA_TOOL_OPTIONS del servicio.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

# Puerto: SERVER_PORT si está definido; si no, PORT (lo inyecta Render); si no, 8080.
# Arranca como root solo para crear STORAGE_PATH y dárselo al usuario app (un disco montado suele pertenecer a root);
# la JVM se ejecuta después sin privilegios mediante setpriv.
ENTRYPOINT ["/bin/sh", "-c", "\
export SERVER_PORT=\"${SERVER_PORT:-${PORT:-8080}}\"; \
if [ \"$(id -u)\" = 0 ]; then \
  if [ -n \"$STORAGE_PATH\" ]; then mkdir -p \"$STORAGE_PATH\" && chown -R app:app \"$STORAGE_PATH\" || exit 1; fi; \
  exec setpriv --reuid=app --regid=app --init-groups java -jar /app/app.jar; \
fi; \
exec java -jar /app/app.jar"]
