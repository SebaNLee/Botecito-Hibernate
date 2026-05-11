<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<paw:layout title="Botecito" mainClass="pt-24 pb-14 w-full max-w-7xl mx-auto px-6">
  <div class="grid grid-cols-1 gap-8 lg:grid-cols-[14rem_minmax(0,1fr)] lg:items-start">
    <paw:requestsSidebar active="incoming" />
    <section class="min-w-0 space-y-4">
      <h1 class="text-3xl font-extrabold tracking-tight text-on-background m-0 break-words">
        <spring:message code="requests.incoming.title" />
      </h1>
      <p class="m-0 rounded-xl border border-dashed border-outline-variant/40 bg-base-200/50 px-4 py-8 text-center text-sm text-on-surface-variant">
        <spring:message code="requests.incoming.placeholder" />
      </p>
    </section>
  </div>
</paw:layout>
