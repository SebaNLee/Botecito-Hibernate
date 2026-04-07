<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
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
    <p class="text-on-surface-variant mt-2 text-lg m-0">Marca en que dias esta disponible tu barco y su franja de alquiler.</p>
  </div>

  <form:form action="${stepTwoUrl}" method="post" modelAttribute="publishForm" class="space-y-8">
    <form:errors path="*" cssClass="bg-red-50 border border-red-200 text-red-700 rounded-xl p-3 text-sm block" />

    <section class="bg-surface-container-lowest rounded-2xl p-8 shadow-[0_32px_48px_rgba(11,28,50,0.04)]">
      <div class="flex items-center gap-3 mb-6 pb-4 border-b border-outline-variant/20">
        <span class="material-symbols-outlined text-primary text-2xl">schedule</span>
        <h2 class="text-xl font-extrabold m-0">Franjas semanales</h2>
      </div>

      <div class="space-y-4" data-availability-grid>
        <div class="grid grid-cols-1 md:grid-cols-[190px_1fr_1fr] gap-3 items-center rounded-xl border border-outline-variant/20 bg-surface-container-high/55 px-4 py-4" data-availability-row>
          <label class="inline-flex items-center gap-3 font-bold text-on-surface" for="mondayEnabled">
            <form:checkbox path="mondayEnabled" id="mondayEnabled" cssClass="h-4 w-4 accent-primary" data-day-toggle="true" />
            Lunes
          </label>
          <div>
            <label class="text-[11px] uppercase tracking-wider text-outline font-bold" for="mondayStartTime">Desde</label>
            <form:input path="mondayStartTime" id="mondayStartTime" type="time" cssClass="mt-1 w-full px-3 py-2 bg-surface-container-lowest rounded-lg border border-outline-variant/30 text-on-surface" />
            <form:errors path="mondayStartTime" cssClass="text-error text-xs mt-1 block" />
          </div>
          <div>
            <label class="text-[11px] uppercase tracking-wider text-outline font-bold" for="mondayEndTime">Hasta</label>
            <form:input path="mondayEndTime" id="mondayEndTime" type="time" cssClass="mt-1 w-full px-3 py-2 bg-surface-container-lowest rounded-lg border border-outline-variant/30 text-on-surface" />
            <form:errors path="mondayEndTime" cssClass="text-error text-xs mt-1 block" />
          </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-[190px_1fr_1fr] gap-3 items-center rounded-xl border border-outline-variant/20 bg-surface-container-high/55 px-4 py-4" data-availability-row>
          <label class="inline-flex items-center gap-3 font-bold text-on-surface" for="tuesdayEnabled">
            <form:checkbox path="tuesdayEnabled" id="tuesdayEnabled" cssClass="h-4 w-4 accent-primary" data-day-toggle="true" />
            Martes
          </label>
          <div>
            <label class="text-[11px] uppercase tracking-wider text-outline font-bold" for="tuesdayStartTime">Desde</label>
            <form:input path="tuesdayStartTime" id="tuesdayStartTime" type="time" cssClass="mt-1 w-full px-3 py-2 bg-surface-container-lowest rounded-lg border border-outline-variant/30 text-on-surface" />
            <form:errors path="tuesdayStartTime" cssClass="text-error text-xs mt-1 block" />
          </div>
          <div>
            <label class="text-[11px] uppercase tracking-wider text-outline font-bold" for="tuesdayEndTime">Hasta</label>
            <form:input path="tuesdayEndTime" id="tuesdayEndTime" type="time" cssClass="mt-1 w-full px-3 py-2 bg-surface-container-lowest rounded-lg border border-outline-variant/30 text-on-surface" />
            <form:errors path="tuesdayEndTime" cssClass="text-error text-xs mt-1 block" />
          </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-[190px_1fr_1fr] gap-3 items-center rounded-xl border border-outline-variant/20 bg-surface-container-high/55 px-4 py-4" data-availability-row>
          <label class="inline-flex items-center gap-3 font-bold text-on-surface" for="wednesdayEnabled">
            <form:checkbox path="wednesdayEnabled" id="wednesdayEnabled" cssClass="h-4 w-4 accent-primary" data-day-toggle="true" />
            Miercoles
          </label>
          <div>
            <label class="text-[11px] uppercase tracking-wider text-outline font-bold" for="wednesdayStartTime">Desde</label>
            <form:input path="wednesdayStartTime" id="wednesdayStartTime" type="time" cssClass="mt-1 w-full px-3 py-2 bg-surface-container-lowest rounded-lg border border-outline-variant/30 text-on-surface" />
            <form:errors path="wednesdayStartTime" cssClass="text-error text-xs mt-1 block" />
          </div>
          <div>
            <label class="text-[11px] uppercase tracking-wider text-outline font-bold" for="wednesdayEndTime">Hasta</label>
            <form:input path="wednesdayEndTime" id="wednesdayEndTime" type="time" cssClass="mt-1 w-full px-3 py-2 bg-surface-container-lowest rounded-lg border border-outline-variant/30 text-on-surface" />
            <form:errors path="wednesdayEndTime" cssClass="text-error text-xs mt-1 block" />
          </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-[190px_1fr_1fr] gap-3 items-center rounded-xl border border-outline-variant/20 bg-surface-container-high/55 px-4 py-4" data-availability-row>
          <label class="inline-flex items-center gap-3 font-bold text-on-surface" for="thursdayEnabled">
            <form:checkbox path="thursdayEnabled" id="thursdayEnabled" cssClass="h-4 w-4 accent-primary" data-day-toggle="true" />
            Jueves
          </label>
          <div>
            <label class="text-[11px] uppercase tracking-wider text-outline font-bold" for="thursdayStartTime">Desde</label>
            <form:input path="thursdayStartTime" id="thursdayStartTime" type="time" cssClass="mt-1 w-full px-3 py-2 bg-surface-container-lowest rounded-lg border border-outline-variant/30 text-on-surface" />
            <form:errors path="thursdayStartTime" cssClass="text-error text-xs mt-1 block" />
          </div>
          <div>
            <label class="text-[11px] uppercase tracking-wider text-outline font-bold" for="thursdayEndTime">Hasta</label>
            <form:input path="thursdayEndTime" id="thursdayEndTime" type="time" cssClass="mt-1 w-full px-3 py-2 bg-surface-container-lowest rounded-lg border border-outline-variant/30 text-on-surface" />
            <form:errors path="thursdayEndTime" cssClass="text-error text-xs mt-1 block" />
          </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-[190px_1fr_1fr] gap-3 items-center rounded-xl border border-outline-variant/20 bg-surface-container-high/55 px-4 py-4" data-availability-row>
          <label class="inline-flex items-center gap-3 font-bold text-on-surface" for="fridayEnabled">
            <form:checkbox path="fridayEnabled" id="fridayEnabled" cssClass="h-4 w-4 accent-primary" data-day-toggle="true" />
            Viernes
          </label>
          <div>
            <label class="text-[11px] uppercase tracking-wider text-outline font-bold" for="fridayStartTime">Desde</label>
            <form:input path="fridayStartTime" id="fridayStartTime" type="time" cssClass="mt-1 w-full px-3 py-2 bg-surface-container-lowest rounded-lg border border-outline-variant/30 text-on-surface" />
            <form:errors path="fridayStartTime" cssClass="text-error text-xs mt-1 block" />
          </div>
          <div>
            <label class="text-[11px] uppercase tracking-wider text-outline font-bold" for="fridayEndTime">Hasta</label>
            <form:input path="fridayEndTime" id="fridayEndTime" type="time" cssClass="mt-1 w-full px-3 py-2 bg-surface-container-lowest rounded-lg border border-outline-variant/30 text-on-surface" />
            <form:errors path="fridayEndTime" cssClass="text-error text-xs mt-1 block" />
          </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-[190px_1fr_1fr] gap-3 items-center rounded-xl border border-outline-variant/20 bg-surface-container-high/55 px-4 py-4" data-availability-row>
          <label class="inline-flex items-center gap-3 font-bold text-on-surface" for="saturdayEnabled">
            <form:checkbox path="saturdayEnabled" id="saturdayEnabled" cssClass="h-4 w-4 accent-primary" data-day-toggle="true" />
            Sabado
          </label>
          <div>
            <label class="text-[11px] uppercase tracking-wider text-outline font-bold" for="saturdayStartTime">Desde</label>
            <form:input path="saturdayStartTime" id="saturdayStartTime" type="time" cssClass="mt-1 w-full px-3 py-2 bg-surface-container-lowest rounded-lg border border-outline-variant/30 text-on-surface" />
            <form:errors path="saturdayStartTime" cssClass="text-error text-xs mt-1 block" />
          </div>
          <div>
            <label class="text-[11px] uppercase tracking-wider text-outline font-bold" for="saturdayEndTime">Hasta</label>
            <form:input path="saturdayEndTime" id="saturdayEndTime" type="time" cssClass="mt-1 w-full px-3 py-2 bg-surface-container-lowest rounded-lg border border-outline-variant/30 text-on-surface" />
            <form:errors path="saturdayEndTime" cssClass="text-error text-xs mt-1 block" />
          </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-[190px_1fr_1fr] gap-3 items-center rounded-xl border border-outline-variant/20 bg-surface-container-high/55 px-4 py-4" data-availability-row>
          <label class="inline-flex items-center gap-3 font-bold text-on-surface" for="sundayEnabled">
            <form:checkbox path="sundayEnabled" id="sundayEnabled" cssClass="h-4 w-4 accent-primary" data-day-toggle="true" />
            Domingo
          </label>
          <div>
            <label class="text-[11px] uppercase tracking-wider text-outline font-bold" for="sundayStartTime">Desde</label>
            <form:input path="sundayStartTime" id="sundayStartTime" type="time" cssClass="mt-1 w-full px-3 py-2 bg-surface-container-lowest rounded-lg border border-outline-variant/30 text-on-surface" />
            <form:errors path="sundayStartTime" cssClass="text-error text-xs mt-1 block" />
          </div>
          <div>
            <label class="text-[11px] uppercase tracking-wider text-outline font-bold" for="sundayEndTime">Hasta</label>
            <form:input path="sundayEndTime" id="sundayEndTime" type="time" cssClass="mt-1 w-full px-3 py-2 bg-surface-container-lowest rounded-lg border border-outline-variant/30 text-on-surface" />
            <form:errors path="sundayEndTime" cssClass="text-error text-xs mt-1 block" />
          </div>
        </div>
      </div>
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

  <script>
    (function () {
      var rows = document.querySelectorAll('[data-availability-row]');
      rows.forEach(function (row) {
        var toggle = row.querySelector('[data-day-toggle="true"]');
        if (!toggle) {
          return;
        }

        var inputs = row.querySelectorAll('input[type="time"]');
        function syncRowState() {
          var enabled = toggle.checked;
          inputs.forEach(function (input) {
            input.disabled = !enabled;
          });
          row.classList.toggle('opacity-60', !enabled);
        }

        toggle.addEventListener('change', syncRowState);
        syncRowState();
      });
    })();
  </script>
</paw:layout>
