# Botecito (.app)

TODO

## Stack:

Java | JSP | JSTL | Tailwind | Spring | JDBC | PostgreSQL

Jetty | (TODO Tomcat) | JUnit | Lombok | Spotless | Spotbugs

## Devs:

pre-commit setup:
```
pip install pre-commit
pre-commit install
```

Levantar Jetty:

```
root$ mvn install
root$ mvn compile
webapp$ mvn install
webapp$ mvn compile
webapp$ mvn tailwind:watch
webapp$ mvn jetty:run

```

O desde el root con:

```bash
./jetty start
./jetty stop
```

Luego, probar con:

```
Clase 2:
curl http://localhost:8080/class
curl -X 'POST' http://localhost:8080/class/\?email\=foo@bar.com

Nota: el root de las rutas utilizadas en clases teóricas se cambió de / a /class (desde Clase 3)

Clase 3:
curl -d "email=test@paw.itba.edu.ar" -d "password=secret" -d "username=PAW" http://localhost:8080/class
curl http://localhost:8080/class/profile/2342342

PostgreSQL setup (ver clase 01:30:00 - 01:45:00):

    Ubuntu:
        check: pg_isready
        si no responde: sudo pg_createcluster 14 main --start

        sudo -u postgres psql -c "ALTER USER postgres WITH PASSWORD 'postgres';"
        sudo -u postgres psql -c "CREATE DATABASE paw;"
        sudo -u postgres psql postgres

        CREATE ROLE pawdbuser WITH LOGIN PASSWORD 'pawdbsecret';
        GRANT ALL PRIVILEGES ON DATABASE paw TO pawdbuser;
        ALTER DATABASE paw OWNER TO pawdbuser;
        ALTER TABLE public.users OWNER TO pawdbuser;

        psql -h localhost -U pawdbuser -d paw -W

Luego de setear PostgreSQL:
curl -d "email=test@paw.itba.edu.ar" -d "password=secret" -d "username=PAW" http://localhost:8080/class
curl http://localhost:8080/class/profile/1

```

Otras notas:

Usar git por cli con ssh para bitbucket, mucho más cómodo

Para que sus commits cuenten para el contribution graph de GitHub, commitear con el mail que usan en GitHub. Al terminar la materia, se hace un clone de este repo a GitHub y se obtendrían los commits. Si quieren chequear que estén commiteando bien pueden verlo con `git log`.
