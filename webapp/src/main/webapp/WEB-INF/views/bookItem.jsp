<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>

<c:url var="bookUrl" value="/item/${itemId}/book" />
<spring:message code="booking.startTime" var="bookingStartTimeLabel" />
<spring:message code="booking.endTime" var="bookingEndTimeLabel" />
<spring:message code="booking.requestMessage" var="bookingRequestMessageLabel" />
<spring:message code="booking.submit" var="bookingSubmitLabel" />

<paw:layout title="Botecito" mainClass="pt-24 pb-14 flex items-center justify-center min-h-screen">
  <div class="w-full max-w-xl px-6">
    <paw:sectionCard icon="event_available">
      <jsp:attribute name="title"><spring:message code="booking.heading" /></jsp:attribute>
      <jsp:body>
        <form:form modelAttribute="bookForm" action="${bookUrl}" method="post" class="space-y-4">
          <fieldset class="fieldset">
            <legend class="fieldset-legend text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="startTime">
              <c:out value="${bookingStartTimeLabel}" />
            </legend>
            <form:input path="startTime" id="startTime" type="datetime-local" cssClass="input w-full" cssErrorClass="input w-full input-error" />
            <form:errors path="startTime" cssClass="text-error text-xs mt-1" element="p" />
          </fieldset>

          <fieldset class="fieldset">
            <legend class="fieldset-legend text-xs font-semibold uppercase tracking-wider text-on-surface-variant" for="endTime">
              <c:out value="${bookingEndTimeLabel}" />
            </legend>
            <form:input path="endTime" id="endTime" type="datetime-local" cssClass="input w-full" cssErrorClass="input w-full input-error" />
            <form:errors path="endTime" cssClass="text-error text-xs mt-1" element="p" />
          </fieldset>

          <paw:textareaField path="requestMessage" label="${bookingRequestMessageLabel}" rows="4" maxlength="1000" />

          <paw:button type="submit" fullWidth="true" color="primary" text="${bookingSubmitLabel}" />
        </form:form>
      </jsp:body>
    </paw:sectionCard>
  </div>
</paw:layout>
