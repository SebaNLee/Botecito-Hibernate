<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="publishUrl" value="/publish" />
<c:url var="homeUrl" value="/" />

<paw:layout title="Explorar - Botecito" mainClass="pt-24 pb-20 md:pb-12 max-w-7xl mx-auto px-6 flex flex-col md:flex-row gap-8">
  <aside class="w-full md:w-80 shrink-0">
    <div class="sticky top-28 space-y-8">
      <a href="${homeUrl}" class="flex items-center gap-2 text-primary hover:opacity-80 transition-opacity font-bold font-manrope mb-4 bg-transparent no-underline">
        <span class="material-symbols-outlined">arrow_back</span>
        <span>Volver</span>
      </a>
      
      <div>
        <h2 class="text-xl font-extrabold mb-6 tracking-tight">Filtros de busqueda</h2>
        <form action="<c:url value='/marketplace' />" method="get" class="space-y-6">
          <div class="space-y-2">
            <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="marketplace-location">Ubicacion</label>
            <div class="relative">
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-outline">location_on</span>
              <input id="marketplace-location" name="location" value="${param.location}" class="w-full pl-12 pr-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface placeholder:text-outline" placeholder="A donde vas?" type="text"/>
            </div>
          </div>
          
          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-2">
              <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="marketplace-date">Fecha</label>
              <div class="relative">
                <input id="marketplace-date" name="date" value="${param.date}" class="w-full px-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface text-sm" type="date"/>
              </div>
            </div>
            <div class="space-y-2">
              <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="marketplace-time">Horario</label>
              <select id="marketplace-time" name="time" class="w-full px-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface text-sm appearance-none">
                <option value="">Cualquiera</option>
                <option value="morning" ${param.time == 'morning' ? 'selected="selected"' : ''}>Manana</option>
                <option value="afternoon" ${param.time == 'afternoon' ? 'selected="selected"' : ''}>Tarde</option>
                <option value="full" ${param.time == 'full' ? 'selected="selected"' : ''}>Dia completo</option>
              </select>
            </div>
          </div>
          
          <div class="space-y-2">
            <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="marketplace-capacity">Personas</label>
            <div class="flex items-center justify-between p-3 bg-surface-container-high rounded-xl">
              <select id="marketplace-capacity" name="capacity" class="w-full bg-transparent border-none p-0 text-on-surface focus:ring-0 font-bold appearance-none text-center">
                <option value="">Cualquiera</option>
                <option value="2" ${param.capacity == '2' ? 'selected="selected"' : ''}>2 Personas</option>
                <option value="4" ${param.capacity == '4' ? 'selected="selected"' : ''}>4 Personas</option>
                <option value="6" ${param.capacity == '6' ? 'selected="selected"' : ''}>6 Personas</option>
                <option value="8" ${param.capacity == '8' ? 'selected="selected"' : ''}>8 Personas</option>
                <option value="10" ${param.capacity == '10' ? 'selected="selected"' : ''}>10 Personas</option>
                <option value="12" ${param.capacity == '12' ? 'selected="selected"' : ''}>12 Personas</option>
              </select>
            </div>
          </div>
          
          <div class="space-y-4 pt-4 border-t border-outline-variant/15">
            <div class="flex justify-between items-center">
              <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant">Peso Maximo</label>
              <span class="text-sm font-bold text-primary">${empty param.maxWeight ? '850' : param.maxWeight} kg</span>
            </div>
            <input name="maxWeight" class="w-full h-1.5 bg-surface-container-highest rounded-lg appearance-none cursor-pointer accent-primary" max="2000" min="100" step="50" type="range" value="${empty param.maxWeight ? '850' : param.maxWeight}"/>
            <div class="flex justify-between text-[10px] text-outline font-medium">
              <span>100kg</span>
              <span>2000kg</span>
            </div>
          </div>
          
          <button type="submit" class="w-full py-4 bg-primary text-on-primary rounded-xl font-bold shadow-lg shadow-primary/20 hover:bg-primary-container transition-all active:scale-[0.98] border-none cursor-pointer">
            Actualizar resultados
          </button>
        </form>
      </div>
    </div>
  </aside>
  
  <section class="flex-1">
    <div class="flex flex-col md:flex-row md:items-end justify-between mb-8 gap-4">
      <div>
        <h1 class="text-4xl font-extrabold tracking-tight text-on-background m-0">Explorar items</h1>
        <p class="text-on-surface-variant mt-2 m-0">${itemsCount} items disponibles cerca de ti</p>
      </div>
      
      <form action="<c:url value='/marketplace' />" method="get" class="flex items-center gap-2 text-sm font-medium text-on-surface-variant">
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
        <c:url var="itemUrl" value="/item/${item.id}" />
        <a href="${itemUrl}" class="group bg-surface-container-lowest rounded-xl overflow-hidden shadow-[0_32px_48px_rgba(11,28,50,0.04)] transition-all hover:shadow-[0_40px_64px_rgba(11,28,50,0.08)] no-underline block">
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
