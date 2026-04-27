<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="profileUrl" value="/profile" />
<c:url var="uploadUrl" value="/item/${item.id}/gallery/upload" />
<c:url var="removeUrl" value="/item/${item.id}/gallery/delete" />
<c:url var="reorderUrl" value="/item/${item.id}/gallery/reorder" />

<paw:layout
  title="Botecito"
  mainClass="pt-24 pb-14 max-w-4xl mx-auto px-6"
  headerCtaMessageCode="nav.rent"
  headerCtaHref="/marketplace"
  headerCtaVariant="rent">

  <div class="mb-8">
    <a href="${profileUrl}" class="link link-hover inline-flex items-center gap-2 text-primary font-bold font-headline no-underline w-fit">
      <span class="material-symbols-outlined">arrow_back</span>
      <span><spring:message code="common.back" /></span>
    </a>
  </div>

  <div class="mb-10">
    <h1 class="text-4xl font-extrabold tracking-tight text-on-background m-0">
      <spring:message code="gallery.page.title" />
    </h1>
    <p class="text-on-surface-variant mt-2 text-lg m-0"><c:out value="${item.title}" /></p>
  </div>

  <c:set var="errorKey" value="${galleryError}" />
  <c:if test="${not empty errorKey}">
    <div class="mb-6">
      <paw:alertMessage type="error">
        <c:choose>
          <c:when test="${errorKey == 'size'}"><spring:message code="publish.validation.images.size" /></c:when>
          <c:when test="${errorKey == 'type'}"><spring:message code="publish.validation.images.type" /></c:when>
          <c:when test="${errorKey == 'count'}"><spring:message code="publish.validation.images.count" /></c:when>
          <c:when test="${errorKey == 'reorder'}"><spring:message code="gallery.error.reorder" /></c:when>
          <c:otherwise><spring:message code="gallery.error.generic" /></c:otherwise>
        </c:choose>
      </paw:alertMessage>
    </div>
  </c:if>

  <paw:sectionCard icon="photo_library">
    <jsp:attribute name="title"><spring:message code="gallery.page.section" /></jsp:attribute>
    <jsp:body>
      <paw:imageGalleryEditor
          imageUrls="${imageUrls}"
          imageIds="${imageIds}"
          uploadUrl="${uploadUrl}"
          removeUrl="${removeUrl}"
          removeFieldName="imageId"
          reorderUrl="${reorderUrl}"
          maxImages="${maxImages}" />
    </jsp:body>
  </paw:sectionCard>
</paw:layout>
