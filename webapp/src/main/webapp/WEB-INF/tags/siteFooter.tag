<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<footer class="footer footer-center w-full py-12 bg-base-100 border-t border-outline-variant/20 mt-12">
  <div class="flex flex-col md:flex-row justify-between items-center w-full max-w-7xl mx-auto px-8 gap-8">
    <div class="font-headline font-bold text-primary text-xl">
      Botecito
    </div>
    <nav class="flex gap-8 font-body text-sm">
      <a class="link link-hover text-on-surface-variant" href="<c:url value='/' />">
        <spring:message code="nav.home" />
      </a>
      <a class="link link-hover text-on-surface-variant" href="<c:url value='/marketplace' />">
        <spring:message code="nav.marketplace" />
      </a>
      <a class="link link-hover text-on-surface-variant" href="<c:url value='/publish' />">
        <spring:message code="nav.publish" />
      </a>
    </nav>
    <div class="text-on-surface-variant font-body text-sm">
      <spring:message code="footer.copyright" />
    </div>
  </div>
</footer>
