<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="marketplaceUrl" value="/marketplace" />

<paw:layout title="Botecito" mainClass="relative min-h-screen flex flex-col pt-20">
  <section class="relative flex-grow flex items-center justify-center px-6 py-20 min-h-[870px]">
    <div class="absolute inset-0 z-0 overflow-hidden">
      <img class="w-full h-full object-cover" alt="serene blue alpine lake surrounded by pine forests and mountains with a wooden pier and nautical equipment in the foreground, soft morning light" src="https://lh3.googleusercontent.com/aida-public/AB6AXuDZ5Kqd4fIKaFZvY-voq31ImCLWMzSnpeVGHpeEyaXd8kXtZiSFMnnKueB6HamkvchzGHcdtKloU7aDgr0MGxnoT_9ajOLMji0wPSC7yQ5emqwmkA73GZQCbn35o0UxfJBxyeJdhr-pzL1EEkw8LCF2DcdIJfVaWRcu-RoozLWsJ5ORG87g28BMPR4QUAoyt2LvbzVgTrE-Am1XsXBjgD1E9SEkR7OkE9HuWK0S0ggwcliJWi00H0b6vl-7cuPGDQ0UDBDUBgelxxN5"/>
      <div class="absolute inset-0 bg-gradient-to-b from-on-background/40 via-on-background/10 to-background"></div>
    </div>
    <div class="relative z-10 w-full max-w-6xl mx-auto text-center">
      <h1 class="font-headline font-extrabold text-4xl md:text-7xl text-white mb-6 tracking-tight drop-shadow-sm">
        Encuentra tu proximo <br class="hidden md:block"/> destino nautico
      </h1>
      <p class="font-body text-white/90 text-lg md:text-xl mb-12 max-w-2xl mx-auto">
        Explora y reserva embarcaciones unicas para tus mejores momentos en el agua.
      </p>
      
      <form action="${marketplaceUrl}" method="get" class="bg-surface-container-lowest p-2 md:p-3 rounded-2xl md:rounded-full shadow-[0_32px_48px_rgba(11,28,50,0.12)] max-w-5xl mx-auto flex flex-col md:flex-row items-center gap-2">
        <div class="flex-1 w-full flex items-center px-4 py-3 group">
          <span class="material-symbols-outlined text-primary mr-3">location_on</span>
          <div class="text-left w-full">
            <label class="block text-[10px] font-bold text-outline uppercase tracking-wider" for="landing-location">Ubicacion</label>
            <input id="landing-location" name="location" class="w-full bg-transparent border-none p-0 text-on-surface focus:ring-0 placeholder:text-outline-variant font-medium" placeholder="A donde vas?" type="text"/>
          </div>
        </div>
        <div class="hidden md:block w-px h-8 bg-outline-variant/20"></div>
        
        <div class="flex-1 w-full flex items-center px-4 py-3">
          <span class="material-symbols-outlined text-primary mr-3">calendar_today</span>
          <div class="text-left w-full">
            <label class="block text-[10px] font-bold text-outline uppercase tracking-wider" for="landing-date">Fecha</label>
            <input id="landing-date" name="date" class="w-full bg-transparent border-none p-0 text-on-surface focus:ring-0 placeholder:text-outline-variant font-medium" placeholder="Selecciona fecha" type="date"/>
          </div>
        </div>
        <div class="hidden md:block w-px h-8 bg-outline-variant/20"></div>
        
        <div class="flex-1 w-full flex items-center px-4 py-3">
          <span class="material-symbols-outlined text-primary mr-3">schedule</span>
          <div class="text-left w-full">
            <label class="block text-[10px] font-bold text-outline uppercase tracking-wider" for="landing-time">Horario</label>
            <select id="landing-time" name="time" class="w-full bg-transparent border-none p-0 text-on-surface focus:ring-0 font-medium appearance-none">
              <option value="">Inicio - Fin</option>
              <option value="morning">Manana</option>
              <option value="afternoon">Tarde</option>
              <option value="full">Dia completo</option>
            </select>
          </div>
        </div>
        <div class="hidden md:block w-px h-8 bg-outline-variant/20"></div>
        
        <div class="flex-1 w-full flex items-center px-4 py-3">
          <span class="material-symbols-outlined text-primary mr-3">group</span>
          <div class="text-left w-full">
            <label class="block text-[10px] font-bold text-outline uppercase tracking-wider" for="landing-capacity">Personas</label>
            <select id="landing-capacity" name="capacity" class="w-full bg-transparent border-none p-0 text-on-surface focus:ring-0 font-medium appearance-none">
              <option value="">Cuantos?</option>
              <option value="2">2 personas</option>
              <option value="4">4 personas</option>
              <option value="6">6 personas</option>
              <option value="8">8 personas</option>
              <option value="10">10 personas</option>
              <option value="12">12 personas</option>
            </select>
          </div>
        </div>
        
        <button type="submit" class="w-full md:w-auto bg-[#005da7] hover:bg-[#0076d1] text-white font-bold px-10 py-4 rounded-full transition-all active:scale-95 shadow-lg shadow-primary/20 flex items-center justify-center gap-2 border-none cursor-pointer">
          <span class="material-symbols-outlined">search</span>
          <span>Buscar</span>
        </button>
      </form>
    </div>
  </section>
</paw:layout>
