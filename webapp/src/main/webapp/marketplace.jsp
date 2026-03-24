<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html lang="es">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Botecito - Marketplace</title>
    <link rel="stylesheet" href="<c:url value='/css/tailwind.css' />" />
  </head>
  <body>
    <paw:heading level="1" text="Botecito" />

    <p>TODO marketplace</p>

    <nav>
      <a class="underline" href="<c:url value='/' />">Landing</a>
    
      <form id="item-form">
        <label for="item-id">Item ID</label>
        <input class="border-2" id="item-id" name="item-id" type="text" required />
        <button class="underline cursor-pointer" type="submit">ItemId</button>
      </form>
    </nav>

    <!-- TODO hardcode JS for routing debbug -->
    <script>
      (function () {
        const form = document.getElementById("item-form");
        const input = document.getElementById("item-id");
        form.addEventListener("submit", function (event) {
          event.preventDefault();
          const itemId = input.value.trim();
          if (!itemId) {
            return;
          }
          window.location.href = "<c:url value='/marketplace/' />" + encodeURIComponent(itemId);
        });
      })();
    </script>
  </body>
</html>

