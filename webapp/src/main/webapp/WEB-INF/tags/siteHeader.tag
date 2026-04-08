<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ attribute name="ctaMessageCode" required="false" %>
<%@ attribute name="ctaHref" required="false" %>
<%@ attribute name="ctaVariant" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<c:set var="resolvedCtaMessageCode" value="${not empty ctaMessageCode ? ctaMessageCode : 'nav.publishCta'}" />
<c:set var="resolvedCtaHref" value="${not empty ctaHref ? ctaHref : '/publish'}" />
<c:set var="resolvedCtaVariant" value="${not empty ctaVariant ? ctaVariant : 'publish'}" />
<c:set
  var="resolvedCtaClasses"
  value="${resolvedCtaVariant == 'rent' ? 'bg-primary text-on-primary font-bold px-6 py-2.5 rounded-lg hover:bg-primary-container transition-colors active:scale-95 duration-200 font-headline text-sm no-underline shadow-lg shadow-primary/20' : 'bg-[#ae3123] text-white font-bold px-6 py-2.5 rounded-lg hover:opacity-90 transition-opacity active:scale-95 duration-200 font-headline text-sm no-underline'}" />

<header class="fixed top-0 w-full z-50 bg-white shadow-[0_32px_48px_rgba(11,28,50,0.06)]">
  <div class="flex justify-between items-center px-6 py-4 max-w-7xl mx-auto">
    <a href="<c:url value='/' />" class="text-2xl font-black text-[#005da7] dark:text-[#0076d1] font-headline tracking-tight no-underline">
      Botecito
    </a>
    <div class="flex items-center gap-6">
      <a href="<c:url value='${resolvedCtaHref}' />" class="${resolvedCtaClasses}">
        <spring:message code="${resolvedCtaMessageCode}" />
      </a>
    </div>
  </div>
</header>
