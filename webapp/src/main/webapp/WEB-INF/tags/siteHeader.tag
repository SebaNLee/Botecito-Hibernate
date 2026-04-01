<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<header class="fixed top-0 w-full z-50 bg-white/80 dark:bg-slate-900/80 backdrop-blur-md shadow-[0_32px_48px_rgba(11,28,50,0.06)]">
  <div class="flex justify-between items-center px-6 py-4 max-w-7xl mx-auto">
    <a href="<c:url value='/' />" class="text-2xl font-black text-[#005da7] dark:text-[#0076d1] font-headline tracking-tight no-underline">
      Botecito
    </a>
    <div class="flex items-center gap-6">
      <a href="<c:url value='/publish' />" class="bg-[#ae3123] text-white font-bold px-6 py-2.5 rounded-lg hover:opacity-90 transition-opacity active:scale-95 duration-200 font-headline text-sm no-underline">
        Publicar barco
      </a>
    </div>
  </div>
</header>
