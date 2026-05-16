<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="outgoingFormAction" value="/requests/outgoing" />

<paw:layout title="Botecito" mainClass="pt-24 pb-14 w-full max-w-7xl mx-auto px-6">
  <paw:requestsBookingsList
      formAction="${outgoingFormAction}"
      sidebarActive="outgoing"
      listMode="outgoing" />
</paw:layout>
