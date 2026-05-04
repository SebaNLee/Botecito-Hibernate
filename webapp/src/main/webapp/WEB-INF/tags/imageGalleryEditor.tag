<%@ tag language="java" pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="imageUrls" required="true" type="java.util.List" %>
<%@ attribute name="imageIds" required="false" type="java.util.List" %>
<%@ attribute name="uploadUrl" required="false" %>
<%@ attribute name="uploadFieldName" required="false" %>
<%@ attribute name="removeUrl" required="false" %>
<%@ attribute name="removeFieldName" required="false" %>
<%@ attribute name="reorderUrl" required="false" %>
<%@ attribute name="maxImages" required="true" type="java.lang.Integer" %>
<%@ attribute name="errorMessage" required="false" %>
<%@ attribute name="enctype" required="false" %>
<%@ attribute name="inlineUpload" required="false" type="java.lang.Boolean" %>
<%@ attribute name="displayMode" required="false" %>
<%@ attribute name="hostAccent" required="false" type="java.lang.Boolean" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<c:set var="resolvedUploadField" value="${not empty uploadFieldName ? uploadFieldName : 'files'}" />
<c:set var="resolvedRemoveField" value="${not empty removeFieldName ? removeFieldName : 'index'}" />
<c:set var="resolvedEnctype" value="${not empty enctype ? enctype : 'multipart/form-data'}" />
<c:set var="imageCount" value="${empty imageUrls ? 0 : fn:length(imageUrls)}" />
<c:set var="canAddMore" value="${imageCount < maxImages}" />
<c:set var="resolvedDisplayMode" value="${displayMode == 'modal' ? 'modal' : 'inline'}" />
<c:set var="modalId" value="image-gallery-modal" />
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
<spring:message code="gallery.manage" var="galleryManageLabel" />
<spring:message code="gallery.modal.title" var="galleryModalTitle" />
<spring:message code="gallery.modal.close" var="galleryModalClose" />

<c:if test="${resolvedDisplayMode == 'modal'}">
  <div class="space-y-3" data-image-gallery-trigger>
    <p class="m-0 text-xs uppercase tracking-wider font-bold text-outline"><c:out value="${galleryCountLabel}" /></p>
    <c:if test="${not empty errorMessage}">
      <p class="m-0 text-error text-sm"><c:out value="${errorMessage}" /></p>
    </c:if>
    <button type="button"
            class="w-full flex items-center gap-3 rounded-xl border border-outline-variant/40 bg-base-100 hover:bg-base-200/60 transition-colors p-3 text-left"
            data-gallery-modal-open="${modalId}">
      <c:choose>
        <c:when test="${imageCount > 0}">
          <img src="${imageUrls[0]}" alt="" class="w-20 h-20 rounded-lg object-cover shrink-0" />
        </c:when>
        <c:otherwise>
          <div class="w-20 h-20 rounded-lg ${gaBgTint} ${gaTextTint} flex items-center justify-center shrink-0">
            <span class="material-symbols-outlined text-3xl">add_photo_alternate</span>
          </div>
        </c:otherwise>
      </c:choose>
      <div class="flex-1 min-w-0">
        <div class="font-bold text-base text-on-surface truncate">
          <c:out value="${imageCount > 0 ? galleryManageLabel : galleryAddLabel}" />
        </div>
        <div class="text-xs text-outline mt-0.5">
          <c:choose>
            <c:when test="${imageCount > 0}"><c:out value="${galleryCountLabel}" /></c:when>
            <c:otherwise><c:out value="${galleryAddHelper}" /></c:otherwise>
          </c:choose>
        </div>
      </div>
      <span class="material-symbols-outlined text-outline">chevron_right</span>
    </button>
  </div>

  <dialog id="${modalId}" class="modal" data-image-gallery-modal>
    <div class="modal-box max-w-3xl bg-base-100">
      <form method="dialog" class="m-0">
        <button type="submit" class="btn btn-sm btn-circle btn-ghost absolute right-3 top-3" aria-label="${galleryModalClose}">
          <span class="material-symbols-outlined">close</span>
        </button>
      </form>
      <h3 class="font-extrabold text-2xl tracking-tight m-0 mb-4"><c:out value="${galleryModalTitle}" /></h3>
</c:if>

<div class="space-y-4 ${resolvedDisplayMode == 'modal' ? '' : ''}" data-image-gallery data-display-mode="${resolvedDisplayMode}">
  <div class="flex items-center justify-between gap-3">
    <p class="m-0 text-xs uppercase tracking-wider font-bold text-outline"><c:out value="${galleryCountLabel}" /></p>
  </div>

  <c:if test="${not empty errorMessage}">
    <p class="m-0 text-error text-sm"><c:out value="${errorMessage}" /></p>
  </c:if>

  <c:choose>
    <c:when test="${imageCount == 0}">
      <div class="rounded-xl bg-base-200 p-6 text-center text-sm text-on-surface-variant">
        <c:out value="${galleryEmptyLabel}" />
      </div>
    </c:when>
    <c:otherwise>
      <ul class="grid grid-cols-1 sm:grid-cols-2 gap-3 list-none p-0 m-0"
          data-gallery-sortable
          data-reorder-url="${reorderUrl}">
        <c:forEach var="url" items="${imageUrls}" varStatus="loop">
          <c:set var="itemKey" value="${not empty imageIds ? imageIds[loop.index] : loop.index}" />
          <li class="relative rounded-xl overflow-hidden border border-outline-variant/30 bg-base-200 group"
              data-gallery-item
              data-gallery-key="${itemKey}"
              data-gallery-position="${loop.index}">
            <img src="${url}" alt="" class="w-full aspect-square object-cover block" />
            <c:if test="${loop.first}">
              <span class="absolute top-2 left-2 ${gaBadgeClass}"><c:out value="${galleryCoverBadge}" /></span>
            </c:if>
            <div class="absolute inset-x-0 bottom-0 p-2 flex items-center justify-between gap-2 bg-gradient-to-t from-black/60 to-transparent opacity-100 pointer-events-auto transition-opacity duration-150 [@media(hover:hover)]:opacity-0 [@media(hover:hover)]:pointer-events-none [@media(hover:hover)]:group-hover:opacity-100 [@media(hover:hover)]:group-hover:pointer-events-auto [@media(hover:hover)]:group-focus-within:opacity-100 [@media(hover:hover)]:group-focus-within:pointer-events-auto">
              <div class="flex gap-1">
                <button type="button"
                        class="btn btn-xs btn-circle btn-ghost text-white"
                        data-gallery-move="left"
                        aria-label="${galleryMoveLeftLabel}"
                        ${loop.first ? 'disabled' : ''}>
                  <span class="material-symbols-outlined text-base">chevron_left</span>
                </button>
                <button type="button"
                        class="btn btn-xs btn-circle btn-ghost text-white"
                        data-gallery-move="right"
                        aria-label="${galleryMoveRightLabel}"
                        ${loop.last ? 'disabled' : ''}>
                  <span class="material-symbols-outlined text-base">chevron_right</span>
                </button>
              </div>
              <c:if test="${not empty removeUrl}">
                <form action="${removeUrl}" method="post" class="m-0">
                  <input type="hidden" name="${resolvedRemoveField}" value="${itemKey}" />
                  <button type="submit"
                          class="btn btn-xs btn-circle btn-error"
                          aria-label="${galleryRemoveLabel}">
                    <span class="material-symbols-outlined text-base">close</span>
                  </button>
                </form>
              </c:if>
            </div>
          </li>
        </c:forEach>
      </ul>

      <c:if test="${not empty reorderUrl}">
        <form action="${reorderUrl}" method="post" class="m-0" data-gallery-reorder-form>
          <input type="hidden" name="order" value="" data-gallery-order-input />
        </form>
      </c:if>
    </c:otherwise>
  </c:choose>

  <c:choose>
    <c:when test="${canAddMore}">
      <c:set var="uploadInputBlock">
        <label class="border-2 border-dashed border-outline-variant rounded-xl p-6 flex flex-col items-center justify-center text-center hover:bg-base-200/60 transition-colors cursor-pointer group">
          <input type="file"
                 name="${resolvedUploadField}"
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
      </c:set>
      <c:choose>
        <c:when test="${inlineUpload}">
          ${uploadInputBlock}
        </c:when>
        <c:otherwise>
          <form action="${uploadUrl}" method="post" enctype="${resolvedEnctype}" class="m-0">
            ${uploadInputBlock}
          </form>
        </c:otherwise>
      </c:choose>
    </c:when>
    <c:otherwise>
      <p class="m-0 text-xs text-on-surface-variant"><c:out value="${galleryFullLabel}" /></p>
    </c:otherwise>
  </c:choose>
</div>

<c:if test="${resolvedDisplayMode == 'modal'}">
    </div>
    <form method="dialog" class="modal-backdrop">
      <button type="submit" aria-label="${galleryModalClose}">close</button>
    </form>
  </dialog>
</c:if>
