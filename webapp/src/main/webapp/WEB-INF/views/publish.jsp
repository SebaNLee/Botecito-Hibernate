<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="homeUrl" value="/" />
<c:url var="publishUrl" value="/publish" />
<c:url var="marketplaceUrl" value="/marketplace" />
<c:url var="placeholderImageUrl" value="/css/boat-placeholder.svg" />
<spring:message code="publish.form.title.placeholder" var="publishTitlePlaceholder" />
<spring:message code="publish.form.description.placeholder" var="publishDescriptionPlaceholder" />
<spring:message code="publish.form.type.placeholder" var="publishTypePlaceholder" />
<spring:message code="publish.form.price.placeholder" var="publishPricePlaceholder" />
<spring:message code="publish.form.location.label" var="publishLocationLabel" />
<spring:message code="publish.form.location.placeholder" var="publishLocationPlaceholder" />
<spring:message code="publish.form.capacity.label" var="publishCapacityLabel" />
<spring:message code="publish.form.maxWeight.placeholder" var="publishMaxWeightPlaceholder" />
<spring:message code="publish.form.difficulty.placeholder" var="publishDifficultyPlaceholder" />
<spring:message code="publish.type.other" var="publishTypeOther" />
<spring:message code="publish.type.kayak" var="publishTypeKayak" />
<spring:message code="publish.type.paddle" var="publishTypePaddle" />
<spring:message code="publish.type.canoe" var="publishTypeCanoe" />
<spring:message code="publish.type.windsurf" var="publishTypeWindsurf" />
<spring:message code="publish.type.efoil" var="publishTypeEFoil" />
<spring:message code="publish.type.optimist" var="publishTypeOptimist" />
<spring:message code="publish.difficulty.1" var="publishDifficulty1" />
<spring:message code="publish.difficulty.2" var="publishDifficulty2" />
<spring:message code="publish.difficulty.3" var="publishDifficulty3" />
<spring:message code="publish.difficulty.4" var="publishDifficulty4" />
<spring:message code="publish.difficulty.5" var="publishDifficulty5" />

<paw:layout
  title="Botecito"
  mainClass="pt-24 pb-14 max-w-6xl mx-auto px-6"
  headerCtaMessageCode="nav.rent"
  headerCtaHref="/marketplace"
  headerCtaVariant="rent">
  <div class="mb-8">
    <a href="${homeUrl}" class="flex items-center gap-2 text-primary hover:opacity-80 transition-opacity font-bold font-manrope bg-transparent no-underline w-fit">
      <span class="material-symbols-outlined">arrow_back</span>
      <span><spring:message code="common.back" /></span>
    </a>
  </div>

  <div class="mb-10">
    <div class="flex items-center justify-between gap-4 text-sm font-bold text-outline uppercase tracking-wider">
      <span><spring:message code="publish.step1.progress" /></span>
      <span><spring:message code="publish.step1.badge" /></span>
    </div>
    <div class="mt-3 h-2 w-full rounded-full bg-surface-container-high overflow-hidden">
      <div class="h-full w-1/3 rounded-full bg-primary"></div>
    </div>
    <h1 class="mt-6 text-4xl font-extrabold tracking-tight text-on-background m-0"><spring:message code="publish.step1.title" /></h1>
    <p class="text-on-surface-variant mt-2 text-lg m-0"><spring:message code="publish.step1.subtitle" /></p>
  </div>

  <form:form action="${publishUrl}" method="post" modelAttribute="publishForm" enctype="multipart/form-data" class="space-y-8">
    <div class="grid grid-cols-1 lg:grid-cols-5 gap-8 items-start">
      <section class="lg:col-span-3 bg-surface-container-lowest rounded-2xl p-8 shadow-[0_32px_48px_rgba(11,28,50,0.04)] space-y-6">
        <div class="flex items-center gap-3 pb-4 border-b border-outline-variant/20">
          <span class="material-symbols-outlined text-primary text-2xl">directions_boat</span>
          <h2 class="text-xl font-extrabold m-0"><spring:message code="publish.step1.section.base" /></h2>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div class="space-y-2 md:col-span-2">
            <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="title"><spring:message code="publish.form.title.label" /></label>
            <form:input path="title" id="title" class="w-full px-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface placeholder:text-outline" placeholder="${publishTitlePlaceholder}"/>
            <form:errors path="title" cssClass="mt-1 block rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs font-medium text-red-700" />
          </div>

          <div class="space-y-2 md:col-span-2">
            <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="description"><spring:message code="publish.form.description.label" /></label>
            <form:textarea path="description" id="description" rows="5" class="w-full px-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface placeholder:text-outline resize-none" placeholder="${publishDescriptionPlaceholder}"/>
            <form:errors path="description" cssClass="mt-1 block rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs font-medium text-red-700" />
          </div>

          <div class="space-y-2">
            <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="itemTypeId"><spring:message code="publish.form.type.label" /></label>
            <form:select path="itemTypeId" id="itemTypeId" class="w-full px-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface">
              <form:option value="" label="${publishTypePlaceholder}" />
              <form:option value="1" label="${publishTypeOther}" />
              <form:option value="2" label="${publishTypeKayak}" />
              <form:option value="3" label="${publishTypePaddle}" />
              <form:option value="4" label="${publishTypeCanoe}" />
              <form:option value="5" label="${publishTypeWindsurf}" />
              <form:option value="6" label="${publishTypeEFoil}" />
              <form:option value="7" label="${publishTypeOptimist}" />
            </form:select>
            <form:errors path="itemTypeId" cssClass="mt-1 block rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs font-medium text-red-700" />
          </div>

          <div class="space-y-2">
            <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="pricePerHour"><spring:message code="publish.form.price.label" /></label>
            <form:input path="pricePerHour" id="pricePerHour" type="number" min="0" class="w-full px-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface" placeholder="${publishPricePlaceholder}"/>
            <form:errors path="pricePerHour" cssClass="mt-1 block rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs font-medium text-red-700" />
          </div>

          <div class="md:col-span-2">
            <paw:locationPicker
                id="marina"
                name="marina"
                label="${publishLocationLabel}"
                value="${publishForm.marina}"
                placeholder="${publishLocationPlaceholder}"
                icon="location_on"
                errorPath="marina" />
          </div>

          <div>
            <paw:peopleCount
                id="capacity"
                name="capacity"
                label="${publishCapacityLabel}"
                value="${empty publishForm.capacity ? '2' : publishForm.capacity}"
                min="1"
                max="20"
                errorPath="capacity" />
          </div>

          <div class="space-y-2">
            <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="maxWeight"><spring:message code="publish.form.maxWeight.label" /></label>
            <div class="relative">
              <form:input path="maxWeight" id="maxWeight" type="number" min="0" class="w-full px-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface" placeholder="${publishMaxWeightPlaceholder}"/>
              <span class="absolute right-4 top-1/2 -translate-y-1/2 text-outline text-sm font-bold">KG</span>
            </div>
            <form:errors path="maxWeight" cssClass="mt-1 block rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs font-medium text-red-700" />
          </div>

          <div class="space-y-2 md:col-span-2">
            <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="difficultyLevel"><spring:message code="publish.form.difficulty.label" /></label>
            <form:select path="difficultyLevel" id="difficultyLevel" class="w-full px-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface">
              <form:option value="" label="${publishDifficultyPlaceholder}" />
              <form:option value="1" label="${publishDifficulty1}" />
              <form:option value="2" label="${publishDifficulty2}" />
              <form:option value="3" label="${publishDifficulty3}" />
              <form:option value="4" label="${publishDifficulty4}" />
              <form:option value="5" label="${publishDifficulty5}" />
            </form:select>
            <form:errors path="difficultyLevel" cssClass="mt-1 block rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs font-medium text-red-700" />
          </div>
        </div>
      </section>

      <aside class="lg:col-span-2 bg-surface-container-lowest rounded-2xl p-8 shadow-[0_32px_48px_rgba(11,28,50,0.04)] space-y-6">
        <div class="flex items-center gap-3 pb-4 border-b border-outline-variant/20">
          <span class="material-symbols-outlined text-primary text-2xl">add_a_photo</span>
          <h2 class="text-xl font-extrabold m-0"><spring:message code="publish.step1.section.photo" /></h2>
        </div>

        <div class="rounded-xl overflow-hidden border border-outline-variant/30 bg-surface-container-high">
          <img
            id="publish-file-preview"
            src="${not empty uploadedImagePreviewUrl ? uploadedImagePreviewUrl : placeholderImageUrl}"
            alt="<spring:message code='publish.image.previewAlt' />"
            class="w-full aspect-[16/10] object-cover" />
        </div>

        <label class="border-2 border-dashed border-outline-variant rounded-xl p-10 flex flex-col items-center justify-center text-center hover:bg-surface-container-high/50 transition-colors cursor-pointer group">
          <input
            type="file"
            name="file"
            accept="image/*"
            class="hidden"
            data-image-preview-input
            data-image-preview-target-id="publish-file-preview"
            data-image-preview-filename-id="file-name-display"
            data-image-preview-placeholder="${not empty uploadedImagePreviewUrl ? uploadedImagePreviewUrl : placeholderImageUrl}"/>
          <div class="w-16 h-16 bg-primary/10 text-primary rounded-full flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
            <span class="material-symbols-outlined text-3xl">upload_file</span>
          </div>
          <span class="font-bold text-lg text-primary"><spring:message code="publish.image.upload" /></span>
          <span id="file-name-display" class="text-sm text-outline mt-1"><spring:message code="publish.image.helper" /></span>
          <form:errors path="file" cssClass="mt-2 block rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs font-medium text-red-700" />
        </label>

        <div class="rounded-xl bg-surface-container-high p-4 text-sm text-on-surface-variant leading-relaxed">
          <spring:message code="publish.image.note" />
        </div>
      </aside>
    </div>

    <div class="flex flex-col sm:flex-row justify-between items-center gap-4 pt-2">
      <a href="${marketplaceUrl}" class="w-full sm:w-auto px-8 py-4 bg-surface-container-high text-primary rounded-xl font-bold hover:bg-surface-container transition-colors no-underline text-center">
        <spring:message code="publish.actions.saveDraft" />
      </a>
      <button type="submit" class="w-full sm:w-auto px-10 py-4 bg-secondary text-white rounded-xl font-bold shadow-[0_16px_32px_rgba(174,49,35,0.24)] hover:bg-[#962619] transition-all active:scale-[0.98] flex items-center justify-center gap-2 border-none cursor-pointer">
        <spring:message code="publish.actions.continueAvailability" />
        <span class="material-symbols-outlined text-sm">arrow_forward</span>
      </button>
    </div>
  </form:form>
</paw:layout>
