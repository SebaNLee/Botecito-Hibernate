<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="homeUrl" value="/" />

<paw:layout title="Botecito" mainClass="pt-24 pb-14 flex items-center justify-center min-h-screen">
  <div class="w-full max-w-md px-6">
    <div class="bg-surface-container-lowest rounded-2xl p-8 shadow-[0_32px_48px_rgba(11,28,50,0.04)] text-center space-y-4">
      <div class="w-14 h-14 bg-red-100 text-red-600 rounded-full flex items-center justify-center mx-auto">
        <span class="material-symbols-outlined text-3xl">block</span>
      </div>
      <h1 class="text-2xl font-extrabold tracking-tight text-on-background m-0">
        <spring:message code="error.403.title" />
      </h1>
      <p class="text-on-surface-variant m-0">
        <spring:message code="error.403.message" />
      </p>
      <a
        href="${homeUrl}"
        class="inline-block px-6 py-3 bg-primary text-on-primary font-bold rounded-xl shadow-lg shadow-primary/20 hover:bg-primary-container transition-all no-underline text-sm mt-2">
        <spring:message code="error.403.backHome" />
      </a>
    </div>
  </div>
</paw:layout>
