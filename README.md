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

La conexión a la base de datos está en `webapp/src/main/java/ar/edu/itba/paw/webapp/config/WebConfig.java` (URL, usuario y contraseña deben coincidir con tu Postgres local).

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

Variables opcionales: `PAW_RUN_SKIP_TESTS=1`, `PAW_RUN_WAIT_SECS`, `PAW_JETTY_URL`, `PAW_JETTY_PORT` (ver comentarios en `run.sh`).

App: **http://localhost:8080**

## pre-commit

```bash
sudo apt install pre-commit
pre-commit install
```

Config: `.pre-commit-config.yaml`.

## Clases teóricas

Notas de teoría y práctica del curso en `logs.md`.
