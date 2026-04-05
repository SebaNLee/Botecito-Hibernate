<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %> <%@ taglib
prefix="paw" tagdir="/WEB-INF/tags" %> <%@ page contentType="text/html;
charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="marketplaceUrl" value="/marketplace" />

<paw:layout
  title="Botecito"
  mainClass="relative min-h-screen flex flex-col pt-20"
>
  <section
    class="relative flex-grow flex items-center justify-center px-6 py-20 min-h-[870px]"
  >
    <div class="absolute inset-0 z-0 overflow-hidden">
      <img
        class="w-full h-full object-cover"
        alt="serene blue alpine lake surrounded by pine forests and mountains with a wooden pier and nautical equipment in the foreground, soft morning light"
        src="https://lh3.googleusercontent.com/aida-public/AB6AXuDZ5Kqd4fIKaFZvY-voq31ImCLWMzSnpeVGHpeEyaXd8kXtZiSFMnnKueB6HamkvchzGHcdtKloU7aDgr0MGxnoT_9ajOLMji0wPSC7yQ5emqwmkA73GZQCbn35o0UxfJBxyeJdhr-pzL1EEkw8LCF2DcdIJfVaWRcu-RoozLWsJ5ORG87g28BMPR4QUAoyt2LvbzVgTrE-Am1XsXBjgD1E9SEkR7OkE9HuWK0S0ggwcliJWi00H0b6vl-7cuPGDQ0UDBDUBgelxxN5"
      />
      <div
        class="absolute inset-0 bg-gradient-to-b from-on-background/40 via-on-background/10 to-background"
      ></div>
    </div>
    <div class="relative z-10 w-full max-w-6xl mx-auto text-center">
      <h1
        class="font-headline font-extrabold text-4xl md:text-6xl text-white mb-6 tracking-tight drop-shadow-sm"
      >
        Encontrá un kayak cerca de tu casa<br class="hidden md:block" /> y
        navegá esta tarde
      </h1>
      <p
        class="font-body text-white/90 text-lg md:text-xl mb-12 max-w-2xl mx-auto"
      >
        Accedé a embarcaciones de vecinos en tu barrio y disfrutá del lago
        cuando quieras.
      </p>

      <form
        action="${marketplaceUrl}"
        method="get"
        data-filter-form="landing"
        class="bg-surface-container-lowest p-2 md:p-3 rounded-2xl md:rounded-full shadow-[0_32px_48px_rgba(11,28,50,0.12)] max-w-5xl mx-auto flex flex-col md:flex-row items-center gap-2"
      >
        <div class="w-full min-w-0 px-3 py-3 group md:flex-[1.05]">
          <paw:locationPicker
            id="landing-location"
            name="location"
            label="Ubicacion"
            placeholder="A donde vas?"
            icon="location_on"
            variant="inline"
          />
        </div>
        <div class="hidden md:block w-px h-8 bg-outline-variant/20"></div>

        <div class="w-full min-w-0 px-3 py-3 md:flex-[0.8]">
          <paw:datePicker
            id="landing-date"
            name="date"
            label="Fecha"
            placeholder="Selecciona fecha"
            restrictToAvailability="false"
            offeredDatesJson="[]"
            occupiedDatesJson="[]"
          />
        </div>
        <div class="hidden md:block w-px h-8 bg-outline-variant/20"></div>

        <div class="w-full min-w-0 px-3 py-3 md:flex-[0.8]">
          <paw:timeRangePicker
            id="landing-time-range"
            dateInputId="landing-date"
            startName="startTime"
            endName="endTime"
            label="Horario"
            placeholder="Inicio - Fin"
            restrictToAvailability="false"
            offeredTimesJson="{}"
            occupiedTimesJson="{}"
          />
        </div>
        <div class="hidden md:block w-px h-8 bg-outline-variant/20"></div>

        <div class="w-full min-w-0 px-3 py-3 md:flex-[0.98]">
          <paw:peopleCount
            id="landing-capacity"
            name="capacity"
            label="Personas"
            icon="groups"
            placeholder="Cuantos?"
            allowEmpty="true"
            min="1"
            max="20"
            variant="inline"
          />
        </div>

        <button
          type="submit"
          class="w-full md:w-auto md:ml-4 md:shrink-0 bg-[#005da7] hover:bg-[#0076d1] text-white font-bold px-10 py-4 rounded-full transition-all active:scale-95 shadow-lg shadow-primary/20 flex items-center justify-center gap-2 border-none cursor-pointer"
        >
          <span class="material-symbols-outlined">search</span>
          <span>Buscar</span>
        </button>
      </form>
    </div>
  </section>
</paw:layout>
