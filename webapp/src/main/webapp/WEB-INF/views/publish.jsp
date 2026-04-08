<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="marketplaceUrl" value="/marketplace" />
<c:url var="publishUrl" value="/publish" />

<paw:layout title="Publicar Bote - Botecito" mainClass="pt-24 pb-14 max-w-6xl mx-auto px-6">
  <div class="mb-8">
    <a href="${marketplaceUrl}" class="flex items-center gap-2 text-primary hover:opacity-80 transition-opacity font-bold font-manrope bg-transparent no-underline w-fit">
      <span class="material-symbols-outlined">arrow_back</span>
      <span>Volver</span>
    </a>
  </div>

  <div class="mb-10">
    <div class="flex items-center justify-between gap-4 text-sm font-bold text-outline uppercase tracking-wider">
      <span>Paso 1 de 3</span>
      <span>Informacion del item</span>
    </div>
    <div class="mt-3 h-2 w-full rounded-full bg-surface-container-high overflow-hidden">
      <div class="h-full w-1/3 rounded-full bg-primary"></div>
    </div>
    <h1 class="mt-6 text-4xl font-extrabold tracking-tight text-on-background m-0">Detalles de tu embarcacion</h1>
    <p class="text-on-surface-variant mt-2 text-lg m-0">Completa la informacion principal para que los navegantes entiendan tu propuesta.</p>
  </div>

  <c:if test="${submitted}">
    <div class="bg-green-50 border border-green-200 text-green-800 rounded-xl p-4 mb-8">
      <p class="m-0 text-sm font-medium">Publicacion guardada correctamente. En breve te llegara un correo de confirmacion al email cargado con un enlace de eliminacion.</p>
      <div class="mt-3 flex flex-wrap gap-3">
        <a href="${publishUrl}" class="inline-flex items-center rounded-lg bg-green-700 px-4 py-2 text-sm font-bold text-white no-underline hover:bg-green-800 transition-colors">
          Crear otra publicacion
        </a>
        <a href="${marketplaceUrl}" class="inline-flex items-center rounded-lg border border-green-300 px-4 py-2 text-sm font-bold text-green-800 no-underline hover:bg-green-100 transition-colors">
          Ir al marketplace
        </a>
      </div>
    </div>
  </c:if>

  <form:form action="${publishUrl}" method="post" modelAttribute="publishForm" enctype="multipart/form-data" class="space-y-8">
    <div class="grid grid-cols-1 lg:grid-cols-5 gap-8 items-start">
      <section class="lg:col-span-3 bg-surface-container-lowest rounded-2xl p-8 shadow-[0_32px_48px_rgba(11,28,50,0.04)] space-y-6">
        <div class="flex items-center gap-3 pb-4 border-b border-outline-variant/20">
          <span class="material-symbols-outlined text-primary text-2xl">directions_boat</span>
          <h2 class="text-xl font-extrabold m-0">Informacion base</h2>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div class="space-y-2 md:col-span-2">
            <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="title">Titulo del anuncio</label>
            <form:input path="title" id="title" class="w-full px-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface placeholder:text-outline" placeholder="Ej. Beneteau Oceanis 45 listo para travesias"/>
            <form:errors path="title" cssClass="mt-1 block rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs font-medium text-red-700" />
          </div>

          <div class="space-y-2 md:col-span-2">
            <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="description">Descripcion</label>
            <form:textarea path="description" id="description" rows="5" class="w-full px-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface placeholder:text-outline resize-none" placeholder="Cuenta comodidades, equipamiento, estado general y para que plan es ideal..."/>
            <form:errors path="description" cssClass="mt-1 block rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs font-medium text-red-700" />
          </div>

          <div class="space-y-2">
            <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="itemTypeId">Tipo</label>
            <form:select path="itemTypeId" id="itemTypeId" class="w-full px-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface">
              <form:option value="" label="Selecciona tipo" />
              <form:options items="${itemTypeOptions}" />
            </form:select>
            <form:errors path="itemTypeId" cssClass="mt-1 block rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs font-medium text-red-700" />
          </div>

          <div class="space-y-2">
            <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="pricePerHour">Precio por hora (ARS)</label>
            <form:input path="pricePerHour" id="pricePerHour" type="number" min="0" class="w-full px-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface" placeholder="Ej. 3200"/>
            <form:errors path="pricePerHour" cssClass="mt-1 block rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs font-medium text-red-700" />
          </div>

          <div class="md:col-span-2">
            <paw:locationPicker
                id="marina"
                name="marina"
                label="Ubicacion"
                value="${publishForm.marina}"
                placeholder="Ej. Puerto Madero, Buenos Aires"
                icon="location_on"
                errorPath="marina" />
          </div>

          <div>
            <paw:peopleCount
                id="capacity"
                name="capacity"
                label="Capacidad (Personas)"
                value="${empty publishForm.capacity ? '2' : publishForm.capacity}"
                min="1"
                max="20"
                errorPath="capacity" />
          </div>

          <div class="space-y-2">
            <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="maxWeight">Peso maximo (kg)</label>
            <div class="relative">
              <form:input path="maxWeight" id="maxWeight" type="number" min="0" class="w-full px-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface" placeholder="Opcional"/>
              <span class="absolute right-4 top-1/2 -translate-y-1/2 text-outline text-sm font-bold">KG</span>
            </div>
            <form:errors path="maxWeight" cssClass="mt-1 block rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs font-medium text-red-700" />
          </div>

          <div class="space-y-2 md:col-span-2">
            <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="difficultyLevel">Nivel de dificultad</label>
            <form:select path="difficultyLevel" id="difficultyLevel" class="w-full px-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface">
              <form:option value="" label="Selecciona nivel" />
              <form:options items="${difficultyOptions}" />
            </form:select>
            <form:errors path="difficultyLevel" cssClass="mt-1 block rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs font-medium text-red-700" />
          </div>
        </div>
      </section>

      <aside class="lg:col-span-2 bg-surface-container-lowest rounded-2xl p-8 shadow-[0_32px_48px_rgba(11,28,50,0.04)] space-y-6">
        <div class="flex items-center gap-3 pb-4 border-b border-outline-variant/20">
          <span class="material-symbols-outlined text-primary text-2xl">add_a_photo</span>
          <h2 class="text-xl font-extrabold m-0">Foto principal</h2>
        </div>

        <label class="border-2 border-dashed border-outline-variant rounded-xl p-10 flex flex-col items-center justify-center text-center hover:bg-surface-container-high/50 transition-colors cursor-pointer group">
          <input type="file" name="file" accept="image/*" class="hidden" onchange="document.getElementById('file-name-display').textContent = this.files[0] ? this.files[0].name : 'JPG, PNG hasta 5MB'"/>
          <div class="w-16 h-16 bg-primary/10 text-primary rounded-full flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
            <span class="material-symbols-outlined text-3xl">upload_file</span>
          </div>
          <span class="font-bold text-lg text-primary">Subir imagen</span>
          <span id="file-name-display" class="text-sm text-outline mt-1">JPG, PNG hasta 5MB</span>
          <form:errors path="file" cssClass="mt-2 block rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-xs font-medium text-red-700" />
        </label>

        <div class="rounded-xl bg-surface-container-high p-4 text-sm text-on-surface-variant leading-relaxed">
          Esta imagen se usa como portada del anuncio. Puedes cambiarla en cualquier paso antes de publicar.
        </div>
      </aside>
    </div>

    <div class="flex flex-col sm:flex-row justify-between items-center gap-4 pt-2">
      <a href="${marketplaceUrl}" class="w-full sm:w-auto px-8 py-4 bg-surface-container-high text-primary rounded-xl font-bold hover:bg-surface-container transition-colors no-underline text-center">
        Guardar borrador
      </a>
      <button type="submit" class="w-full sm:w-auto px-10 py-4 bg-secondary text-white rounded-xl font-bold shadow-[0_16px_32px_rgba(174,49,35,0.24)] hover:bg-[#962619] transition-all active:scale-[0.98] flex items-center justify-center gap-2 border-none cursor-pointer">
        Continuar a disponibilidad
        <span class="material-symbols-outlined text-sm">arrow_forward</span>
      </button>
    </div>
  </form:form>
</paw:layout>
