### Implementaciones de clases teóricas:

#### Clase 9/3/2026:

```
curl http://localhost:8080/class
curl -X 'POST' http://localhost:8080/class/\?email\=foo@bar.com
```

Nota: el root de las rutas utilizadas en clases teóricas se cambió de / a /class (desde Clase 3)

#### Clase 23/3/2026:

PostgreSQL setup: ver grabación 01:30:00 - 01:45:00

##### Ubuntu:

Instalar postgre:

```
sudo apt install postgresql postgresql-contrib
```

Crear DB a usar y cambiar owner:

```
sudo -u postgres psql -c "ALTER USER postgres WITH PASSWORD 'postgres';"
sudo -u postgres psql -c "CREATE DATABASE paw;"
```

```
sudo -u postgres psql postgres
CREATE ROLE pawdbuser WITH LOGIN PASSWORD 'pawdbsecret';
GRANT ALL PRIVILEGES ON DATABASE paw TO pawdbuser;
ALTER DATABASE paw OWNER TO pawdbuser;
ALTER TABLE public.users OWNER TO pawdbuser;
```

Uso de Flyway:

Configurado para que, al levantar Jetty, Flyway aplique automaticamente las migraciones SQL (```V1__```, ```V2__```, etc.) y los ejecute en Postgres.

Para chequear que está corriendo Posgtres localmente:

```
pg_isready
sudo service postgresql status
```

Para iniciar/frenar Posgres:
```
sudo service postgresql start
sudo service postgresql stop
```

Para conectarse manualmente:

```
psql -h localhost -U pawdbuser -d paw -W
```

Test luego de setear Postgres:

```
curl -d "email=test@paw.itba.edu.ar" -d "password=secret" -d "username=PAW" http://localhost:8080/class
curl http://localhost:8080/class/profile/1
```