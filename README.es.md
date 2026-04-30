# Botecito

¡Bienvenido a Botecito, aplicación web para alquilar y hostear equipamiento náutico como kayaks, paddles y más! 

## Stack

Java 21 · Spring · JSP/JSTL · Tailwind · JDBC · PostgreSQL · Jetty · Flyway · JUnit · Mockito · HSQLDB · Lombok · Spotless · Spotbugs

## Devs

### Requisitos

```bash
sudo apt update
sudo apt install openjdk-21-jdk maven postgresql postgresql-contrib
```

### Credentials

Completar el archivo `credentials-production.properties` para credenciales de producción.

Se provee `credentials-production.properties.example` para referencia.

### PostgreSQL (local)

Database: `paw`

Username: `postgres`

Password: `postgres`

```bash
sudo service postgresql start
sudo -u postgres psql -c "CREATE DATABASE paw;"
```

### Ejecutar

Se provee el script `run.sh` para levantar el proyecto localmente:

```bash
./run.sh start | stop | restart | status | logs [jetty|tailwind|both]
```

O, alternativamente (en diferentes terminales):

```bash
mvn clean install
mvn install -pl webapp -am
```

```bash
mvn tailwind:watch
```

```bash
mvn jetty:run
```

> App: `http://localhost:8080`

### Deployear

```bash
./deploy.sh <username>
```

Requiere haber creado el archivo `credentials-production.properties` en `webapp/src/main/resources/config/`.

Despliega la web en: [http://pawserver.it.itba.edu.ar/paw-2026a-11](http://pawserver.it.itba.edu.ar/paw-2026a-11).

### pre-commit

```bash
sudo apt install pre-commit
pre-commit install
```

Config: `.pre-commit-config.yaml`.

### Docs

Para más detalles sobre el manejo de base de datos, contenido de clase o sobre deploys a Pampero, consultar: `docs/db.md`, `docs/class.md` y `docs/deploy.md`.