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

Las credenciales y la configuración de cada entorno (local o producción) están en `webapp/src/main/resources/config/`:

- **`credentials-local.properties`** → Para desarrollo local. Está en el repo e incluye la configuración JDBC (`jdbc.*`). Si necesitás más claves (como mail), agregalas acá usando el mismo prefijo.
- **`credentials-production.properties`** → Para producción. No está en el repo. Tomá `credentials-production.properties.example`, renombralo y completalo con los datos reales del servidor.

¿Cuál archivo se usa?  
La app lo decide según el valor de `credentials.file`, que está en `config/credentials-selection.properties`. Este archivo se filtra automáticamente según el entorno al construir el proyecto con Maven:

- Builds locales (`mvn compile`, `mvn jetty:run`, etc.): usa `credentials-local.properties`
- Build para producción (`./deploy.sh`, perfil `production-war`): usa `credentials-production.properties`

En builds locales, también se define `credentials.fallback.file=credentials-production.properties`: si falta una clave en `credentials-local.properties`, y existe `credentials-production.properties`, la toma de ahí. Así podés mantener credenciales locales simples y dejar datos sensibles (ej: mail) sólo en producción.

La URL, usuario y contraseña de la base de datos se leen de las claves `jdbc.*` que quedan al final de esta selección automática de archivos.

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

**WAR para Pampero:** `./deploy.sh <username>` (requiere `credentials-production.properties` local y estar en branch `main`). Compila y despliega en `web/app.war`.

App: **http://localhost:8080**

### Deployear

```bash
./deploy.sh <username>
```

Requiere haber creado el archivo `credentials-production.properties` en `webapp/src/main/resources/config/`.

Despliega la web en: [http://pawserver.it.itba.edu.ar/paw-2026a-11](http://pawserver.it.itba.edu.ar/paw-2026a-11).

## pre-commit

```bash
sudo apt install pre-commit
pre-commit install
```

Config: `.pre-commit-config.yaml`.

## Clases teóricas

Notas de teoría y práctica del curso en `logs.md`.
