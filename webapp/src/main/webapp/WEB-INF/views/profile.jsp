<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="logoutUrl" value="/logout" />

<c:set var="initials" value="" />
<c:if test="${not empty user.givenName}">
  <c:set var="initials" value="${fn:substring(user.givenName, 0, 1)}" />
</c:if>
<c:if test="${not empty user.lastName}">
  <c:set var="initials" value="${initials}${fn:substring(user.lastName, 0, 1)}" />
</c:if>
<c:set var="initials" value="${fn:toUpperCase(initials)}" />

<paw:layout title="Botecito" mainClass="pt-24 pb-14 max-w-2xl mx-auto px-6">
  <div class="bg-surface-container-lowest rounded-2xl p-8 shadow-[0_32px_48px_rgba(11,28,50,0.04)] space-y-8">

    <div class="flex items-center gap-5 pb-6 border-b border-outline-variant/20">
      <div class="w-16 h-16 rounded-full bg-primary flex items-center justify-center shrink-0">
        <span class="text-on-primary font-bold text-2xl font-headline">${initials}</span>
      </div>
      <div>
        <h1 class="text-2xl font-extrabold tracking-tight text-on-background m-0">
          <c:out value="${user.name}" />
        </h1>
        <p class="text-on-surface-variant m-0 mt-1">
          <c:out value="${user.email}" />
        </p>
      </div>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div class="rounded-xl bg-surface-container-high p-4">
        <p class="text-[11px] uppercase tracking-wider font-bold text-outline m-0">
          <spring:message code="profile.givenName" />
        </p>
        <p class="text-base font-bold text-on-surface mt-1 mb-0">
          <c:out value="${user.givenName}" />
        </p>
      </div>

      <div class="rounded-xl bg-surface-container-high p-4">
        <p class="text-[11px] uppercase tracking-wider font-bold text-outline m-0">
          <spring:message code="profile.lastName" />
        </p>
        <p class="text-base font-bold text-on-surface mt-1 mb-0">
          <c:out value="${user.lastName}" />
        </p>
      </div>

      <div class="rounded-xl bg-surface-container-high p-4">
        <p class="text-[11px] uppercase tracking-wider font-bold text-outline m-0">
          <spring:message code="profile.email" />
        </p>
        <p class="text-base font-bold text-on-surface mt-1 mb-0">
          <c:out value="${user.email}" />
        </p>
      </div>

      <div class="rounded-xl bg-surface-container-high p-4">
        <p class="text-[11px] uppercase tracking-wider font-bold text-outline m-0">
          <spring:message code="profile.memberSince" />
        </p>
        <p class="text-base font-bold text-on-surface mt-1 mb-0">
          <c:out value="${user.createdAt}" />
        </p>
      </div>
    </div>

    <form action="${logoutUrl}" method="post">
      <button
        type="submit"
        class="w-full px-6 py-3 bg-secondary text-white font-bold rounded-xl shadow-[0_16px_32px_rgba(174,49,35,0.24)] hover:bg-[#962619] transition-all active:scale-[0.98] border-none cursor-pointer text-sm">
        <spring:message code="profile.logout" />
      </button>
    </form>
  </div>
</paw:layout>
