<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> <%@ taglib
prefix="paw" tagdir="/WEB-INF/tags" %> <%@ taglib prefix="spring"
uri="http://www.springframework.org/tags" %> <%@ page contentType="text/html;
charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="marketplaceUrl" value="/marketplace" />
<c:url var="heroImageUrl" value="/img/landing-hero.png" />
<spring:message code="filters.location" var="locationLabel" />
<spring:message code="filters.location.placeholder" var="locationPlaceholder" />
<spring:message code="filters.date" var="dateLabel" />
<spring:message code="filters.date.placeholder" var="datePlaceholder" />
<spring:message code="filters.time" var="timeLabel" />
<spring:message code="filters.time.placeholder" var="timePlaceholder" />
<spring:message code="filters.people" var="peopleLabel" />
<spring:message code="filters.people.placeholder" var="peoplePlaceholder" />
<spring:message code="page.title.home" var="titleHome" />

<paw:layout
  title="${titleHome}"
  mainClass="relative min-h-screen flex flex-col"
  scripts="search-filters,date-time"
>
  <section
    class="relative flex-grow flex items-center justify-center px-6 py-20 min-h-[870px]"
  >
    <div class="absolute inset-0 z-0 overflow-hidden">
      <img
        class="w-full h-full object-cover"
        alt="<spring:message code='landing.hero.imageAlt' />"
        src="<c:out value='${heroImageUrl}' />"
      />
      <div
        class="absolute inset-0 bg-gradient-to-b from-on-background/40 via-on-background/10 to-background"
      ></div>
    </div>
    <div class="relative z-10 w-full max-w-7xl mx-auto text-center">
      <h1
        class="hero-title font-extrabold text-4xl md:text-6xl text-white mb-6 tracking-tight drop-shadow-sm"
      >
        <spring:message code="landing.hero.title.line1" /><br
          class="hidden md:block"
        />
        <spring:message code="landing.hero.title.line2" />
      </h1>
      <p
        class="font-body text-white/90 text-lg md:text-xl mb-12 max-w-2xl mx-auto"
      >
        <spring:message code="landing.hero.subtitle" />
      </p>

      <form
        action="<c:out value='${marketplaceUrl}' />"
        method="get"
        data-filter-form="landing"
        class="bg-base-100/95 backdrop-blur-sm border border-base-200 p-2 md:p-3 rounded-2xl md:rounded-[2rem] shadow-xl w-full max-w-7xl mx-auto flex flex-col md:flex-row md:items-stretch items-center gap-2"
      >
        <div class="w-full min-w-0 px-2 py-3 md:px-3 group md:flex-1 md:min-w-0">
          <paw:optionsPicker
            id="landing-location"
            name="location"
            label="${locationLabel}"
            placeholder="${locationPlaceholder}"
            icon="location_on"
            variant="inline"
          />
        </div>
        <div class="hidden md:block w-px h-8 bg-outline-variant/20"></div>

        <div class="w-full min-w-0 px-2 py-3 md:px-3 md:flex-1 md:min-w-0">
          <paw:datePicker
            id="landing-date"
            dateFieldName="date"
            label="${dateLabel}"
            placeholder="${datePlaceholder}"
            restrictToAvailability="false"
          />
        </div>
        <div class="hidden md:block w-px h-8 bg-outline-variant/20"></div>

        <div class="w-full min-w-0 px-2 py-3 md:px-3 md:flex-1 md:min-w-0">
          <paw:timeRangePicker
            id="landing-time-range"
            dateInputId="landing-date"
            startTimeFieldName="startTime"
            endTimeFieldName="endTime"
            label="${timeLabel}"
            placeholder="${timePlaceholder}"
            restrictToAvailability="false"
          />
        </div>
        <div class="hidden md:block w-px h-8 bg-outline-variant/20"></div>

        <div class="w-full min-w-0 px-2 py-3 md:px-3 md:flex-1 md:min-w-0">
          <paw:peopleCount
            id="landing-capacity"
            name="capacity"
            label="${peopleLabel}"
            placeholder="${peoplePlaceholder}"
            allowEmpty="true"
            min="1"
            max="20"
            variant="inline"
          />
        </div>

        <button
          type="submit"
          class="btn btn-primary btn-lg w-full md:w-auto md:mx-3 md:my-3 md:shrink-0 md:self-center rounded-full gap-2"
        >
          <span class="material-symbols-outlined">search</span>
          <span><spring:message code="landing.hero.search" /></span>
        </button>
      </form>
    </div>
  </section>
</paw:layout>
