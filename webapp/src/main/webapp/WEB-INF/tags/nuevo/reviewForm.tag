<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ attribute name="modelAttribute" required="false" %>
<%@ attribute name="action" required="true" %>
<%@ attribute name="bookingId" required="true" type="java.lang.Integer" %>
<%@ attribute name="returnTo" required="true" %>
<%@ attribute name="itemId" required="false" type="java.lang.Integer" %>
<%@ attribute name="ratingLabel" required="false" %>
<%@ attribute name="commentLabel" required="false" %>
<%@ attribute name="submitLabel" required="false" %>
<%@ attribute name="ratingInputId" required="false" %>
<%@ attribute name="commentInputId" required="false" %>
<%@ attribute name="targetDisplayName" required="false" %>
<%@ attribute name="targetBadgeLabel" required="false" %>
<%@ attribute name="targetBadgeColor" required="false" %>
<%@ attribute name="buttonColor" required="false" %>
<%@ attribute name="cssClass" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<c:set var="resolvedModelAttribute" value="${not empty modelAttribute ? modelAttribute : 'reviewForm'}" />
<c:set var="resolvedRatingLabel" value="${not empty ratingLabel ? ratingLabel : 'Rating'}" />
<c:set var="resolvedCommentLabel" value="${not empty commentLabel ? commentLabel : 'Comment'}" />
<c:set var="resolvedSubmitLabel" value="${not empty submitLabel ? submitLabel : 'Submit'}" />
<c:set var="resolvedRatingInputId" value="${not empty ratingInputId ? ratingInputId : 'review-rating-' + bookingId}" />
<c:set var="resolvedCommentInputId" value="${not empty commentInputId ? commentInputId : 'review-comment-' + bookingId}" />
<c:set var="resolvedButtonColor" value="${not empty buttonColor ? buttonColor : 'primary'}" />
<c:set var="formCssClass" value="space-y-3 border-t border-outline-variant/20 pt-3 ${not empty cssClass ? cssClass : ''}" />

<form:form modelAttribute="${resolvedModelAttribute}" action="${action}" method="post" cssClass="${formCssClass}">
  <input type="hidden" name="returnTo" value="${returnTo}" />
  <c:if test="${not empty itemId}">
    <input type="hidden" name="itemId" value="${itemId}" />
  </c:if>

  <c:if test="${not empty targetDisplayName}">
    <div class="flex items-start justify-between gap-2">
      <p class="m-0 min-w-0 truncate text-xs text-on-surface-variant"><c:out value="${targetDisplayName}" /></p>
      <c:if test="${not empty targetBadgeLabel}">
        <c:set var="badgeColor" value="${not empty targetBadgeColor ? targetBadgeColor : 'badge-primary'}" />
        <span class="badge ${badgeColor} badge-sm shrink-0 font-bold"><c:out value="${targetBadgeLabel}" /></span>
      </c:if>
    </div>
  </c:if>

  <form:errors path="rating" cssClass="text-error text-xs mt-1" element="p" />
  <form:errors path="comment" cssClass="text-error text-xs mt-1" element="p" />

  <div class="grid grid-cols-1 sm:grid-cols-[8rem_minmax(0,1fr)] gap-3 items-center">
    <label class="text-xs font-bold uppercase tracking-wider text-outline" for="${resolvedRatingInputId}"><c:out value="${resolvedRatingLabel}" /></label>
    <div class="flex items-center gap-1" data-rating-stars>
      <form:hidden id="${resolvedRatingInputId}" path="rating" data-rating-value="true" />
      <c:forEach var="starIndex" begin="1" end="5">
        <button type="button" class="btn btn-ghost btn-sm btn-square min-h-9 h-9 w-9 p-0" data-rating-star="${starIndex}" aria-label="${resolvedRatingLabel} ${starIndex}">
          <span class="material-symbols-outlined text-xl leading-none text-outline" style="opacity: 0.35;">star</span>
        </button>
      </c:forEach>
    </div>
  </div>

  <div class="grid grid-cols-1 sm:grid-cols-[8rem_minmax(0,1fr)] gap-3">
    <label class="text-xs font-bold uppercase tracking-wider text-outline mt-2" for="${resolvedCommentInputId}"><c:out value="${resolvedCommentLabel}" /></label>
    <form:textarea id="${resolvedCommentInputId}" path="comment" rows="3" maxlength="1000" cssClass="textarea textarea-bordered w-full" cssErrorClass="textarea textarea-bordered w-full textarea-error" />
  </div>

  <button type="submit" class="btn btn-${resolvedButtonColor} btn-sm"><c:out value="${resolvedSubmitLabel}" /></button>
</form:form>
