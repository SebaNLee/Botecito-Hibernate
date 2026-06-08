<%@ tag language="java" pageEncoding="UTF-8" body-content="scriptless" %>
<%@ attribute name="title" required="false" %>
<%@ attribute name="icon" required="false" %>
<%@ attribute name="cssClass" required="false" %>
<%@ attribute name="element" required="false" %>
<%@ attribute name="hostAccent" required="false" type="java.lang.Boolean" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:choose>
  <c:when test="${hostAccent ne null and hostAccent}">
    <c:set var="iconToneClass" value="text-secondary" />
  </c:when>
  <c:otherwise>
    <c:set var="iconToneClass" value="text-primary" />
  </c:otherwise>
</c:choose>
<c:set var="resolvedCssClass" value="${not empty cssClass ? cssClass : ''}" />
<c:set var="tag" value="${not empty element ? element : 'section'}" />
<c:set var="classes" value="card bg-base-100 shadow-sm border-outline-variant/30 ${resolvedCssClass}" />
<c:set var="iconSpanClass" value="material-symbols-outlined ${iconToneClass} text-2xl" />

<c:choose>
  <c:when test="${tag == 'aside'}">
    <aside class="<c:out value='${classes}' />">
      <div class="card-body p-8 gap-6">
        <c:if test="${not empty title}">
          <div class="flex items-center gap-3 pb-4 border-b border-outline-variant/25">
            <c:if test="${not empty icon}">
              <span class="<c:out value='${iconSpanClass}' />"><c:out value="${icon}" /></span>
            </c:if>
            <h2 class="card-title text-xl font-extrabold m-0"><c:out value="${title}" /></h2>
          </div>
        </c:if>
        <jsp:doBody />
      </div>
    </aside>
  </c:when>
  <c:otherwise>
    <section class="<c:out value='${classes}' />">
      <div class="card-body p-8 gap-6">
        <c:if test="${not empty title}">
          <div class="flex items-center gap-3 pb-4 border-b border-outline-variant/25">
            <c:if test="${not empty icon}">
              <span class="<c:out value='${iconSpanClass}' />"><c:out value="${icon}" /></span>
            </c:if>
            <h2 class="card-title text-xl font-extrabold m-0"><c:out value="${title}" /></h2>
          </div>
        </c:if>
        <jsp:doBody />
      </div>
    </section>
  </c:otherwise>
</c:choose>
