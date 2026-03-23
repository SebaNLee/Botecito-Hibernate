<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html lang="es">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Botecito - Marketplace - Item</title>
    <link rel="stylesheet" href="<c:url value='/css/tailwind.css' />" />
  </head>
  <body>
    <main>
    <paw:heading level="1" text="Botecito" />

          <p>TODO marketplace - itemId: ${itemId}</p>

      <nav>
        <a class="underline" href="<c:url value='/' />">Landing</a>
        <a class="underline" href="<c:url value='/marketplace' />">Marketplace</a>
      </nav>
      
    </main>
  </body>
</html>
