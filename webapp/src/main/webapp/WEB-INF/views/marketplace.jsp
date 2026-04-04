<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="publishUrl" value="/publish" />
<c:url var="homeUrl" value="/" />
<c:url var="clearMarketplaceFiltersUrl" value="/marketplace">
  <c:if test="${sort != 'recommended'}">
    <c:param name="sort" value="${sort}" />
  </c:if>
</c:url>

<paw:layout title="Explorar - Botecito" mainClass="pt-24 pb-12 max-w-7xl mx-auto px-6 grid grid-cols-1 md:grid-cols-[18rem_minmax(0,1fr)] lg:grid-cols-[20rem_minmax(0,1fr)] gap-8 items-start">
  <aside class="relative z-40 w-full md:min-w-0">
    <div class="space-y-8">
      <a href="${homeUrl}" class="flex items-center gap-2 text-primary hover:opacity-80 transition-opacity font-bold font-manrope mb-4 bg-transparent no-underline">
        <span class="material-symbols-outlined">arrow_back</span>
        <span>Volver</span>
      </a>
      
      <div class="rounded-2xl bg-surface-container-lowest p-6 shadow-[0_24px_40px_rgba(11,28,50,0.06)]">
        <h2 class="text-xl font-extrabold mb-6 tracking-tight">Filtros de busqueda</h2>
        <form action="<c:url value='/marketplace' />" method="get" class="space-y-6" data-filter-form="marketplace">
          <paw:locationPicker
              id="marketplace-location"
              name="location"
              label="Ubicacion"
              value="${param.location}"
              placeholder="A donde vas?"
              icon="location_on" />
          
          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-2">
              <paw:datePicker
                  id="marketplace-date"
                  name="date"
                  label="Fecha"
                  value="${param.date}"
                  placeholder="Selecciona fecha"
                  restrictToAvailability="false"
                  offeredDatesJson="[]"
                  occupiedDatesJson="[]" />
            </div>
            <div class="space-y-2">
              <paw:timeRangePicker
                  id="marketplace-time-range"
                  dateInputId="marketplace-date"
                  startName="startTime"
                  endName="endTime"
                  label="Horario"
                  startValue="${param.startTime}"
                  endValue="${param.endTime}"
                  placeholder="Inicio - Fin"
                  restrictToAvailability="false"
                  offeredTimesJson="{}"
                  occupiedTimesJson="{}" />
            </div>
          </div>
          
          <paw:peopleCount
              id="marketplace-capacity"
              name="capacity"
              label="Personas"
              value="${param.capacity}"
              allowEmpty="true"
              placeholder="Cualquiera"
              min="1"
              max="20" />
          
          <div class="pt-4 border-t border-outline-variant/15">
            <paw:weightCapacitySlider
                id="marketplace-max-weight"
                name="maxWeight"
                label="Peso a soportar"
                value="${param.maxWeight}"
                min="100"
                max="2000"
                step="50"
                helper="Mostramos botes que soportan este peso o mas." />
          </div>
          
          <div class="flex flex-col gap-3">
            <button type="submit" class="w-full py-4 bg-primary text-on-primary rounded-xl font-bold shadow-lg shadow-primary/20 hover:bg-primary-container transition-all active:scale-[0.98] border-none cursor-pointer">
              Actualizar resultados
            </button>
            <a
                href="${clearMarketplaceFiltersUrl}"
                class="inline-flex w-full items-center justify-center rounded-xl border border-outline-variant/30 bg-transparent px-4 py-3 text-sm font-bold text-on-surface no-underline transition-colors hover:bg-surface-container-high"
                data-clear-marketplace-filters>
              Limpiar filtros
            </a>
          </div>
        </form>
      </div>
    </div>
  </aside>
  
  <section class="relative z-0 min-w-0">
    <div class="flex flex-col md:flex-row md:items-end justify-between mb-8 gap-4">
      <div>
        <h1 class="text-4xl font-extrabold tracking-tight text-on-background m-0">Explorar items</h1>
        <p class="text-on-surface-variant mt-2 m-0">${itemsCount} items disponibles cerca de ti</p>
      </div>
      
      <form action="<c:url value='/marketplace' />" method="get" class="flex items-center gap-2 text-sm font-medium text-on-surface-variant">
        <input type="hidden" name="location" value="${param.location}" data-applied-filter-mirror />
        <input type="hidden" name="date" value="${param.date}" data-applied-filter-mirror />
        <input type="hidden" name="startTime" value="${param.startTime}" data-applied-filter-mirror />
        <input type="hidden" name="endTime" value="${param.endTime}" data-applied-filter-mirror />
        <input type="hidden" name="capacity" value="${param.capacity}" data-applied-filter-mirror />
        <input type="hidden" name="maxWeight" value="${param.maxWeight}" data-applied-filter-mirror />
        <label for="marketplace-sort">Ordenar por:</label>
        <div class="relative flex items-center">
          <select id="marketplace-sort" name="sort" class="appearance-none bg-transparent border-none font-bold text-primary pr-6 focus:ring-0 cursor-pointer" onchange="this.form.submit()">
            <option value="recommended" ${sort == 'recommended' ? 'selected="selected"' : ''}>Recomendados</option>
            <option value="price" ${sort == 'price' ? 'selected="selected"' : ''}>Precio</option>
            <option value="capacity" ${sort == 'capacity' ? 'selected="selected"' : ''}>Capacidad</option>
          </select>
          <span class="material-symbols-outlined text-sm text-primary absolute right-0 pointer-events-none">expand_more</span>
        </div>
      </form>
    </div>
    
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
      <c:forEach items="${items}" var="item">
        <c:url var="itemUrl" value="/item/${item.id}">
          <c:if test="${not empty param.date}">
            <c:param name="date" value="${param.date}" />
          </c:if>
          <c:if test="${not empty param.startTime}">
            <c:param name="startTime" value="${param.startTime}" />
          </c:if>
          <c:if test="${not empty param.endTime}">
            <c:param name="endTime" value="${param.endTime}" />
          </c:if>
        </c:url>
        <a href="${itemUrl}" data-marketplace-item-link class="group relative z-0 bg-surface-container-lowest rounded-xl overflow-hidden shadow-[0_32px_48px_rgba(11,28,50,0.04)] transition-all hover:shadow-[0_40px_64px_rgba(11,28,50,0.08)] no-underline block">
          <div class="aspect-[16/10] overflow-hidden relative">
            <img class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-105" alt="${item.title}" src="${itemImages[item.id]}"/>
          </div>
          <div class="p-6 space-y-4">
            <div class="flex justify-between items-start">
              <h3 class="text-xl font-bold text-on-background leading-tight m-0"><c:out value="${item.title}" /></h3>
              <div class="text-right">
                <span class="block text-2xl font-black text-primary">$<c:out value="${item.pricePerHour}" /></span>
                <span class="text-[10px] font-bold uppercase tracking-tighter text-outline">por hora</span>
              </div>
            </div>
            <div class="flex items-center text-on-surface-variant text-sm gap-1">
              <span class="material-symbols-outlined text-primary text-lg">location_on</span>
              <span><c:out value="${item.location}" /></span>
            </div>
            <div class="pt-4 border-t border-outline-variant/15 flex justify-between items-center">
              <div class="flex items-center gap-4">
                <div class="flex items-center gap-1.5">
                  <span class="material-symbols-outlined text-outline text-lg">groups</span>
                  <span class="text-sm font-semibold"><c:out value="${item.capacityPeople}" /> pers.</span>
                </div>
                <div class="flex items-center gap-1.5">
                  <span class="material-symbols-outlined text-outline text-lg">weight</span>
                  <span class="text-sm font-semibold"><c:out value="${item.maxWeightKg}" /> kg</span>
                </div>
              </div>
              <div class="text-primary font-bold text-sm flex items-center gap-1 group/btn">
                Ver detalles
                <span class="material-symbols-outlined text-sm transition-transform group-hover/btn:translate-x-1">arrow_forward</span>
              </div>
            </div>
          </div>
        </a>
      </c:forEach>
    </div>
  </section>
</paw:layout>
