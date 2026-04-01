<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<footer class="w-full py-12 bg-[#f9f9ff] dark:bg-slate-950 border-t border-slate-100 dark:border-slate-800 mt-12">
  <div class="flex flex-col md:flex-row justify-between items-center px-8 max-w-7xl mx-auto gap-8">
    <div class="font-headline font-bold text-[#005da7] text-xl">
      Botecito
    </div>
    <div class="flex gap-8 font-body text-sm">
      <a class="text-slate-500 hover:text-[#ae3123] transition-colors no-underline" href="#">Terminos</a>
      <a class="text-slate-500 hover:text-[#ae3123] transition-colors no-underline" href="#">Privacidad</a>
      <a class="text-slate-500 hover:text-[#ae3123] transition-colors no-underline" href="#">Contacto</a>
    </div>
    <div class="text-slate-500 font-body text-sm">
      &copy; 2024 Botecito. Nautical Curator.
    </div>
  </div>
</footer>

<nav class="md:hidden fixed bottom-0 left-0 w-full flex justify-around items-center pb-safe pt-2 px-4 bg-white/80 dark:bg-slate-900/80 backdrop-blur-md shadow-[0_-8px_24px_rgba(11,28,50,0.04)] z-50 rounded-t-2xl">
  <a class="flex flex-col items-center justify-center bg-[#f0f3ff] dark:bg-slate-800 text-[#005da7] dark:text-[#0076d1] rounded-xl px-4 py-1 active:scale-90 duration-150 no-underline" href="<c:url value='/marketplace' />">
    <span class="material-symbols-outlined">explore</span>
    <span class="font-body text-[10px] font-medium">Explorar</span>
  </a>
  <a class="flex flex-col items-center justify-center text-slate-500 dark:text-slate-400 px-4 py-1 hover:bg-slate-100 dark:hover:bg-slate-800 active:scale-90 duration-150 no-underline" href="<c:url value='/publish' />">
    <span class="material-symbols-outlined">add_circle</span>
    <span class="font-body text-[10px] font-medium">Publicar</span>
  </a>
</nav>
