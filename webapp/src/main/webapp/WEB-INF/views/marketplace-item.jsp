<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="publishUrl" value="/publish" />
<c:url var="marketplaceUrl" value="/marketplace" />
<c:url var="reservationRequestUrl" value="/item/${item.id}" />

<paw:layout title="Botecito | Detalle" mainClass="pt-24 pb-12 max-w-7xl mx-auto px-6 flex flex-col gap-8">
  <div class="w-full">
    <a href="${marketplaceUrl}" class="flex items-center gap-2 text-primary hover:opacity-80 transition-opacity font-bold font-manrope mb-6 bg-transparent no-underline w-fit">
      <span class="material-symbols-outlined">arrow_back</span>
      <span>Volver</span>
    </a>
  </div>
  
  <div class="flex flex-col lg:flex-row gap-8">
    <section class="flex-1 space-y-8">
      <div class="bg-surface-container-lowest rounded-2xl overflow-hidden shadow-[0_32px_48px_rgba(11,28,50,0.04)]">
        <img class="w-full h-[400px] object-cover" alt="${item.title}" src="${itemImageUrl}"/>
      </div>
      
      <div class="bg-surface-container-lowest rounded-2xl p-8 shadow-[0_32px_48px_rgba(11,28,50,0.04)]">
        <h2 class="text-2xl font-extrabold tracking-tight mb-4 m-0">Descripcion del Item</h2>
        <div class="space-y-4 text-on-surface-variant leading-relaxed">
          <p class="m-0"><c:out value="${item.description}" /></p>
        </div>
        
        <h3 class="text-xl font-extrabold tracking-tight mt-10 mb-6 m-0">Especificaciones Tecnicas</h3>
        <ul class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <li class="flex items-center gap-3 text-on-surface-variant">
            <span class="material-symbols-outlined text-primary">check_circle</span>
            <c:out value="${itemType != null ? itemType.name : 'Sin tipo'}" />
          </li>
          <li class="flex items-center gap-3 text-on-surface-variant">
            <span class="material-symbols-outlined text-primary">check_circle</span>
            Capacidad: <c:out value="${item.capacityPeople}" /> personas
          </li>
          <li class="flex items-center gap-3 text-on-surface-variant">
            <span class="material-symbols-outlined text-primary">check_circle</span>
            Peso maximo: <c:out value="${item.maxWeightKg}" /> kg
          </li>
          <li class="flex items-center gap-3 text-on-surface-variant">
            <span class="material-symbols-outlined text-primary">check_circle</span>
            Dificultad: <c:out value="${item.difficultyLevel}" />/5
          </li>
        </ul>
      </div>
    </section>
    
    <aside class="w-full lg:w-[400px] space-y-6">
      <div class="bg-surface-container-lowest rounded-2xl p-8 shadow-[0_32px_48px_rgba(11,28,50,0.04)]">
        <span class="inline-block px-3 py-1 bg-surface-container-high text-primary text-xs font-bold uppercase tracking-wider rounded-full mb-4">
          <c:out value="${difficultyLabel}" />
        </span>
        <h1 class="text-3xl font-extrabold tracking-tight mb-2 m-0"><c:out value="${item.title}" /></h1>
        <div class="flex items-center text-on-surface-variant text-sm gap-1 mb-6">
          <span class="material-symbols-outlined text-primary text-lg">location_on</span>
          <span><c:out value="${item.location}" /></span>
        </div>
        <div class="flex items-baseline gap-2 mb-8">
          <span class="text-4xl font-black text-primary">$<c:out value="${item.pricePerHour}" /></span>
          <span class="text-xs font-bold uppercase tracking-wider text-outline">por hora</span>
        </div>
        
        <form:form action="${reservationRequestUrl}" method="post" modelAttribute="reservationRequestForm" class="space-y-4">
          <c:if test="${not empty mailSuccess}">
            <div class="rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-700">
              <c:out value="${mailSuccess}" />
            </div>
          </c:if>
          <c:if test="${not empty mailError}">
            <div class="rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700">
              <c:out value="${mailError}" />
            </div>
          </c:if>
          <form:errors path="*" element="div" cssClass="rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700" />
          <paw:datePicker
              id="reservation-date"
              name="date"
              label="Fecha"
              value="${reservationRequestForm.date}"
              placeholder="Selecciona fecha"
              offeredDatesJson="${reservationOfferedDatesJson}"
              occupiedDatesJson="${reservationOccupiedDatesJson}" />
          <form:errors path="date" element="div" cssClass="text-sm text-error" />
          <paw:timeRangePicker
              id="reservation-time-range"
              dateInputId="reservation-date"
              startName="startTime"
              endName="endTime"
              label="Horario"
              startValue="${reservationRequestForm.startTime}"
              endValue="${reservationRequestForm.endTime}"
              placeholder="Inicio - Fin"
              offeredTimesJson="${reservationOfferedTimesJson}"
              occupiedTimesJson="${reservationOccupiedTimesJson}" />
          <form:errors path="startTime" element="div" cssClass="text-sm text-error" />
          <form:errors path="endTime" element="div" cssClass="text-sm text-error" />
          <div class="space-y-2">
            <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="requesterName">Nombre</label>
            <form:input path="requesterName" id="requesterName" class="w-full px-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface placeholder:text-outline" placeholder="Tu nombre" />
            <form:errors path="requesterName" element="div" cssClass="text-sm text-error" />
          </div>
          <div class="space-y-2">
            <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="requesterEmail">Email</label>
            <form:input path="requesterEmail" id="requesterEmail" type="email" class="w-full px-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface placeholder:text-outline" placeholder="tu@email.com" />
            <form:errors path="requesterEmail" element="div" cssClass="text-sm text-error" />
          </div>
          <div class="space-y-2">
            <label class="text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="requestMessage">Mensaje</label>
            <form:textarea path="requestMessage" id="requestMessage" rows="4" class="w-full px-4 py-3 bg-surface-container-high border-none rounded-xl focus:ring-2 focus:ring-primary/20 text-on-surface placeholder:text-outline resize-none" placeholder="Agrega un mensaje para tu solicitud" />
            <form:errors path="requestMessage" element="div" cssClass="text-sm text-error" />
          </div>
          <button type="submit" class="w-full py-4 bg-primary text-on-primary rounded-xl font-bold shadow-lg shadow-primary/20 hover:bg-primary-container transition-all active:scale-[0.98] mt-4 flex justify-center items-center gap-2 border-none cursor-pointer">
            Realizar pre-reserva
            <span class="material-symbols-outlined text-sm">chevron_right</span>
          </button>
        </form:form>
      </div>
      
      <div class="grid grid-cols-2 gap-4">
        <div class="bg-surface-container-lowest rounded-2xl p-6 shadow-[0_32px_48px_rgba(11,28,50,0.04)] flex flex-col items-center justify-center text-center gap-2">
          <span class="material-symbols-outlined text-outline text-3xl">groups</span>
          <div>
            <div class="text-[10px] font-bold uppercase tracking-wider text-outline">Capacidad</div>
            <div class="font-extrabold text-lg">${item.capacityPeople} Personas</div>
          </div>
        </div>
        <div class="bg-surface-container-lowest rounded-2xl p-6 shadow-[0_32px_48px_rgba(11,28,50,0.04)] flex flex-col items-center justify-center text-center gap-2">
          <span class="material-symbols-outlined text-outline text-3xl">weight</span>
          <div>
            <div class="text-[10px] font-bold uppercase tracking-wider text-outline">Peso Max.</div>
            <div class="font-extrabold text-lg">${item.maxWeightKg} kg</div>
          </div>
        </div>
      </div>
      
      <div class="bg-surface-container-lowest rounded-2xl p-8 shadow-[0_32px_48px_rgba(11,28,50,0.04)]">
        <div class="flex items-center gap-4 mb-6">
          <div class="w-14 h-14 rounded-full bg-primary/10 text-primary flex items-center justify-center font-extrabold text-xl">
            <c:out value="${ownerInitial}" />
          </div>
          <div>
            <h3 class="font-extrabold text-lg m-0"><c:out value="${itemOwner != null ? itemOwner.name : 'Sin propietario'}" /></h3>
            <div class="text-xs text-on-surface-variant"><c:out value="${itemOwner != null ? itemOwner.email : 'Sin email'}" /></div>
            <div class="flex items-center gap-1 text-xs font-bold text-green-600 mt-1">
              <span class="material-symbols-outlined text-[14px]">verified</span>
              Propietario activo
            </div>
          </div>
        </div>
        <div class="space-y-3">
          <button type="button" class="w-full py-3 bg-surface-container-high text-primary rounded-xl font-bold hover:bg-surface-container transition-colors border-none cursor-pointer">
            Contactar Anfitrion
          </button>
          <a href="tel:${itemOwner != null ? itemOwner.phone : ''}" class="w-full py-3 bg-surface-container-high text-primary rounded-xl font-bold hover:bg-surface-container transition-colors flex justify-center items-center gap-2 no-underline">
            <span class="material-symbols-outlined text-sm">call</span>
            Llamar ahora
          </a>
          <div class="grid grid-cols-2 gap-3">
            <a href="https://wa.me/${itemOwner != null ? itemOwner.phone : ''}" class="py-3 bg-[#25D366]/10 text-[#25D366] rounded-xl font-bold hover:bg-[#25D366]/20 transition-colors flex justify-center items-center gap-2 no-underline">
              <span class="material-symbols-outlined text-sm">chat</span>
              WhatsApp
            </a>
            <a href="mailto:${itemOwner != null ? itemOwner.email : ''}" class="py-3 bg-surface-container-high text-primary rounded-xl font-bold hover:bg-surface-container transition-colors flex justify-center items-center gap-2 no-underline">
              <span class="material-symbols-outlined text-sm">mail</span>
              Email
            </a>
          </div>
        </div>
        <p class="text-[10px] text-outline text-center mt-6 leading-relaxed">
          Al contactar, aceptas nuestros Terminos. Botecito es curador de la experiencia.
        </p>
      </div>
    </aside>
  </div>

  <div
    class="fixed inset-0 z-[280] hidden items-center justify-center bg-on-background/45 px-6"
    data-item-unavailable-alert
    data-marketplace-url="${marketplaceUrl}"
    data-item-location="${item.location}"
    data-item-capacity="${item.capacityPeople}"
    data-item-max-weight="${item.maxWeightKg}"
    hidden
  >
    <div
      class="w-full max-w-lg rounded-3xl bg-surface-container-lowest p-8 shadow-[0_32px_64px_rgba(11,28,50,0.18)]"
    >
      <div class="flex items-start gap-4">
        <div
          class="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-error-container text-error"
        >
          <span class="material-symbols-outlined">warning</span>
        </div>
        <div class="space-y-3">
          <h2 class="m-0 text-2xl font-extrabold tracking-tight">
            Item no disponible
          </h2>
          <p class="m-0 leading-relaxed text-on-surface-variant" data-item-unavailable-message>
            El item seleccionado no esta disponible en la fecha y horario
            elegidos. Puedes seguir viendo este item sin esos filtros o volver
            al marketplace para elegir otro.
          </p>
        </div>
      </div>

      <div class="mt-8 flex flex-col gap-3 sm:flex-row">
        <button
          type="button"
          class="flex-1 rounded-xl border-none bg-primary px-5 py-3 font-bold text-on-primary cursor-pointer"
          data-item-unavailable-clear
        >
          Seguir sin filtros
        </button>
        <button
          type="button"
          class="flex-1 rounded-xl border border-outline-variant bg-transparent px-5 py-3 font-bold text-on-surface cursor-pointer"
          data-item-unavailable-marketplace
        >
          Volver al marketplace
        </button>
      </div>
    </div>
  </div>
</paw:layout>
