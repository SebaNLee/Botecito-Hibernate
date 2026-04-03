### Deployment a Pampero

#### Dependencias

- javax.servlet, jstl, 1.2 (root y webapp)

#### Credenciales JDBC de producción

1. Copiar `webapp/src/main/resources/config/jdbc-production.properties.example` → `jdbc-production.properties` (archivo gitignored).
2. Completar `jdbc.url`, `jdbc.username`, `jdbc.password` del servidor de grupo.
3. En la raíz del repo: `./deploy.sh` — compila el WAR con perfil `production-war` (marcador `META-INF/paw-jdbc-profile` = `production` y empaqueta `jdbc-production.properties`).

#### Comandos deploy en Pampero

```
$ ./deploy.sh
$ scp webapp/target/webapp.war <username>@pampero.itba.edu.ar:/home/<username>/.
$ ssh <username>@pampero.itba.edu.ar
[<username>@pampero ~]$ sftp paw-2026a-11@10.16.1.110
sftp> put webapp.war web/app.war
```

Y con eso estaría, acceder a: `http://pawserver.it.itba.edu.ar/paw-2026a-11/`

Para logs: `http://pawserver.it.itba.edu.ar/logs/catalina.err`
