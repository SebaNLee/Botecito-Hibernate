<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<fmt:setLocale value="es_AR" />
<spring:message code="page.title.adminReports" var="titleAdminReports" />
<spring:message code="admin.reports.sort.newest" var="sortNewestLabel" />
<spring:message code="admin.reports.sort.oldest" var="sortOldestLabel" />
<spring:message code="admin.reports.dismiss" var="dismissLabel" />
<spring:message code="admin.reports.deletePublication" var="deletePublicationLabel" />
<spring:message code="admin.reports.dismiss.confirm.title" var="dismissConfirmTitle" />
<spring:message code="admin.reports.dismiss.confirm.message" var="dismissConfirmMessage" />
<spring:message code="admin.reports.deletePublication.confirm.title" var="deletePublicationConfirmTitle" />
<spring:message code="admin.reports.deletePublication.confirm.message" var="deletePublicationConfirmMessage" />
<spring:message code="admin.reports.dismiss.confirm.confirm" var="dismissConfirmConfirmLabel" />
<spring:message code="admin.reports.deletePublication.confirm.confirm" var="deletePublicationConfirmConfirmLabel" />
<spring:message code="settings.publications.delete.confirm.cancel" var="cancelLabel" />

<c:set var="pageSize" value="${adminReportsSearch.pageSize != null ? adminReportsSearch.pageSize : 12}" />
<c:set var="currentSortBy" value="${empty adminReportsSearch.sortBy ? 'newest' : adminReportsSearch.sortBy}" />

<c:if test="${reportPage.totalPages > 1}">
  <c:url var="previousPageUrl" value="/admin/reports">
    <c:param name="page" value="${reportPage.previousPage}" />
    <c:param name="sortBy" value="${currentSortBy}" />
    <c:param name="pageSize" value="${pageSize}" />
  </c:url>
  <c:url var="nextPageUrl" value="/admin/reports">
    <c:param name="page" value="${reportPage.nextPage}" />
    <c:param name="sortBy" value="${currentSortBy}" />
    <c:param name="pageSize" value="${pageSize}" />
  </c:url>
</c:if>

<paw:layout title="${titleAdminReports} - Botecito" mainClass="pt-24 pb-14 w-full max-w-7xl mx-auto px-6" scripts="toast">
  <paw:toastNotifier />
  <section class="min-w-0 space-y-6">
    <div class="flex flex-col gap-2">
      <h1 class="text-3xl font-extrabold tracking-tight text-on-background m-0 break-words">
        <spring:message code="admin.reports.title" />
      </h1>
      <p class="text-on-surface-variant m-0">
        <spring:message code="admin.reports.subtitle" />
      </p>
    </div>

    <form action="<c:url value='/admin/reports' />" method="get" class="flex flex-wrap items-end gap-3">
      <label class="form-control w-full sm:w-auto">
        <span class="label-text text-xs font-bold uppercase tracking-wider text-outline">
          <spring:message code="admin.reports.sort.label" />
        </span>
        <select name="sortBy" class="select select-bordered select-sm">
          <option value="newest" ${currentSortBy == 'newest' ? 'selected' : ''}><c:out value="${sortNewestLabel}" /></option>
          <option value="oldest" ${currentSortBy == 'oldest' ? 'selected' : ''}><c:out value="${sortOldestLabel}" /></option>
        </select>
      </label>
      <label class="form-control w-full sm:w-auto">
        <span class="label-text text-xs font-bold uppercase tracking-wider text-outline">
          <spring:message code="admin.reports.pageSize.label" />
        </span>
        <select name="pageSize" class="select select-bordered select-sm">
          <option value="6" ${pageSize == 6 ? 'selected' : ''}>6</option>
          <option value="12" ${pageSize == 12 ? 'selected' : ''}>12</option>
          <option value="18" ${pageSize == 18 ? 'selected' : ''}>18</option>
        </select>
      </label>
      <input type="hidden" name="page" value="1" />
      <button type="submit" class="btn btn-primary btn-sm">
        <spring:message code="admin.reports.applyFilters" />
      </button>
    </form>

    <c:choose>
      <c:when test="${empty reportPage.content}">
        <paw:alertMessage type="info">
          <spring:message code="admin.reports.empty" />
        </paw:alertMessage>
      </c:when>
      <c:otherwise>
        <div class="overflow-x-auto rounded-2xl border border-outline-variant/20 bg-base-100 shadow-sm">
          <table class="table table-zebra">
            <thead>
              <tr class="text-xs uppercase tracking-wider text-outline">
                <th><spring:message code="admin.reports.column.createdAt" /></th>
                <th><spring:message code="admin.reports.column.reason" /></th>
                <th><spring:message code="admin.reports.column.description" /></th>
                <th><spring:message code="admin.reports.column.publication" /></th>
                <th><spring:message code="admin.reports.column.reporter" /></th>
                <th><spring:message code="admin.reports.column.owner" /></th>
                <th><spring:message code="admin.reports.column.actions" /></th>
              </tr>
            </thead>
            <tbody>
              <c:forEach items="${reportPage.content}" var="report">
                <tr>
                  <td class="whitespace-nowrap text-sm">
                    <c:out value="${reportDatesById[report.id]}" />
                  </td>
                  <td class="text-sm">
                    <spring:message code="report.reason.${report.reason}" />
                  </td>
                  <td class="text-sm max-w-xs break-words">
                    <c:choose>
                      <c:when test="${not empty report.description}">
                        <c:out value="${report.description}" />
                      </c:when>
                      <c:otherwise>
                        <span class="text-outline italic"><spring:message code="admin.reports.noDescription" /></span>
                      </c:otherwise>
                    </c:choose>
                  </td>
                  <td class="text-sm">
                    <c:url var="itemDetailUrl" value="/item/${report.item.id}" />
                    <a href="${itemDetailUrl}" class="link link-primary font-semibold no-underline break-words">
                      <c:out value="${not empty report.itemTitle ? report.itemTitle : report.item.id}" />
                    </a>
                  </td>
                  <td class="text-sm break-words">
                    <c:choose>
                      <c:when test="${report.sender != null}">
                        <c:out value="${report.sender.firstName}" /> <c:out value="${report.sender.lastName}" />
                        <div class="text-xs text-outline break-all"><c:out value="${report.sender.email}" /></div>
                      </c:when>
                      <c:otherwise>
                        <spring:message code="admin.reports.anonymousReporter" />
                      </c:otherwise>
                    </c:choose>
                  </td>
                  <td class="text-sm break-words">
                    <c:choose>
                      <c:when test="${report.item.host != null}">
                        <c:out value="${report.item.host.firstName}" /> <c:out value="${report.item.host.lastName}" />
                        <div class="text-xs text-outline break-all"><c:out value="${report.item.host.email}" /></div>
                      </c:when>
                      <c:otherwise>
                        <spring:message code="itemDetail.owner.none" />
                      </c:otherwise>
                    </c:choose>
                  </td>
                  <td>
                    <div class="flex flex-col gap-2 min-w-[10rem]">
                      <c:set var="dismissModalId" value="dismiss-report-${report.id}" />
                      <c:set var="deletePublicationModalId" value="delete-publication-report-${report.id}" />
                      <button type="button" class="btn btn-outline btn-sm" onclick="document.getElementById('${dismissModalId}').showModal()">
                        <c:out value="${dismissLabel}" />
                      </button>
                      <button type="button" class="btn btn-outline btn-error btn-sm" onclick="document.getElementById('${deletePublicationModalId}').showModal()">
                        <c:out value="${deletePublicationLabel}" />
                      </button>
                      <paw:confirmModal id="${dismissModalId}" title="${dismissConfirmTitle}" message="${dismissConfirmMessage}" confirmText="${dismissConfirmConfirmLabel}" cancelText="${cancelLabel}" confirmColor="primary" icon="flag">
                        <form action="<c:url value='/admin/reports/${report.id}/dismiss' />" method="post" class="m-0">
                          <input type="hidden" name="page" value="${reportPage.page}" />
                          <input type="hidden" name="pageSize" value="${pageSize}" />
                          <input type="hidden" name="sortBy" value="${currentSortBy}" />
                          <button type="submit" class="btn btn-primary min-h-11 whitespace-nowrap px-5">
                            <c:out value="${dismissConfirmConfirmLabel}" />
                          </button>
                        </form>
                      </paw:confirmModal>
                      <paw:confirmModal id="${deletePublicationModalId}" title="${deletePublicationConfirmTitle}" message="${deletePublicationConfirmMessage}" confirmText="${deletePublicationConfirmConfirmLabel}" cancelText="${cancelLabel}" icon="delete">
                        <form action="<c:url value='/admin/reports/${report.id}/delete-publication' />" method="post" class="m-0">
                          <input type="hidden" name="page" value="${reportPage.page}" />
                          <input type="hidden" name="pageSize" value="${pageSize}" />
                          <input type="hidden" name="sortBy" value="${currentSortBy}" />
                          <button type="submit" class="btn btn-error min-h-11 whitespace-nowrap px-5">
                            <c:out value="${deletePublicationConfirmConfirmLabel}" />
                          </button>
                        </form>
                      </paw:confirmModal>
                    </div>
                  </td>
                </tr>
              </c:forEach>
            </tbody>
          </table>
        </div>

        <c:if test="${reportPage.totalPages > 1}">
          <nav class="mt-6 flex flex-col sm:flex-row items-center justify-between gap-4 text-sm font-bold text-on-surface-variant">
            <c:choose>
              <c:when test="${reportPage.hasPrevious}">
                <a href="${previousPageUrl}" class="btn btn-outline btn-sm no-underline gap-2">
                  <span class="material-symbols-outlined text-sm">arrow_back</span>
                  <spring:message code="marketplace.pagination.previous" />
                </a>
              </c:when>
              <c:otherwise>
                <span class="btn btn-outline btn-sm btn-disabled gap-2">
                  <span class="material-symbols-outlined text-sm">arrow_back</span>
                  <spring:message code="marketplace.pagination.previous" />
                </span>
              </c:otherwise>
            </c:choose>

            <span>
              <spring:message code="marketplace.pagination.page" arguments="${reportPage.page},${reportPage.totalPages}" />
            </span>

            <c:choose>
              <c:when test="${reportPage.hasNext}">
                <a href="${nextPageUrl}" class="btn btn-outline btn-sm no-underline gap-2">
                  <spring:message code="marketplace.pagination.next" />
                  <span class="material-symbols-outlined text-sm">arrow_forward</span>
                </a>
              </c:when>
              <c:otherwise>
                <span class="btn btn-outline btn-sm btn-disabled gap-2">
                  <spring:message code="marketplace.pagination.next" />
                  <span class="material-symbols-outlined text-sm">arrow_forward</span>
                </span>
              </c:otherwise>
            </c:choose>
          </nav>
        </c:if>
      </c:otherwise>
    </c:choose>
  </section>
</paw:layout>
