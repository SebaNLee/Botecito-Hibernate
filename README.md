<p align="center">
  English | <a href="README.es.md">Español</a>
</p>

# Botecito

Welcome to Botecito, a web application for renting and hosting nautical equipment such as kayaks, paddle boards, and more!

## Stack

Java 21 · Spring · JSP/JSTL · Tailwind · JDBC · PostgreSQL · Jetty · Flyway · JUnit · Mockito · HSQLDB · Lombok · Spotless · Spotbugs

## Devs

### Requirements

```bash
sudo apt update
sudo apt install openjdk-21-jdk maven postgresql postgresql-contrib
```

### Credentials

Fill in the `credentials-production.properties` file with production credentials.

A reference file `credentials-production.properties.example` is provided.

### PostgreSQL (local)

Database: `paw`

Username: `postgres`

Password: `postgres`

```bash
sudo service postgresql start
sudo -u postgres psql -c "CREATE DATABASE paw;"
```

### Running the app

A `run.sh` script is provided to run the project locally:

```bash
./run.sh start | stop | restart | status | logs [jetty|tailwind|both]
```

Or you could also (in separate terminals):

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

### Deployment

```bash
./deploy.sh <username>
```

Requires the `credentials-production.properties` file to be created under `webapp/src/main/resources/config/`.

The application will be deployed at: [http://pawserver.it.itba.edu.ar/paw-2026a-11](http://pawserver.it.itba.edu.ar/paw-2026a-11).

### pre-commit

```bash
sudo apt install pre-commit
pre-commit install
```

Config file: `.pre-commit-config.yaml`.

### Docs

For more details about database management, class content, or deployments to Pampero, see:
`docs/db.md`, `docs/class.md`, and `docs/deploy.md`.