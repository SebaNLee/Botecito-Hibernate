### Deployment a Pampero

#### Dependencias

- javax.servlet, jstl, 1.2 (root y webapp)

#### Comandos deploy en Pampero

```
$ mvn clean package
$ scp webapp/target/webapp.war <username>@pampero.itba.edu.ar:/home/<username>/.
$ ssh slee@pampero.itba.edu.ar
[<username>@pampero ~]$ sftp paw-2026a-11@10.16.1.110
sftp> put webapp.war web/app.war
```

Y con eso estaría, acceder a: `http://pawserver.it.itba.edu.ar/paw-2026a-11/`

Para logs: `http://pawserver.it.itba.edu.ar/logs/catalina.err`
