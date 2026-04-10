### Deployment a Pampero 

> **Nota:** Recomendamos el uso del script `deploy.sh` para hacer deployments a Pampero en vez del uso de esta guía.

Esta es una guía comando por comando para subir y dejar corriendo el WAR del proyecto a Pampero.

Es necesario tener las credenciales necesarias de producción en el archivo `webapp/src/main/resources/config/credentials-production.properties` como se indica en `README.md`.

#### Comandos:

Usar preferiblemente:

```
$ ./deploy.sh
```

De última, estos serían los comandos de a uno para ejecutarlos individualmente:

```
$ scp webapp/target/webapp.war <username>@pampero.itba.edu.ar:/home/<username>/.
$ ssh <username>@pampero.itba.edu.ar
[<username>@pampero ~]$ sftp paw-2026a-11@10.16.1.110
sftp> put webapp.war web/app.war
```

Deploy: `http://pawserver.it.itba.edu.ar/paw-2026a-11/`

Para logs: `http://pawserver.it.itba.edu.ar/logs/catalina.err`

### Postgres Pampero

Se detallan abajo los comandos para ingresar manualmente a la DB de producción:

#### Comandos:
```
$ ssh <username>@pampero.itba.edu.ar
[<username>@pampero ~]$ psql -h 10.16.1.110 -U paw-2026a-11 -d paw-2026a-11 -W
```
