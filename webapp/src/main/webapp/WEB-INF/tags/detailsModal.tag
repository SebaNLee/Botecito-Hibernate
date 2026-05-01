<%@ tag language="java" pageEncoding="UTF-8" body-content="scriptless" %>
<%@ attribute name="id" required="true" %>
<%@ attribute name="title" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<dialog id="${id}" class="modal">
  <div class="modal-box max-w-2xl">
    <form method="dialog" class="m-0">
      <button type="submit" class="btn btn-sm btn-circle btn-ghost absolute right-2 top-2" aria-label="close">
        <span class="material-symbols-outlined text-base leading-none">close</span>
      </button>
    </form>
    <c:if test="${not empty title}">
      <h3 class="m-0 mb-4 pr-10 text-lg font-extrabold tracking-tight"><c:out value="${title}" /></h3>
    </c:if>
    <div class="space-y-4">
      <jsp:doBody />
    </div>
  </div>
  <form method="dialog" class="modal-backdrop">
    <button aria-label="close">close</button>
  </form>
</dialog>
