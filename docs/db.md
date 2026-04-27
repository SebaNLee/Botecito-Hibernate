## Postgres local

Se detallan abajo los comandos para el manejo de la DB local con Bash:

### Comandos DB local:

Para conectarse:

```
psql -h localhost -U pawdbuser -d paw -W
```

Para borrar y recrear DB:

```
sudo -u postgres psql -c "DROP DATABASE paw;"
sudo -u postgres psql -c "CREATE DATABASE paw;"
```

## Postgres Pampero

Se detallan abajo los comandos para ingresar manualmente a la DB de producción:

### Comandos:

```
$ ssh <username>@pampero.itba.edu.ar
[<username>@pampero ~]$ psql -h 10.16.1.110 -U paw-2026a-11 -d paw-2026a-11 -W
```

## Backups de DBs

> **Nota:** Se recomienda el uso del script `db.sh` para el manejo de backups de deploy, se detallan abajo los comandos indviduales para debugging. 

El script `db.sh` crea y trae una copia .sql de la DB actual de deploy y tambien permite restaurar un .sql local sobre deploy.

### Comandos:

#### Backup

Usar preferiblemente:

```
$ ./db.sh backup <username>
```

De última, estos serían los comandos de a uno para ejecutarlos individualmente:

```
$ ssh <username>@pampero.itba.edu.ar
[<username>@pampero ~]$ pg_dump -h 10.16.1.110 -U paw-2026a-11 -d paw-2026a-11 -f backup.sql
$ scp <username>@pampero.itba.edu.ar:/home/<username>/backup.sql backups/backup.sql
```

#### Restore

Usar preferiblemente:

```
$ ./db.sh restore <file> <username>
```

El restore en deploy usa estrategia `DROP SCHEMA public CASCADE; CREATE SCHEMA public;` y luego ejecuta el SQL subido.
No se hace backup automatico: si se quiere respaldo antes de restaurar, correr manualmente `./db.sh backup <username>`.

De última, estos serían los comandos de a uno para ejecutarlos individualmente:

```
$ scp backups/backup.sql <username>@pampero.itba.edu.ar:/home/<username>/backup.sql
$ ssh <username>@pampero.itba.edu.ar
$ sed -i '/^SET transaction_timeout = /d' /home/<username>/backup.sql
$ psql -h 10.16.1.110 -U paw-2026a-11 -d paw-2026a-11 -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
$ psql -h 10.16.1.110 -U paw-2026a-11 -d paw-2026a-11 -f backup.sql
```

### Otros comandos:

Si se quiere hacer backup de la DB local:

```
pg_dump -h localhost -U postgres -d paw -f backups/backup.sql
```

Si se quiere hacer restore en la DB local:

```
sudo -u postgres psql -c "DROP DATABASE paw;"
sudo -u postgres psql -c "CREATE DATABASE paw;"
psql -h localhost -U postgres -d paw -f backups/backup.sql
```

> **Nota:** Ignorar errores de roles inexistentes, los pisa con usuario postgres (que es lo que venimos usando localmente).

Si se quiere hacer backup/restore en formato .dump (en vez de .sql):

```
pg_dump -h <host> -U <username> -d <database> -Fc -f backup.dump
pg_restore -h <host> -U <username> -d <database> backup.dump
```

### Datos:

```
# Pampero
<host>: 10.16.1.110
<username>: paw-2026a-11
<database>: paw-2026a-11
```

```
# Local
<host>: localhost
<username>: postgres
<database>: paw
```
