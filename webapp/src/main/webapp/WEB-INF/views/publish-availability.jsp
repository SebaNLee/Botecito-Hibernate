<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="stepOneUrl" value="/publish" />
<c:url var="stepTwoUrl" value="/publish/availability" />
<c:url var="marketplaceUrl" value="/marketplace" />

<paw:layout title="Publicar Bote - Disponibilidad" mainClass="pt-24 pb-14 max-w-6xl mx-auto px-6">
  <div class="mb-8">
    <a href="${stepOneUrl}" class="flex items-center gap-2 text-primary hover:opacity-80 transition-opacity font-bold font-manrope bg-transparent no-underline w-fit">
      <span class="material-symbols-outlined">arrow_back</span>
      <span>Volver</span>
    </a>
  </div>

  <div class="mb-10">
    <div class="flex items-center justify-between gap-4 text-sm font-bold text-outline uppercase tracking-wider">
      <span>Paso 2 de 3</span>
      <span>Disponibilidad semanal</span>
    </div>
    <div class="mt-3 h-2 w-full rounded-full bg-surface-container-high overflow-hidden">
      <div class="h-full w-2/3 rounded-full bg-primary"></div>
    </div>
    <h1 class="mt-6 text-4xl font-extrabold tracking-tight text-on-background m-0">Define dias y horarios</h1>
    <p class="text-on-surface-variant mt-2 text-lg m-0">Selecciona franjas de 30 minutos en cada dia. Podes agregar multiples franjas por dia (minimo 2 horas cada una).</p>
  </div>

  <form:form action="${stepTwoUrl}" method="post" modelAttribute="publishForm" class="space-y-8">

    <section class="bg-surface-container-lowest rounded-2xl p-8 shadow-[0_32px_48px_rgba(11,28,50,0.04)]"
             data-weekly-availability-grid
             data-existing-slots='${existingSlotsJson}'
             data-min-duration="120">
      <div class="flex items-center gap-3 mb-4 pb-4 border-b border-outline-variant/20">
        <span class="material-symbols-outlined text-primary text-2xl">schedule</span>
        <h2 class="text-xl font-extrabold m-0">Franjas semanales</h2>
      </div>

      <div class="mb-4 flex flex-wrap items-center gap-4 text-[10px] font-bold text-on-surface-variant">
        <span class="inline-flex items-center gap-1.5">
          <span class="inline-block h-3 w-3 rounded bg-surface-container-low border border-outline-variant/30"></span>
          Disponible
        </span>
        <span class="inline-flex items-center gap-1.5">
          <span class="inline-block h-3 w-3 rounded bg-gradient-to-br from-primary to-primary-container"></span>
          Seleccionado
        </span>
        <span class="inline-flex items-center gap-1.5">
          <span class="inline-block h-3 w-3 rounded bg-primary/15 border border-primary"></span>
          Inicio de franja
        </span>
        <span class="inline-flex items-center gap-1.5">
          <span class="inline-block h-3 w-3 rounded bg-[rgba(212,227,255,0.35)]"></span>
          No disponible
        </span>
      </div>
      <p class="mb-6 text-xs text-outline">Franja minima: 2 horas. Haz click en un slot de inicio y luego en uno de fin para crear una franja. Click en una franja existente para eliminarla.</p>

      <form:errors path="availabilityByWeekday" cssClass="mb-4 block rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs font-medium text-red-700" />
      <spring:hasBindErrors name="publishForm">
        <c:forEach var="error" items="${errors.globalErrors}">
          <div class="mb-4 block rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs font-medium text-red-700">
            <c:out value="${error.defaultMessage}" />
          </div>
        </c:forEach>
      </spring:hasBindErrors>

      <div class="space-y-6">

        <div class="rounded-xl border border-outline-variant/20 bg-surface-container-high/40 p-4" data-availability-row>
          <label class="inline-flex items-center gap-3 font-bold text-on-surface mb-3 cursor-pointer" for="mondayEnabled">
            <input type="checkbox" id="mondayEnabled" name="enabledDays" value="MONDAY" class="h-4 w-4 accent-primary" data-day-toggle="MONDAY" <c:if test="${enabledWeekdays['MONDAY']}">checked="checked"</c:if> />
            Lunes
          </label>
          <div class="grid grid-cols-6 sm:grid-cols-8 md:grid-cols-12 gap-1.5" data-day-slots="MONDAY"></div>
          <div data-day-summary="MONDAY"></div>
        </div>

        <div class="rounded-xl border border-outline-variant/20 bg-surface-container-high/40 p-4" data-availability-row>
          <label class="inline-flex items-center gap-3 font-bold text-on-surface mb-3 cursor-pointer" for="tuesdayEnabled">
            <input type="checkbox" id="tuesdayEnabled" name="enabledDays" value="TUESDAY" class="h-4 w-4 accent-primary" data-day-toggle="TUESDAY" <c:if test="${enabledWeekdays['TUESDAY']}">checked="checked"</c:if> />
            Martes
          </label>
          <div class="grid grid-cols-6 sm:grid-cols-8 md:grid-cols-12 gap-1.5" data-day-slots="TUESDAY"></div>
          <div data-day-summary="TUESDAY"></div>
        </div>

        <div class="rounded-xl border border-outline-variant/20 bg-surface-container-high/40 p-4" data-availability-row>
          <label class="inline-flex items-center gap-3 font-bold text-on-surface mb-3 cursor-pointer" for="wednesdayEnabled">
            <input type="checkbox" id="wednesdayEnabled" name="enabledDays" value="WEDNESDAY" class="h-4 w-4 accent-primary" data-day-toggle="WEDNESDAY" <c:if test="${enabledWeekdays['WEDNESDAY']}">checked="checked"</c:if> />
            Miercoles
          </label>
          <div class="grid grid-cols-6 sm:grid-cols-8 md:grid-cols-12 gap-1.5" data-day-slots="WEDNESDAY"></div>
          <div data-day-summary="WEDNESDAY"></div>
        </div>

        <div class="rounded-xl border border-outline-variant/20 bg-surface-container-high/40 p-4" data-availability-row>
          <label class="inline-flex items-center gap-3 font-bold text-on-surface mb-3 cursor-pointer" for="thursdayEnabled">
            <input type="checkbox" id="thursdayEnabled" name="enabledDays" value="THURSDAY" class="h-4 w-4 accent-primary" data-day-toggle="THURSDAY" <c:if test="${enabledWeekdays['THURSDAY']}">checked="checked"</c:if> />
            Jueves
          </label>
          <div class="grid grid-cols-6 sm:grid-cols-8 md:grid-cols-12 gap-1.5" data-day-slots="THURSDAY"></div>
          <div data-day-summary="THURSDAY"></div>
        </div>

        <div class="rounded-xl border border-outline-variant/20 bg-surface-container-high/40 p-4" data-availability-row>
          <label class="inline-flex items-center gap-3 font-bold text-on-surface mb-3 cursor-pointer" for="fridayEnabled">
            <input type="checkbox" id="fridayEnabled" name="enabledDays" value="FRIDAY" class="h-4 w-4 accent-primary" data-day-toggle="FRIDAY" <c:if test="${enabledWeekdays['FRIDAY']}">checked="checked"</c:if> />
            Viernes
          </label>
          <div class="grid grid-cols-6 sm:grid-cols-8 md:grid-cols-12 gap-1.5" data-day-slots="FRIDAY"></div>
          <div data-day-summary="FRIDAY"></div>
        </div>

        <div class="rounded-xl border border-outline-variant/20 bg-surface-container-high/40 p-4" data-availability-row>
          <label class="inline-flex items-center gap-3 font-bold text-on-surface mb-3 cursor-pointer" for="saturdayEnabled">
            <input type="checkbox" id="saturdayEnabled" name="enabledDays" value="SATURDAY" class="h-4 w-4 accent-primary" data-day-toggle="SATURDAY" <c:if test="${enabledWeekdays['SATURDAY']}">checked="checked"</c:if> />
            Sabado
          </label>
          <div class="grid grid-cols-6 sm:grid-cols-8 md:grid-cols-12 gap-1.5" data-day-slots="SATURDAY"></div>
          <div data-day-summary="SATURDAY"></div>
        </div>

        <div class="rounded-xl border border-outline-variant/20 bg-surface-container-high/40 p-4" data-availability-row>
          <label class="inline-flex items-center gap-3 font-bold text-on-surface mb-3 cursor-pointer" for="sundayEnabled">
            <input type="checkbox" id="sundayEnabled" name="enabledDays" value="SUNDAY" class="h-4 w-4 accent-primary" data-day-toggle="SUNDAY" <c:if test="${enabledWeekdays['SUNDAY']}">checked="checked"</c:if> />
            Domingo
          </label>
          <div class="grid grid-cols-6 sm:grid-cols-8 md:grid-cols-12 gap-1.5" data-day-slots="SUNDAY"></div>
          <div data-day-summary="SUNDAY"></div>
        </div>

      </div>

      <div data-availability-hidden-inputs></div>
    </section>

    <div class="flex flex-col sm:flex-row justify-between items-center gap-4 pt-2">
      <a href="${marketplaceUrl}" class="w-full sm:w-auto px-8 py-4 bg-surface-container-high text-primary rounded-xl font-bold hover:bg-surface-container transition-colors no-underline text-center">
        Guardar borrador
      </a>
      <button type="submit" class="w-full sm:w-auto px-10 py-4 bg-secondary text-white rounded-xl font-bold shadow-[0_16px_32px_rgba(174,49,35,0.24)] hover:bg-[#962619] transition-all active:scale-[0.98] flex items-center justify-center gap-2 border-none cursor-pointer">
        Continuar a contacto
        <span class="material-symbols-outlined text-sm">arrow_forward</span>
      </button>
    </div>
  </form:form>
</paw:layout>
