<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>

<c:url var="createUrl" value="/item/create" />
<spring:message code="item.typeId" var="typeIdLabel" />
<spring:message code="item.title" var="titleLabel" />
<spring:message code="item.description" var="descriptionLabel" />
<spring:message code="item.pricePerHour" var="pricePerHourLabel" />
<spring:message code="item.capacityPeople" var="capacityPeopleLabel" />
<spring:message code="item.maxWeightKg" var="maxWeightKgLabel" />
<spring:message code="item.difficultyLevel" var="difficultyLevelLabel" />
<spring:message code="item.location" var="locationLabel" />
<spring:message code="item.create.submit" var="createSubmitLabel" />

<paw:layout title="Botecito" mainClass="pt-24 pb-14 flex items-center justify-center min-h-screen">
  <div class="w-full max-w-2xl px-6">
    <paw:sectionCard icon="directions_boat">
      <jsp:attribute name="title"><spring:message code="item.create.heading" /></jsp:attribute>
      <jsp:body>
        <form:form modelAttribute="createForm" action="${createUrl}" method="post" class="space-y-4">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <paw:formField path="typeId" label="${typeIdLabel}" />
            <paw:formField path="title" label="${titleLabel}" />
            <div class="md:col-span-2">
              <paw:textareaField path="description" label="${descriptionLabel}" rows="4" maxlength="1000" />
            </div>
            <paw:formField path="pricePerHour" label="${pricePerHourLabel}" />
            <paw:formField path="capacityPeople" label="${capacityPeopleLabel}" />
            <paw:formField path="maxWeightKg" label="${maxWeightKgLabel}" />
            <paw:formField path="difficultyLevel" label="${difficultyLevelLabel}" />
            <div class="md:col-span-2">
              <paw:formField path="location" label="${locationLabel}" />
            </div>
          </div>
          <paw:button type="submit" fullWidth="true" color="primary" text="${createSubmitLabel}" />
        </form:form>
      </jsp:body>
    </paw:sectionCard>
  </div>
</paw:layout>
