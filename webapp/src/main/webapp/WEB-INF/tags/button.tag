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
<%@ attribute name="submitLoading" required="false" type="java.lang.Boolean" %>
<%@ attribute name="loadingText" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="btnColorAttr" value="${not empty color ? color : ''}" />
<c:set var="btnVariantAttr" value="${not empty variant ? variant : ''}" />
<c:set var="btnSize" value="${not empty size ? size : 'md'}" />
<c:set var="btnCssClass" value="${not empty cssClass ? cssClass : ''}" />
<c:set var="btnDisabled" value="${disabled ne null ? disabled : false}" />
<c:set var="btnType" value="${not empty type ? type : 'button'}" />
<c:set var="btnFullWidth" value="${fullWidth ne null ? fullWidth : false}" />
<c:set var="btnIconTrailing" value="${iconTrailing ne null ? iconTrailing : false}" />
<c:set var="btnSubmitLoading" value="${submitLoading ne null ? submitLoading : false}" />

<c:set var="btnColorResolved" value="${empty btnColorAttr and empty btnVariantAttr ? 'primary' : btnColorAttr}" />

<c:choose>
  <c:when test="${btnColorResolved == 'secondary'}">
    <c:set var="btnColorClass" value="btn-secondary" />
  </c:when>
  <c:when test="${btnColorResolved == 'outline'}">
    <c:set var="btnColorClass" value="btn-outline" />
  </c:when>
  <c:when test="${btnColorResolved == 'ghost'}">
    <c:set var="btnColorClass" value="btn-ghost" />
  </c:when>
  <c:when test="${btnColorResolved == 'success'}">
    <c:set var="btnColorClass" value="btn-success" />
  </c:when>
  <c:when test="${btnColorResolved == 'danger'}">
    <c:set var="btnColorClass" value="btn-error" />
  </c:when>
  <c:when test="${btnColorResolved == 'neutral'}">
    <c:set var="btnColorClass" value="btn-neutral" />
  </c:when>
  <c:when test="${btnColorResolved == 'primary'}">
    <c:set var="btnColorClass" value="btn-primary" />
  </c:when>
  <c:otherwise>
    <c:set var="btnColorClass" value="" />
  </c:otherwise>
</c:choose>

<c:choose>
  <c:when test="${btnVariantAttr == 'outline'}">
    <c:set var="btnVariantClass" value="btn-outline" />
  </c:when>
  <c:when test="${btnVariantAttr == 'soft'}">
    <c:set var="btnVariantClass" value="btn-soft" />
  </c:when>
  <c:when test="${btnVariantAttr == 'ghost'}">
    <c:set var="btnVariantClass" value="btn-ghost" />
  </c:when>
  <c:when test="${btnVariantAttr == 'link'}">
    <c:set var="btnVariantClass" value="btn-link" />
  </c:when>
  <c:otherwise>
    <c:set var="btnVariantClass" value="" />
  </c:otherwise>
</c:choose>

<c:choose>
  <c:when test="${btnSize == 'sm'}">
    <c:set var="btnSizeClass" value="btn-sm" />
  </c:when>
  <c:when test="${btnSize == 'lg'}">
    <c:set var="btnSizeClass" value="btn-lg" />
  </c:when>
  <c:when test="${btnSize == 'xs'}">
    <c:set var="btnSizeClass" value="btn-xs" />
  </c:when>
  <c:otherwise>
    <c:set var="btnSizeClass" value="" />
  </c:otherwise>
</c:choose>

<c:set var="btnWidthClass" value="${btnFullWidth ? 'btn-block' : ''}" />
<c:set var="btnClasses" value="btn ${btnColorClass} ${btnVariantClass} ${btnSizeClass} ${btnWidthClass} no-underline ${btnCssClass}" />

<c:choose>
  <c:when test="${not empty href and not btnDisabled}">
    <a href="${href}" class="${btnClasses}">
      <c:if test="${not empty icon and not btnIconTrailing}">
        <span class="material-symbols-outlined text-base leading-none"><c:out value="${icon}" /></span>
      </c:if>
      <span><c:out value="${text}" /></span>
      <c:if test="${not empty icon and btnIconTrailing}">
        <span class="material-symbols-outlined text-base leading-none"><c:out value="${icon}" /></span>
      </c:if>
    </a>
  </c:when>
  <c:when test="${not empty href and btnDisabled}">
    <span class="${btnClasses} btn-disabled" role="link" aria-disabled="true" tabindex="-1">
      <c:if test="${not empty icon and not btnIconTrailing}">
        <span class="material-symbols-outlined text-base leading-none"><c:out value="${icon}" /></span>
      </c:if>
      <span><c:out value="${text}" /></span>
      <c:if test="${not empty icon and btnIconTrailing}">
        <span class="material-symbols-outlined text-base leading-none"><c:out value="${icon}" /></span>
      </c:if>
    </span>
  </c:when>
  <c:otherwise>
    <button type="${btnType}" class="${btnClasses} ${btnSubmitLoading ? 'relative' : ''}" <c:if test="${btnDisabled}">disabled</c:if> <c:if test="${btnSubmitLoading}">data-submit-loading-button</c:if> <c:if test="${not empty loadingText}">data-loading-text="${loadingText}"</c:if>>
      <span class="${btnSubmitLoading ? 'flex items-center justify-center gap-2' : ''}" <c:if test="${btnSubmitLoading}">data-submit-loading-content</c:if>>
        <c:if test="${not empty icon and not btnIconTrailing}">
          <span class="material-symbols-outlined text-base leading-none"><c:out value="${icon}" /></span>
        </c:if>
        <span><c:out value="${text}" /></span>
        <c:if test="${not empty icon and btnIconTrailing}">
          <span class="material-symbols-outlined text-base leading-none"><c:out value="${icon}" /></span>
        </c:if>
      </span>
      <c:if test="${btnSubmitLoading}">
        <span class="pointer-events-none absolute inset-0 hidden items-center justify-center" aria-hidden="true" data-submit-loading-spinner>
          <span class="loading loading-spinner loading-sm"></span>
        </span>
      </c:if>
    </button>
  </c:otherwise>
</c:choose>
