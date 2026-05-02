<%@ tag language="java" pageEncoding="UTF-8" body-content="scriptless" %>
<%@ attribute name="id" required="true" %>
<%@ attribute name="title" required="false" %>
<%@ attribute name="layout" required="false" %>
<%@ attribute name="aside" fragment="true" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="modalLayout" value="${empty layout ? 'single' : layout}" />

<dialog id="${id}" class="modal">
  <c:choose>
    <c:when test="${modalLayout == 'split'}">
      <div class="modal-box w-full max-w-5xl max-h-[90vh] overflow-y-auto">
        <form method="dialog" class="m-0">
          <button type="submit" class="btn btn-sm btn-circle btn-ghost absolute right-2 top-2" aria-label="close">
            <span class="material-symbols-outlined text-base leading-none">close</span>
          </button>
        </form>
        <c:if test="${not empty title}">
          <h3 class="m-0 mb-4 pr-10 text-lg font-extrabold tracking-tight"><c:out value="${title}" /></h3>
        </c:if>
        <div class="grid grid-cols-1 gap-4 lg:grid-cols-[20rem_minmax(0,1fr)]">
          <aside class="space-y-4">
            <jsp:invoke fragment="aside" />
          </aside>
          <div class="space-y-4 min-w-0">
            <jsp:doBody />
          </div>
        </div>
      </div>
    </c:when>
    <c:otherwise>
      <div class="modal-box w-full max-w-2xl max-h-[90vh] overflow-y-auto">
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
    </c:otherwise>
  </c:choose>
  <form method="dialog" class="modal-backdrop">
    <button aria-label="close">close</button>
  </form>
</dialog>
