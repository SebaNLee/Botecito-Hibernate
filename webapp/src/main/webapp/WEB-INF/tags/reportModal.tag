<%@ tag language="java" pageEncoding="UTF-8" body-content="scriptless" %>
<%@ attribute name="itemId" required="true" type="java.lang.Integer" rtexprvalue="true" %>
<%@ attribute name="modalId" required="true" type="java.lang.String" rtexprvalue="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>

<spring:message code="report.form.title" var="reportFormTitle" />
<spring:message code="report.form.reason" var="reportReasonLabel" />
<spring:message code="report.form.description" var="reportDescriptionLabel" />
<spring:message code="report.form.description.placeholder" var="reportDescriptionPlaceholder" />
<spring:message code="report.form.submit" var="reportSubmitLabel" />

<paw:detailsModal id="${modalId}" title="${reportFormTitle}">
  <jsp:body>
    <form action="<c:url value='/item/${itemId}/report' />" method="post" class="space-y-4">
      <div class="grid grid-cols-1 sm:grid-cols-[8rem_minmax(0,1fr)] gap-3 items-center">
        <label class="text-xs font-bold uppercase tracking-wider text-outline" for="report-reason-${itemId}">
          <c:out value="${reportReasonLabel}" />
        </label>
        <select id="report-reason-${itemId}" name="reason" class="select select-bordered w-full" required>
          <option value="" disabled selected hidden></option>
          <option value="FAKE"><spring:message code="report.reason.FAKE" /></option>
          <option value="ABANDONED"><spring:message code="report.reason.ABANDONED" /></option>
          <option value="DUPLICATE"><spring:message code="report.reason.DUPLICATE" /></option>
          <option value="SPAM"><spring:message code="report.reason.SPAM" /></option>
          <option value="IRRELEVANT"><spring:message code="report.reason.IRRELEVANT" /></option>
          <option value="INAPPROPRIATE"><spring:message code="report.reason.INAPPROPRIATE" /></option>
          <option value="OTHER"><spring:message code="report.reason.OTHER" /></option>
        </select>
      </div>
      <div class="grid grid-cols-1 sm:grid-cols-[8rem_minmax(0,1fr)] gap-3">
        <label class="text-xs font-bold uppercase tracking-wider text-outline pt-2" for="report-description-${itemId}">
          <c:out value="${reportDescriptionLabel}" />
        </label>
        <textarea
          id="report-description-${itemId}"
          name="description"
          rows="3"
          maxlength="255"
          class="textarea textarea-bordered w-full"
          placeholder="<c:out value='${reportDescriptionPlaceholder}' />"
        ></textarea>
      </div>
      <paw:button type="submit" color="primary" text="${reportSubmitLabel}" submitLoading="true" />
    </form>
  </jsp:body>
</paw:detailsModal>

<c:if test="${openReportModal}">
  <script>
    document.getElementById('<c:out value="${modalId}" />')?.showModal();
  </script>
</c:if>
