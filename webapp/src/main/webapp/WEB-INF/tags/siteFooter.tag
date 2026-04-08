<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<footer class="w-full py-12 bg-white border-t border-slate-100 mt-12">
  <div class="flex flex-col md:flex-row justify-between items-center px-8 max-w-7xl mx-auto gap-8">
    <div class="font-headline font-bold text-[#005da7] text-xl">
      Botecito
    </div>
    <div class="flex gap-8 font-body text-sm">
      <a class="text-slate-500 hover:text-primary transition-colors no-underline" href="<c:url value='/' />">
        <spring:message code="nav.home" />
      </a>
      <a class="text-slate-500 hover:text-primary transition-colors no-underline" href="<c:url value='/marketplace' />">
        <spring:message code="nav.marketplace" />
      </a>
      <a class="text-slate-500 hover:text-primary transition-colors no-underline" href="<c:url value='/publish' />">
        <spring:message code="nav.publish" />
      </a>
    </div>
    <div class="text-slate-500 font-body text-sm">
      <spring:message code="footer.copyright" />
    </div>
  </div>
</footer>
