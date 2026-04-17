<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="homeUrl" value="/" />
<spring:message code="error.403.backHome" var="backHomeLabel" />

<paw:layout title="Botecito" mainClass="pt-24 pb-14 flex items-center justify-center min-h-screen">
  <div class="w-full max-w-md px-6">
    <div class="card bg-base-100 shadow-sm">
      <div class="card-body p-8 gap-4 text-center">
        <div class="w-14 h-14 bg-error/15 text-error rounded-full flex items-center justify-center mx-auto">
          <span class="material-symbols-outlined text-3xl">block</span>
        </div>
        <h1 class="text-2xl font-extrabold tracking-tight text-on-background m-0">
          <spring:message code="error.403.title" />
        </h1>
        <p class="text-on-surface-variant m-0">
          <spring:message code="error.403.message" />
        </p>
        <div class="card-actions justify-center mt-2">
          <paw:button href="${homeUrl}" color="primary" text="${backHomeLabel}" />
        </div>
      </div>
    </div>
  </div>
</paw:layout>
