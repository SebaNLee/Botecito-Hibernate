# Botecito (.app)

TODO

## Stack:

Java | JSP | JSTL | Tailwind | Spring | (TODO db)

Jetty | (TODO Tomcat) | JUnit | Lombok | Spotless

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

Clase 3:
curl -d "email=test@paw.itba.edu.ar" -d "password=secret" -d "username=PAW" http://localhost:8080/class
curl http://localhost:8080/class/profile/2342342


Nota: el root de las rutas utilizadas en clases teóricas se cambió de / a /class
```

Otras notas:

Usar git por cli con ssh para bitbucket, mucho más cómodo

Para que sus commits cuenten para el contribution graph de GitHub, commitear con el mail que usan en GitHub. Al terminar la materia, se hace un clone de este repo a GitHub y se obtendrían los commits. Si quieren chequear que estén commiteando bien pueden verlo con `git log`.
