# Botecito (.app)

TODO

## Stack

Java 21 · Spring · JSP/JSTL · Tailwind · JDBC · PostgreSQL · Jetty · Flyway · JUnit · Lombok · Spotless · Spotbugs

## Devs

### Requisitos (Ubuntu)

```bash
sudo apt update
sudo apt install openjdk-21-jdk maven postgresql postgresql-contrib
```

`java -version` debe ser 21.x.

### Overview

Aplicación web Spring MVC: vistas JSP, acceso a datos con JDBC contra PostgreSQL. Al arrancar Jetty, Flyway aplica migraciones en `webapp/src/main/resources/db/migration/`.

La conexión JDBC se define en archivos properties bajo `webapp/src/main/resources/config/`:

- **`jdbc-local.properties`**: desarrollo local (usuario `postgres`, base `paw`).
- **`jdbc-production.properties`**: no está en el repo; copiá `jdbc-production.properties.example` a ese nombre y completá credenciales del servidor (usado al empaquetar el WAR de producción).

El perfil activo es `local` o `production`: `./run.sh start` fuerza `PAW_JDBC_PROFILE=local`. Un WAR construido con `./deploy.sh` (perfil Maven `production-war`) incluye el marcador `production` y usa `jdbc-production.properties` dentro del WAR. URL, usuario y contraseña salen solo de esos archivos properties.

### PostgreSQL

Con la configuración por defecto del código: base de datos `paw`, usuario `postgres`, contraseña `postgres`.

```bash
sudo service postgresql start
sudo -u postgres psql -c "ALTER USER postgres WITH PASSWORD 'postgres';"
sudo -u postgres psql -c "CREATE DATABASE paw;"
PGPASSWORD=postgres psql -h 127.0.0.1 -U postgres -d paw -c 'SELECT 1;'
```

Si la app no levanta el contexto Spring (p. ej. **503**), revisar `.run/jetty.log` luego de usar `run.sh`.

### Ejecutar

Desde la raíz del repo:

```bash
./run.sh start | stop | restart | status | logs [jetty|tailwind|both]
```

- **run.sh**: build (`mvn install -pl webapp -am`), Tailwind watch y servidor Jetty en segundo plano; logs en `.run/`.
- **Manual:** en la raíz `mvn install -pl webapp -am`; en `webapp/`, `mvn tailwind:watch` y en otra terminal `mvn jetty:run`.

Variables opcionales: `PAW_RUN_SKIP_TESTS=1`, `PAW_RUN_WAIT_SECS`, `PAW_JETTY_URL`, `PAW_JETTY_PORT`, `PAW_JDBC_PROFILE` (ver comentarios en `run.sh`).

**WAR para Pampero:** `./deploy.sh` (requiere `jdbc-production.properties` local). Genera `webapp/target/webapp.war`.

App: **http://localhost:8080**

## pre-commit

```bash
sudo apt install pre-commit
pre-commit install
```

Config: `.pre-commit-config.yaml`.

## Clases teóricas

Notas de teoría y práctica del curso en `logs.md`.
