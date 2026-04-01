<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ attribute name="text" required="true" %>
<%@ attribute name="color" required="false" %>
<%@ attribute name="variant" required="false" %>
<%@ attribute name="size" required="false" %>
<%@ attribute name="cssClass" required="false" %>
<%@ attribute name="disabled" required="false" type="java.lang.Boolean" %>
<%@ attribute name="href" required="false" %>
<%@ attribute name="type" required="false" %>
<%@ attribute name="fullWidth" required="false" type="java.lang.Boolean" %>
<%@ attribute name="icon" required="false" %>
<%@ attribute name="iconTrailing" required="false" type="java.lang.Boolean" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="btnColor" value="${not empty variant ? variant : (not empty color ? color : 'primary')}" />
<c:set var="btnSize" value="${not empty size ? size : 'md'}" />
<c:set var="btnCssClass" value="${not empty cssClass ? cssClass : ''}" />
<c:set var="btnDisabled" value="${disabled ne null ? disabled : false}" />
<c:set var="btnType" value="${not empty type ? type : 'button'}" />
<c:set var="btnFullWidth" value="${fullWidth ne null ? fullWidth : false}" />
<c:set var="btnIconTrailing" value="${iconTrailing ne null ? iconTrailing : false}" />
<c:set var="btnBaseClass" value="btn-link inline-flex items-center justify-center gap-2 rounded-pill font-semibold tracking-tight transition duration-200 focus:outline-none focus:ring-4 focus:ring-brand-primary/15 disabled:pointer-events-none disabled:opacity-50" />

<c:choose>
  <c:when test="${btnColor == 'secondary'}">
    <c:set var="btnColorClass" value="bg-brand-secondary text-white shadow-soft hover:bg-[#962619]" />
  </c:when>
  <c:when test="${btnColor == 'outline'}">
    <c:set var="btnColorClass" value="border border-brand-border bg-white text-brand-foreground hover:border-brand-primary hover:text-brand-primary" />
  </c:when>
  <c:when test="${btnColor == 'ghost'}">
    <c:set var="btnColorClass" value="bg-transparent text-brand-primary hover:bg-brand-surface-soft" />
  </c:when>
  <c:when test="${btnColor == 'success'}">
    <c:set var="btnColorClass" value="bg-brand-success text-white shadow-soft hover:opacity-95" />
  </c:when>
  <c:when test="${btnColor == 'danger'}">
    <c:set var="btnColorClass" value="bg-brand-danger text-white shadow-soft hover:opacity-95" />
  </c:when>
  <c:otherwise>
    <c:set var="btnColorClass" value="bg-brand-primary text-white shadow-soft hover:bg-brand-accent" />
  </c:otherwise>
</c:choose>

<c:choose>
  <c:when test="${btnSize == 'sm'}">
    <c:set var="btnSizeClass" value="min-h-9 px-3.5 text-xs" />
  </c:when>
  <c:when test="${btnSize == 'lg'}">
    <c:set var="btnSizeClass" value="min-h-13 px-6 text-base" />
  </c:when>
  <c:otherwise>
    <c:set var="btnSizeClass" value="min-h-11 px-5 text-sm" />
  </c:otherwise>
</c:choose>

<c:set var="btnWidthClass" value="${btnFullWidth ? 'w-full' : ''}" />
<c:set var="btnClasses" value="${btnBaseClass} ${btnColorClass} ${btnSizeClass} ${btnWidthClass} ${btnCssClass}" />

<c:choose>
  <c:when test="${not empty href}">
    <a href="${href}" class="${btnClasses}" <c:if test="${btnDisabled}">aria-disabled="true"</c:if>>
      <c:if test="${not empty icon and not btnIconTrailing}">
        <span class="text-base leading-none"><c:out value="${icon}" /></span>
      </c:if>
      <span><c:out value="${text}" /></span>
      <c:if test="${not empty icon and btnIconTrailing}">
        <span class="text-base leading-none"><c:out value="${icon}" /></span>
      </c:if>
    </a>
  </c:when>
  <c:otherwise>
    <button type="${btnType}" class="${btnClasses}" <c:if test="${btnDisabled}">disabled</c:if>>
      <c:if test="${not empty icon and not btnIconTrailing}">
        <span class="text-base leading-none"><c:out value="${icon}" /></span>
      </c:if>
      <span><c:out value="${text}" /></span>
      <c:if test="${not empty icon and btnIconTrailing}">
        <span class="text-base leading-none"><c:out value="${icon}" /></span>
      </c:if>
    </button>
  </c:otherwise>
</c:choose>
