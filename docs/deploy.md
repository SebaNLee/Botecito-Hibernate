### Deployment a Pampero

#### Dependencias

- javax.servlet, jstl, 1.2 (root y webapp, solo para )

#### Credenciales de producción

1. Copiar `webapp/src/main/resources/config/credentials-production.properties.example` a `credentials-production.properties` (archivo gitignored) y agregarle las contraseñas
2. Completar al menos `jdbc.password`, `mail.password`(y ver si hay otras credenciales a agregar)
3. En la raíz del repo: `./deploy.sh`  compila el WAR con perfil `production-war` (marcador `META-INF/paw-credentials-profile` = `production` y empaqueta `credentials-production.properties`). Esto usando el flag `-Pproduction-war`

#### Comandos deploy en Pampero

##### Deploy con script
```
$ ./deploy.sh <username>
```

##### Deploy manual

```
$ ./deploy.sh
$ scp webapp/target/webapp.war <username>@pampero.itba.edu.ar:/home/<username>/.
$ ssh <username>@pampero.itba.edu.ar
[<username>@pampero ~]$ sftp paw-2026a-11@10.16.1.110
sftp> put webapp.war web/app.war
```

Deploy: `http://pawserver.it.itba.edu.ar/paw-2026a-11/`

Para logs: `http://pawserver.it.itba.edu.ar/logs/catalina.err`

### Postgres Pampero

##### Ingreso manual

```
$ ssh <username>@pampero.itba.edu.ar
[<username>@pampero ~]$ psql -h 10.16.1.110 -U paw-2026a-11 -d paw-2026a-11 -W
```
