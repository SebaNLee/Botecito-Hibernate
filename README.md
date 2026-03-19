# Botecito (.app)

TODO

## Para arrancar y observaciones:

Levantar Jetty:

```
root$ mvn install
root$ mvn compile
webapp$ mvn install
webapp$ mvn compile
webapp$ mvn tailwind:watch
webapp$ mvn jetty:run

```

Luego, probar con:

```
curl http://localhost:8080
curl -X 'POST' http://localhost:8080/\?email\=foo@bar.com
```

Aparte, usar git por cli con ssh para bitbucket, mucho más cómodo

Para que sus commits cuenten para el contribution graph de GitHub, commitear con el mail que usan en GitHub. Al terminar la materia, se hace un clone de este repo a GitHub y se obtendrían los commits. Si quieren chequear que estén commiteando bien pueden verlo con `git log`.

