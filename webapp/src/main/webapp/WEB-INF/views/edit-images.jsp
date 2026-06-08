<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="editAvailabilityUrl" value="/edit/${itemId}/availability" />
<c:url var="editImagesUrl" value="/edit/${itemId}/images" />
<spring:message code="editPublication.actions.save" var="editSubmitLabel" />
<spring:message code="publish.validation.images.size" var="galleryFileSizeMessage" />
<spring:message code="publish.back.imagesLost" var="publishBackImagesLostMsg" />
<spring:bind path="publishForm.files">
  <c:set var="filesError" value="${status.errorMessage}" />
</spring:bind>

<spring:message code="page.title.edit" var="titleEdit" />
<paw:layout
  title="${titleEdit} - Botecito"
  mainClass="pt-24 pb-14 w-full max-w-6xl mx-auto px-6"
  headerCtaMessageCode="nav.rent"
  headerCtaHref="/marketplace"
  headerCtaVariant="rent"
  scripts="toast,edit-wizard,form-submit"
>
  <paw:toastNotifier />
  <div
    data-edit-wizard-root="step3"
    data-edit-availability-url="${editAvailabilityUrl}"
    data-item-id="${itemId}"
    data-images-lost-message="${publishBackImagesLostMsg}"
    class="w-full"
  >
    <div class="mb-8">
      <a
        href="${editAvailabilityUrl}"
        data-edit-wizard-back
        class="link link-hover inline-flex items-center gap-2 text-secondary font-bold font-headline no-underline w-fit"
      >
        <span class="material-symbols-outlined">arrow_back</span>
        <span><spring:message code="common.back" /></span>
      </a>
    </div>

    <div class="mb-10">
      <div
        class="flex items-center justify-between gap-4 text-sm font-bold text-outline uppercase tracking-wider"
      >
        <span><spring:message code="editPublication.step3.progress" /></span>
        <span><spring:message code="editPublication.step3.badge" /></span>
      </div>
      <progress
        class="progress progress-secondary mt-3 w-full"
        value="66"
        max="100"
      ></progress>
      <h1
        class="mt-6 text-4xl font-extrabold tracking-tight text-on-background m-0"
      >
        <spring:message code="editPublication.step3.title" />
      </h1>
      <p class="text-on-surface-variant mt-2 text-lg m-0">
        <spring:message code="editPublication.step3.subtitle" />
      </p>
      <p class="mt-4 text-base font-bold text-on-surface m-0">
        <span data-edit-wizard-title></span>
      </p>
    </div>

    <spring:hasBindErrors name="publishForm">
      <c:forEach var="error" items="${errors.globalErrors}">
        <paw:alertMessage type="error" cssClass="mb-4">
          <spring:message
            code="${error.codes[0]}"
            arguments="${error.arguments}"
          />
        </paw:alertMessage>
      </c:forEach>
    </spring:hasBindErrors>

    <form:form
      action="${editImagesUrl}"
      method="post"
      modelAttribute="publishForm"
      enctype="multipart/form-data"
      class="space-y-8"
      data-submit-loading-form="true"
      data-max-file-bytes="${maxUploadFileBytes}"
      data-max-file-message="${galleryFileSizeMessage}"
    >
      <div data-edit-wizard-hidden-fields></div>
      <input type="hidden" name="galleryOrder" value="" data-edit-gallery-order-input />

      <paw:sectionCard icon="add_a_photo" hostAccent="true">
        <jsp:attribute name="title"
          ><spring:message code="publish.step3.section.photos"
        /></jsp:attribute>
        <jsp:body>
          <paw:imageGalleryEditor
            imageUrls="${galleryPreviewUrls}"
            maxImages="${maxGalleryImages}"
            errorMessage="${filesError}"
            hostAccent="true"
          />
          <form:errors path="editGalleryWithinLimit" cssClass="text-error text-xs mt-2" element="p" />
          <div
            class="rounded-xl bg-base-200 p-4 text-sm text-on-surface-variant leading-relaxed mt-4"
          >
            <spring:message code="publish.image.note" />
          </div>
        </jsp:body>
      </paw:sectionCard>

      <paw:sectionCard icon="check_circle" hostAccent="true">
        <jsp:body>
          <div
            class="flex flex-col sm:flex-row sm:justify-end items-center gap-4"
          >
            <button
              type="submit"
              class="btn btn-secondary btn-lg w-full sm:w-auto"
              data-submit-loading-button
            >
              <span
                class="flex items-center justify-center gap-2"
                data-submit-loading-content
              >
                <c:out value="${editSubmitLabel}" />
                <span class="material-symbols-outlined text-sm">check_circle</span>
              </span>
              <span
                class="pointer-events-none absolute inset-0 hidden items-center justify-center"
                aria-hidden="true"
                data-submit-loading-spinner
              >
                <span class="loading loading-spinner loading-sm"></span>
              </span>
            </button>
          </div>
        </jsp:body>
      </paw:sectionCard>
    </form:form>
  </div>
</paw:layout>
