<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html lang="es">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Botecito</title>
    <link rel="stylesheet" href="<c:url value='/css/tailwind.css' />" />
  </head>
  <body>
    <paw:heading level="1" text="Botecito" />

    <p>TODO landing</p>

    <nav aria-label="Navegacion principal">
      <a class="underline" href="<c:url value='/marketplace' />">Marketplace</a>
    </nav>

    <br/>
    <br/>
    <br/>

    <paw:heading level="2" text="TP0 - Librería de custom tags" />

    <paw:heading level="1" text="Heading nivel 1" />
    <paw:heading level="2" text="Heading nivel 2" />
    <paw:heading level="3" text="Heading nivel 3" />
    <paw:heading level="4" text="Heading nivel 4" />
    <paw:heading level="5" text="Heading nivel 5" />
    <paw:heading level="6" text="Heading nivel 6" />

    <div class="flex flex-row mb-2 gap-1">
      <paw:button text="Principal" color="primary" />
      <paw:button text="Secundario" color="secondary" />
      <paw:button text="Éxito" color="success" />
      <paw:button text="Peligro" color="danger" />
      <paw:button text="Advertencia" color="warning" />
      <paw:button text="Contorno" color="outline" />
    </div>

    <div class="flex flex-row mb-2 gap-1">
      <paw:button text="Chico" color="primary" size="sm" />
      <paw:button text="Mediano" color="primary" size="md" />
      <paw:button text="Grande" color="primary" size="lg" />
    </div>

    <div class="mb-2">
      <paw:button text="Deshabilitado" color="primary" disabled="${true}" />
    </div>

    <div class="flex flex-row mb-2 gap-1">
      <paw:card name="Producto A" price="1500" date="17/03/2026" />
      <paw:card name="Producto B" price="2300,50" date="01/04/2026" />
      <paw:card name="Producto C" price="890" date="15/05/2026" />
    </div>

    <div>
      <h1 class="text-3xl font-bold underline text-violet-500">Hola</h1>
    </div>
    
  </body>
</html>
