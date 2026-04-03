<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="marketplaceUrl" value="/marketplace" />
<c:url var="publishUrl" value="/publish" />

<paw:layout title="Publicar Bote - Botecito" mainClass="pt-24 pb-12 max-w-4xl mx-auto px-6">
  <div class="mb-8">
    <a href="${marketplaceUrl}" class="flex items-center gap-2 text-primary hover:opacity-80 transition-opacity font-bold font-manrope bg-transparent no-underline w-fit">
      <span class="material-symbols-outlined">arrow_back</span>
      <span>Volver</span>
    </a>
  </div>
  
  <div class="mb-10">
    <div class="text-sm font-bold text-outline uppercase tracking-wider mb-2">Paso 1 de 3</div>
    <h1 class="text-4xl font-extrabold tracking-tight text-on-background m-0">Detalles basicos</h1>
    <p class="text-on-surface-variant mt-2 text-lg m-0">Cuentanos sobre tu embarcacion para empezar.</p>
  </div>
  
  <c:if test="${submitted}">
    <div class="bg-green-50 border border-green-200 text-green-800 rounded-xl p-4 mb-8 text-sm font-medium">
      Guardamos tu borrador visual de publicacion para que sigas con el siguiente paso.
    </div>
  </c:if>
  
  <form:form action="${publishUrl}" method="post" modelAttribute="publishForm" class="space-y-8">
    <section class="bg-surface-container-lowest rounded-2xl p-8 shadow-[0_32px_48px_rgba(11,28,50,0.04)]">
      <div class="flex items-center gap-3 mb-6 border-b border-outline-variant/15 pb-4">
        <span class="material-symbols-outlined text-primary text-2xl">edit_note</span>
        <h2 class="text-xl font-extrabold m-0">Identidad del Bote</h2>
      </div>
      <div class="space-y-6">
        <div class="space-y-2">
          <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="title">Titulo del anuncio</label>
          <form:input path="title" id="title" class="w-full px-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface placeholder:text-outline" placeholder="Ej. Lancha deportiva Sea Ray 2023"/>
          <form:errors path="title" cssClass="text-error text-xs mt-1 block" />
        </div>
        <div class="space-y-2">
          <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="description">Descripcion</label>
          <form:textarea path="description" id="description" rows="4" class="w-full px-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface placeholder:text-outline resize-none" placeholder="Describe el estado, equipamiento y por que es perfecto para una aventura..."/>
          <form:errors path="description" cssClass="text-error text-xs mt-1 block" />
        </div>
      </div>
    </section>
    
    <section class="bg-surface-container-lowest rounded-2xl p-8 shadow-[0_32px_48px_rgba(11,28,50,0.04)]">
      <div class="flex items-center gap-3 mb-6 border-b border-outline-variant/15 pb-4">
        <span class="material-symbols-outlined text-primary text-2xl">add_a_photo</span>
        <h2 class="text-xl font-extrabold m-0">Foto principal</h2>
      </div>
      <div class="border-2 border-dashed border-outline-variant rounded-xl p-12 flex flex-col items-center justify-center text-center hover:bg-surface-container-high/50 transition-colors cursor-pointer group">
        <div class="w-16 h-16 bg-primary/10 text-primary rounded-full flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
          <span class="material-symbols-outlined text-3xl">upload_file</span>
        </div>
        <span class="font-bold text-lg text-primary">Subir imagen</span>
        <span class="text-sm text-outline mt-1">JPG, PNG hasta 10MB</span>
      </div>
    </section>
    
    <section class="bg-surface-container-lowest rounded-2xl p-8 shadow-[0_32px_48px_rgba(11,28,50,0.04)]">
      <div class="flex items-center gap-3 mb-6 border-b border-outline-variant/15 pb-4">
        <span class="material-symbols-outlined text-primary text-2xl">location_on</span>
        <h2 class="text-xl font-extrabold m-0">Ubicacion</h2>
      </div>
      <div class="space-y-2">
        <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="marina">Nombre del Puerto/Marina</label>
        <div class="relative">
          <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline">search</span>
          <form:input path="marina" id="marina" class="w-full pl-12 pr-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface placeholder:text-outline" placeholder="Ej. Puerto Madero, Buenos Aires"/>
        </div>
        <form:errors path="marina" cssClass="text-error text-xs mt-1 block" />
      </div>
    </section>
    
    <section class="bg-surface-container-lowest rounded-2xl p-8 shadow-[0_32px_48px_rgba(11,28,50,0.04)]">
      <div class="flex items-center gap-3 mb-6 border-b border-outline-variant/15 pb-4">
        <span class="material-symbols-outlined text-primary text-2xl">settings_input_component</span>
        <h2 class="text-xl font-extrabold m-0">Especificaciones</h2>
      </div>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div class="space-y-2">
          <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="capacity">Capacidad (Personas)</label>
          <div class="flex items-center justify-between p-2 bg-surface-container-high rounded-xl">
            <button type="button" class="w-10 h-10 flex items-center justify-center rounded-lg bg-surface-container-lowest text-primary shadow-sm active:scale-90 transition-transform border-none cursor-pointer">
              <span class="material-symbols-outlined">remove</span>
            </button>
            <form:select path="capacity" id="capacity" class="bg-transparent border-none font-bold text-center appearance-none focus:ring-0 p-0 w-20">
              <form:option value="2" label="2"/>
              <form:option value="4" label="4"/>
              <form:option value="6" label="6"/>
              <form:option value="8" label="8"/>
              <form:option value="10" label="10"/>
              <form:option value="12" label="12"/>
            </form:select>
            <button type="button" class="w-10 h-10 flex items-center justify-center rounded-lg bg-surface-container-lowest text-primary shadow-sm active:scale-90 transition-transform border-none cursor-pointer">
              <span class="material-symbols-outlined">add</span>
            </button>
          </div>
          <form:errors path="capacity" cssClass="text-error text-xs mt-1 block" />
        </div>
        <div class="space-y-2">
          <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="maxWeight">Peso Maximo (kg)</label>
          <div class="relative">
            <form:input path="maxWeight" id="maxWeight" type="number" class="w-full px-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface font-bold" placeholder="Ej. 850"/>
            <span class="absolute right-4 top-1/2 -translate-y-1/2 text-outline font-bold text-sm">KG</span>
          </div>
          <form:errors path="maxWeight" cssClass="text-error text-xs mt-1 block" />
        </div>
      </div>
    </section>
    
    <div class="flex flex-col sm:flex-row justify-between items-center gap-4 pt-6">
      <button type="button" class="w-full sm:w-auto px-8 py-4 bg-surface-container-high text-primary rounded-xl font-bold hover:bg-surface-container transition-colors border-none cursor-pointer">
        Guardar borrador
      </button>
      <button type="submit" class="w-full sm:w-auto px-10 py-4 bg-primary text-on-primary rounded-xl font-bold shadow-lg shadow-primary/20 hover:bg-primary-container transition-all active:scale-[0.98] flex items-center justify-center gap-2 border-none cursor-pointer">
        Siguiente
        <span class="material-symbols-outlined text-sm">arrow_forward</span>
      </button>
    </div>
  </form:form>
</paw:layout>
