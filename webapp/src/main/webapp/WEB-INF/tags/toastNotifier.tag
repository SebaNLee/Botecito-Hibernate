<%@ tag language="java" pageEncoding="UTF-8" body-content="empty" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<spring:message code="alert.dismiss" var="toastDismissLabel" htmlEscape="true" />

<c:set var="hasQueryToast" value="${not empty param.toastError}" />

<c:if test="${hasQueryToast or not empty toasts}">
  <div class="toast toast-end toast-bottom z-[60]" data-paw-toast-container>
    <c:if test="${hasQueryToast}">
      <div role="status" data-paw-toast data-paw-toast-type="error" class="alert alert-error shadow-lg rounded-xl items-start gap-3 pr-11 max-w-sm">
        <span class="material-symbols-outlined text-lg shrink-0">error</span>
        <span class="min-w-0 flex-1 text-sm">
          <spring:message code="${param.toastError}" />
        </span>
        <button type="button" class="btn btn-ghost btn-sm btn-square absolute top-1.5 right-1.5 shrink-0 h-7 w-7 min-h-0 p-0" data-paw-toast-dismiss aria-label="<c:out value='${toastDismissLabel}' />">
          <span class="material-symbols-outlined text-base leading-none" aria-hidden="true">close</span>
        </button>
      </div>
    </c:if>
    <c:forEach items="${toasts}" var="toast">
      <c:choose>
        <c:when test="${toast.type == 'error'}">
          <c:set var="toastVariantClass" value="alert-error" />
          <c:set var="toastIcon" value="error" />
        </c:when>
        <c:when test="${toast.type == 'success'}">
          <c:set var="toastVariantClass" value="alert-success" />
          <c:set var="toastIcon" value="check_circle" />
        </c:when>
        <c:when test="${toast.type == 'warning'}">
          <c:set var="toastVariantClass" value="alert-warning" />
          <c:set var="toastIcon" value="warning" />
        </c:when>
        <c:otherwise>
          <c:set var="toastVariantClass" value="alert-info" />
          <c:set var="toastIcon" value="info" />
        </c:otherwise>
      </c:choose>
      <c:set var="toastFullClass" value="alert ${toastVariantClass} shadow-lg rounded-xl items-start gap-3 pr-11 max-w-sm" />
      <div role="status" data-paw-toast data-paw-toast-type="<c:out value='${toast.type}' />" class="<c:out value='${toastFullClass}' />">
        <span class="material-symbols-outlined text-lg shrink-0"><c:out value="${toastIcon}" /></span>
        <span class="min-w-0 flex-1 text-sm">
          <c:choose>
            <c:when test="${not empty toast.text}">
              <c:out value="${toast.text}" />
            </c:when>
            <c:otherwise>
              <spring:message code="${toast.code}" />
            </c:otherwise>
          </c:choose>
        </span>
        <button type="button" class="btn btn-ghost btn-sm btn-square absolute top-1.5 right-1.5 shrink-0 h-7 w-7 min-h-0 p-0" data-paw-toast-dismiss aria-label="<c:out value='${toastDismissLabel}' />">
          <span class="material-symbols-outlined text-base leading-none" aria-hidden="true">close</span>
        </button>
      </div>
    </c:forEach>
  </div>
</c:if>
