<%@ tag language="java" pageEncoding="UTF-8" body-content="scriptless" %>
<%@ attribute name="id" required="true" %>
<%@ attribute name="title" required="true" %>
<%@ attribute name="message" required="false" %>
<%@ attribute name="confirmText" required="true" %>
<%@ attribute name="cancelText" required="true" %>
<%@ attribute name="confirmColor" required="false" %>
<%@ attribute name="icon" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:message code="modal.close" var="modalCloseLabel" />

<c:set var="confirmColorResolved" value="${not empty confirmColor ? confirmColor : 'danger'}" />
<c:choose>
  <c:when test="${confirmColorResolved == 'primary'}">
    <c:set var="confirmBtnColorClass" value="btn-primary" />
    <c:set var="iconTintClass" value="bg-primary/15 text-primary" />
  </c:when>
  <c:when test="${confirmColorResolved == 'success'}">
    <c:set var="confirmBtnColorClass" value="btn-success" />
    <c:set var="iconTintClass" value="bg-success/15 text-success" />
  </c:when>
  <c:when test="${confirmColorResolved == 'secondary'}">
    <c:set var="confirmBtnColorClass" value="btn-secondary" />
    <c:set var="iconTintClass" value="bg-secondary/15 text-secondary" />
  </c:when>
  <c:otherwise>
    <c:set var="confirmBtnColorClass" value="btn-error" />
    <c:set var="iconTintClass" value="bg-error/15 text-error" />
  </c:otherwise>
</c:choose>
<c:set var="resolvedIcon" value="${not empty icon ? icon : 'warning'}" />

<dialog id="<c:out value='${id}' />" class="modal">
  <div class="modal-box max-w-lg">
    <div class="flex items-start gap-4">
      <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-full <c:out value='${iconTintClass}' />">
        <span class="material-symbols-outlined"><c:out value="${resolvedIcon}" /></span>
      </div>
      <div class="space-y-2">
        <h3 class="text-xl font-extrabold tracking-tight m-0">
          <c:out value="${title}" />
        </h3>
        <c:if test="${not empty message}">
          <p class="m-0 leading-relaxed text-on-surface-variant">
            <c:out value="${message}" />
          </p>
        </c:if>
      </div>
    </div>

    <div class="modal-action mt-6 flex flex-row flex-nowrap items-center justify-end gap-3">
      <form method="dialog" class="m-0 shrink-0">
        <button type="submit" class="btn btn-outline min-h-11 whitespace-nowrap px-5">
          <c:out value="${cancelText}" />
        </button>
      </form>
      <div class="m-0 flex shrink-0 items-center [&_form]:m-0">
        <jsp:doBody />
      </div>
    </div>
  </div>
  <form method="dialog" class="modal-backdrop">
    <button aria-label="<c:out value='${modalCloseLabel}' />">close</button>
  </form>
</dialog>
