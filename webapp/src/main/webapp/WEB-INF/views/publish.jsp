<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> <%@ taglib
prefix="form" uri="http://www.springframework.org/tags/form" %> <%@ taglib
prefix="paw" tagdir="/WEB-INF/tags" %> <%@ taglib prefix="spring"
uri="http://www.springframework.org/tags" %> <%@ page contentType="text/html;
charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="myBoatsUrl" value="/my-boats" />
<c:url var="publishUrl" value="/publish" />
<spring:message
  code="publish.form.title.placeholder"
  var="publishTitlePlaceholder"
/>
<spring:message
  code="publish.form.description.placeholder"
  var="publishDescriptionPlaceholder"
/>
<spring:message
  code="publish.form.type.placeholder"
  var="publishTypePlaceholder"
/>
<spring:message
  code="publish.form.price.placeholder"
  var="publishPricePlaceholder"
/>
<spring:message code="publish.form.location.label" var="publishLocationLabel" />
<spring:message
  code="publish.form.location.placeholder"
  var="publishLocationPlaceholder"
/>
<spring:message code="publish.form.capacity.label" var="publishCapacityLabel" />
<spring:message
  code="publish.form.weight.placeholder"
  var="publishWeightPlaceholder"
/>
<spring:message
  code="publish.form.difficulty.placeholder"
  var="publishDifficultyPlaceholder"
/>
<spring:message code="publish.form.title.label" var="publishTitleLabel" />
<spring:message
  code="publish.form.description.label"
  var="publishDescriptionLabel"
/>
<spring:message code="publish.form.type.label" var="publishTypeLabel" />
<spring:message code="publish.form.price.label" var="publishPriceLabel" />
<spring:message code="publish.form.weight.label" var="publishWeightLabel" />
<spring:message
  code="publish.form.difficulty.label"
  var="publishDifficultyLabel"
/>
<spring:message code="filters.itemType.panel" var="publishItemTypePanelCaption" />
<spring:message code="filters.itemType.noMatch" var="publishItemTypeNoMatchCaption" />
<spring:message
  code="publish.validation.itemType.required"
  var="publishItemTypeRequiredMsg"
/>
<spring:message code="publish.difficulty.1" var="publishDifficulty1" />
<spring:message code="publish.difficulty.2" var="publishDifficulty2" />
<spring:message code="publish.difficulty.3" var="publishDifficulty3" />
<spring:message code="publish.difficulty.4" var="publishDifficulty4" />
<spring:message code="publish.difficulty.5" var="publishDifficulty5" />
<spring:message
  code="publish.actions.continueAvailability"
  var="publishContinueAvailabilityLabel"
/>
<spring:message
  code="publish.validation.location.required"
  var="publishLocationRequiredMsg"
/>

<spring:message code="page.title.publish" var="titlePublish" />
<paw:layout
  title="${titlePublish} - Botecito"
  mainClass="pt-24 pb-14 w-full max-w-6xl mx-auto px-6"
  headerCtaMessageCode="nav.rent"
  headerCtaHref="/marketplace"
  headerCtaVariant="rent"
  scripts="search-filters,publish-wizard"
>
  <div
    data-publish-wizard-root="step1"
    data-publish-wizard-step1-form="true"
    class="w-full"
  >
    <div class="mb-8">
      <a
        href="<c:out value='${myBoatsUrl}' />"
        class="link link-hover inline-flex items-center gap-2 text-secondary font-bold font-headline no-underline w-fit"
      >
        <span class="material-symbols-outlined">arrow_back</span>
        <span><spring:message code="manageAvailability.back.myBoats" /></span>
      </a>
    </div>

    <div class="mb-10">
      <div
        class="flex items-center justify-between gap-4 text-sm font-bold text-outline uppercase tracking-wider"
      >
        <span><spring:message code="publish.step1.progress" /></span>
        <span><spring:message code="publish.step1.badge" /></span>
      </div>
      <progress
        class="progress progress-secondary mt-3 w-full"
        value="0"
        max="100"
      ></progress>
      <h1
        class="mt-6 text-4xl font-extrabold tracking-tight text-on-background m-0"
      >
        <spring:message code="publish.step1.title" />
      </h1>
      <p class="text-on-surface-variant mt-2 text-lg m-0">
        <spring:message code="publish.step1.subtitle" />
      </p>
    </div>

    <form:form
      action="${publishUrl}"
      method="post"
      modelAttribute="publishForm"
      class="space-y-8"
    >
      <paw:sectionCard icon="directions_boat" hostAccent="true">
        <jsp:attribute name="title"
          ><spring:message code="publish.step1.section.base"
        /></jsp:attribute>
        <jsp:body>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div class="md:col-span-2">
              <paw:formField
                path="title"
                maxlength="100"
                label="${publishTitleLabel}"
                placeholder="${publishTitlePlaceholder}"
                required="true"
              />
            </div>
            <div class="md:col-span-2">
              <paw:textareaField
                path="description"
                label="${publishDescriptionLabel}"
                placeholder="${publishDescriptionPlaceholder}"
                rows="5"
                maxlength="1000"
              />
            </div>
            <paw:optionsPicker
              id="publish-item-type"
              name="itemTypeId"
              label="${publishTypeLabel}"
              value="${publishForm.itemTypeId}"
              placeholder="${publishTypePlaceholder}"
              icon="category"
              optionsUrl="/item-type-options"
              panelCaption="${publishItemTypePanelCaption}"
              emptyCaption="${publishItemTypeNoMatchCaption}"
              errorPath="itemTypeId"
              requiredMessage="${publishItemTypeRequiredMsg}"
              required="true"
              hostAccent="true"
            />
            <paw:formField
              path="pricePerHour"
              type="number"
              min="1"
              max="1500000000"
              step="1"
              label="${publishPriceLabel}"
              placeholder="${publishPricePlaceholder}"
              required="true"
            />
            <div class="md:col-span-2">
              <paw:optionsPicker
                id="locationOptionId"
                name="locationOptionId"
                label="${publishLocationLabel}"
                value="${publishForm.locationOptionId}"
                placeholder="${publishLocationPlaceholder}"
                icon="location_on"
                errorPath="locationOptionId"
                requiredMessage="${publishLocationRequiredMsg}"
                required="true"
                hostAccent="true"
              />
            </div>
            <paw:peopleCount
              id="capacity"
              name="capacity"
              label="${publishCapacityLabel}"
              value="${publishForm.capacity}"
              allowEmpty="true"
              min="1"
              max="20"
              errorPath="capacity"
              required="true"
              hostAccent="true"
            />
            <fieldset class="fieldset">
              <legend
                class="fieldset-legend text-xs font-semibold uppercase tracking-wider text-on-surface-variant"
                for="weight"
              >
                <c:out value="${publishWeightLabel}" />
                <span class="text-error" aria-hidden="true">*</span>
              </legend>
              <spring:bind path="publishForm.weight">
                <c:choose>
                  <c:when test="${status.error}">
                    <c:set var="weightInputClass" value="input w-full input-error" />
                  </c:when>
                  <c:otherwise>
                    <c:set var="weightInputClass" value="input w-full" />
                  </c:otherwise>
                </c:choose>
                <label class="<c:out value='${weightInputClass}' />">
                  <form:input
                    path="weight"
                    id="weight"
                    type="number"
                    min="1"
                    max="15000"
                    step="1"
                    required="required"
                    cssClass="grow bg-transparent border-none outline-none focus:outline-none focus:ring-0 shadow-none p-0"
                    placeholder="${publishWeightPlaceholder}"
                  />
                  <span class="text-outline text-sm font-bold">KG</span>
                </label>
              </spring:bind>
              <form:errors
                path="weight"
                cssClass="text-error text-xs mt-1"
                element="p"
              />
            </fieldset>
            <fieldset class="fieldset md:col-span-2">
              <legend
                class="fieldset-legend text-xs font-semibold uppercase tracking-wider text-on-surface-variant"
                for="difficulty"
              >
                <c:out value="${publishDifficultyLabel}" />
                <span class="text-error" aria-hidden="true">*</span>
              </legend>
              <form:select
                path="difficulty"
                id="difficulty"
                required="required"
                cssClass="select w-full"
                cssErrorClass="select w-full select-error"
              >
                <form:option value="" label="${publishDifficultyPlaceholder}" />
                <form:option value="1" label="${publishDifficulty1}" />
                <form:option value="2" label="${publishDifficulty2}" />
                <form:option value="3" label="${publishDifficulty3}" />
                <form:option value="4" label="${publishDifficulty4}" />
                <form:option value="5" label="${publishDifficulty5}" />
              </form:select>
              <form:errors
                path="difficulty"
                cssClass="text-error text-xs mt-1"
                element="p"
              />
            </fieldset>
          </div>
        </jsp:body>
      </paw:sectionCard>

      <div
        class="flex flex-col sm:flex-row sm:justify-end items-center gap-4 pt-2"
      >
        <paw:button
          type="submit"
          color="secondary"
          size="lg"
          icon="arrow_forward"
          iconTrailing="true"
          cssClass="w-full sm:w-auto"
          text="${publishContinueAvailabilityLabel}"
        />
      </div>
    </form:form>
  </div>
</paw:layout>
