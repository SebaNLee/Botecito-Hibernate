<%@ tag language="java" pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="imageUrls" required="true" type="java.util.List" %>
<%@ attribute name="maxImages" required="true" type="java.lang.Integer" %>
<%@ attribute name="errorMessage" required="false" %>
<%@ attribute name="hostAccent" required="false" type="java.lang.Boolean" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<c:set var="imageCount" value="${empty imageUrls ? 0 : fn:length(imageUrls)}" />
<c:set var="canAddMore" value="${imageCount < maxImages}" />
<c:set var="gaBgTint" value="${hostAccent ne null and hostAccent ? 'bg-secondary/10' : 'bg-primary/10'}" />
<c:set var="gaTextTint" value="${hostAccent ne null and hostAccent ? 'text-secondary' : 'text-primary'}" />
<c:set var="gaBadgeClass" value="${hostAccent ne null and hostAccent ? 'badge badge-secondary badge-sm font-bold' : 'badge badge-primary badge-sm font-bold'}" />
<spring:message code="gallery.add.label" var="galleryAddLabel" />
<spring:message code="gallery.add.helper" arguments="${maxImages}" var="galleryAddHelper" />
<spring:message code="gallery.full" var="galleryFullLabel" />
<spring:message code="gallery.cover.badge" var="galleryCoverBadge" />
<spring:message code="gallery.remove" var="galleryRemoveLabel" />
<spring:message code="gallery.move.left" var="galleryMoveLeftLabel" />
<spring:message code="gallery.move.right" var="galleryMoveRightLabel" />
<spring:message code="gallery.empty" var="galleryEmptyLabel" />
<spring:message code="gallery.count.label" arguments="${imageCount},${maxImages}" var="galleryCountLabel" />
<spring:message code="gallery.count.label" var="galleryCountMessageTemplate" />

<div class="space-y-4"
     data-image-gallery
     data-max-images="${maxImages}"
     data-gallery-count-template="<c:out value='${galleryCountMessageTemplate}' />"
     data-gallery-cover-badge="<c:out value='${galleryCoverBadge}' />"
     data-gallery-badge-class="${gaBadgeClass}"
     data-gallery-remove-label="<c:out value='${galleryRemoveLabel}' />"
     data-gallery-move-left-label="<c:out value='${galleryMoveLeftLabel}' />"
     data-gallery-move-right-label="<c:out value='${galleryMoveRightLabel}' />">
  <div class="flex items-center justify-between gap-3">
    <p class="m-0 text-xs uppercase tracking-wider font-bold text-outline" data-gallery-count><c:out value="${galleryCountLabel}" /></p>
  </div>

  <c:if test="${not empty errorMessage}">
    <p class="m-0 text-error text-sm"><c:out value="${errorMessage}" /></p>
  </c:if>

  <div data-gallery-empty class="rounded-xl bg-base-200 p-6 text-center text-sm text-on-surface-variant">
    <c:out value="${galleryEmptyLabel}" />
  </div>
  <ul data-gallery-local-preview data-gallery-sortable class="hidden grid grid-cols-1 sm:grid-cols-2 gap-3 list-none p-0 m-0"></ul>

  <c:choose>
    <c:when test="${canAddMore}">
      <label class="border-2 border-dashed border-outline-variant rounded-xl p-6 flex flex-col items-center justify-center text-center hover:bg-base-200/60 transition-colors cursor-pointer group">
        <input type="file"
               name="files"
               accept="image/jpeg,image/png,image/webp,image/gif"
               multiple
               class="hidden"
               data-gallery-file-input />
        <div class="w-12 h-12 ${gaBgTint} ${gaTextTint} rounded-full flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
          <span class="material-symbols-outlined text-2xl">add_photo_alternate</span>
        </div>
        <span class="font-bold text-base ${gaTextTint}"><c:out value="${galleryAddLabel}" /></span>
        <span class="text-xs text-outline mt-1"><c:out value="${galleryAddHelper}" /></span>
      </label>
    </c:when>
    <c:otherwise>
      <p class="m-0 text-xs text-on-surface-variant"><c:out value="${galleryFullLabel}" /></p>
    </c:otherwise>
  </c:choose>
</div>
