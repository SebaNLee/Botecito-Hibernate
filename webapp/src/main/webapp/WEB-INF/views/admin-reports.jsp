<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<spring:message code="page.title.adminReports" var="titleAdminReports" />
<spring:message code="admin.reports.sort.newest" var="sortNewestLabel" />
<spring:message code="admin.reports.sort.oldest" var="sortOldestLabel" />
<spring:message code="marketplace.sort.label" var="sortLabel" />
<spring:message code="marketplace.field.pageSize" var="pageSizeFieldLabel" />
<spring:message code="admin.reports.dismiss" var="dismissLabel" />
<spring:message code="admin.reports.deletePublication" var="deletePublicationLabel" />
<spring:message code="admin.reports.dismiss.confirm.title" var="dismissConfirmTitle" />
<spring:message code="admin.reports.dismiss.confirm.message" var="dismissConfirmMessage" />
<spring:message code="admin.reports.deletePublication.confirm.title" var="deletePublicationConfirmTitle" />
<spring:message code="admin.reports.deletePublication.confirm.message" var="deletePublicationConfirmMessage" />
<spring:message code="admin.reports.dismiss.confirm.confirm" var="dismissConfirmConfirmLabel" />
<spring:message code="admin.reports.deletePublication.confirm.confirm" var="deletePublicationConfirmConfirmLabel" />
<spring:message code="settings.publications.delete.confirm.cancel" var="cancelLabel" />

<c:set var="hasActiveReportsFilters" value="${adminReportsSearch.sortBy != 'newest' or adminReportsSearch.pageSize != 12 or adminReportsSearch.page > 1}" />
<c:set var="showReportsFilterEmpty" value="${hasValidationErrors or hasActiveReportsFilters or reportPage.totalItems > 0}" />
<c:url var="clearReportsFiltersUrl" value="/admin/reports" />

<c:if test="${reportPage.totalPages > 1}">
  <c:url var="previousPageUrl" value="/admin/reports">
    <c:param name="page" value="${reportPage.previousPage}" />
    <c:param name="sortBy" value="${adminReportsSearch.sortBy}" />
    <c:param name="pageSize" value="${adminReportsSearch.pageSize}" />
  </c:url>
  <c:url var="nextPageUrl" value="/admin/reports">
    <c:param name="page" value="${reportPage.nextPage}" />
    <c:param name="sortBy" value="${adminReportsSearch.sortBy}" />
    <c:param name="pageSize" value="${adminReportsSearch.pageSize}" />
  </c:url>
</c:if>

<paw:layout title="${titleAdminReports} - Botecito" mainClass="pt-24 pb-14 w-full max-w-7xl mx-auto px-6" scripts="toast">
  <paw:toastNotifier />
  <section class="min-w-0 space-y-6">
    <div class="flex flex-col md:flex-row md:items-end justify-between gap-4">
      <div class="min-w-0">
        <h1 class="text-4xl font-extrabold tracking-tight text-on-background m-0 break-words">
          <spring:message code="admin.reports.title" />
        </h1>
        <p class="text-on-surface-variant mt-2 m-0">
          <c:choose>
            <c:when test="${reportPage.totalItems == 1}">
              <spring:message code="admin.reports.results.count.singular" />
            </c:when>
            <c:otherwise>
              <spring:message code="admin.reports.results.count.plural" arguments="${reportPage.totalItems}" />
            </c:otherwise>
          </c:choose>
        </p>
      </div>

      <form
          id="admin-reports-filters-form"
          action="<c:url value='/admin/reports' />"
          method="get"
          class="flex items-center gap-3 text-sm font-medium text-on-surface-variant">
        <input type="hidden" name="page" value="${adminReportsSearch.page}" />
        <label for="admin-reports-sort" class="shrink-0"><c:out value="${sortLabel}" /></label>
        <select
            id="admin-reports-sort"
            name="sortBy"
            class="select select-sm font-bold text-primary"
            onchange="this.form.requestSubmit()">
          <option value="newest" ${adminReportsSearch.sortBy == 'newest' ? 'selected="selected"' : ''}><c:out value="${sortNewestLabel}" /></option>
          <option value="oldest" ${adminReportsSearch.sortBy == 'oldest' ? 'selected="selected"' : ''}><c:out value="${sortOldestLabel}" /></option>
        </select>
        <label for="admin-reports-page-size" class="shrink-0 ml-2"><c:out value="${pageSizeFieldLabel}" /></label>
        <select
            id="admin-reports-page-size"
            name="pageSize"
            class="select select-sm w-20 font-bold text-primary"
            onchange="this.form.requestSubmit()">
          <option value="6" ${adminReportsSearch.pageSize == 6 ? 'selected="selected"' : ''}>6</option>
          <option value="12" ${adminReportsSearch.pageSize == 12 ? 'selected="selected"' : ''}>12</option>
          <option value="18" ${adminReportsSearch.pageSize == 18 ? 'selected="selected"' : ''}>18</option>
        </select>
      </form>
    </div>

    <c:choose>
      <c:when test="${empty reportPage.content}">
        <c:choose>
          <c:when test="${showReportsFilterEmpty}">
            <div class="card bg-base-100 shadow-sm">
              <div class="card-body items-center gap-4 p-10 text-center">
                <div class="flex h-16 w-16 items-center justify-center rounded-full bg-primary/10 text-primary">
                  <span class="material-symbols-outlined text-4xl" aria-hidden="true">flag</span>
                </div>
                <div class="max-w-lg">
                  <h2 class="m-0 text-2xl font-extrabold tracking-tight text-on-background"><spring:message code="admin.reports.filter.empty.title" /></h2>
                  <p class="m-0 mt-2 text-on-surface-variant"><spring:message code="admin.reports.filter.empty.message" /></p>
                </div>
                <a href="${clearReportsFiltersUrl}" class="btn btn-primary no-underline" data-clear-list-filters>
                  <spring:message code="marketplace.empty.clear" />
                </a>
              </div>
            </div>
          </c:when>
          <c:otherwise>
            <paw:alertMessage type="info">
              <spring:message code="admin.reports.empty" />
            </paw:alertMessage>
          </c:otherwise>
        </c:choose>
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
                    <c:if test="${not empty report.createdAt}">
                      <fmt:parseDate value="${fn:substring(report.createdAt, 0, 16)}" pattern="yyyy-MM-dd'T'HH:mm" var="parsedReportDate" />
                      <fmt:formatDate value="${parsedReportDate}" pattern="dd/MM/yyyy HH:mm" var="formattedReportDate" />
                    </c:if>
                    <c:out value="${formattedReportDate}" />
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
                      <c:out value="${not empty report.item.latestVersion.title ? report.item.latestVersion.title : report.item.id}" />
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
                          <input type="hidden" name="pageSize" value="${adminReportsSearch.pageSize}" />
                          <input type="hidden" name="sortBy" value="${adminReportsSearch.sortBy}" />
                          <button type="submit" class="btn btn-primary min-h-11 whitespace-nowrap px-5">
                            <c:out value="${dismissConfirmConfirmLabel}" />
                          </button>
                        </form>
                      </paw:confirmModal>
                      <paw:confirmModal id="${deletePublicationModalId}" title="${deletePublicationConfirmTitle}" message="${deletePublicationConfirmMessage}" confirmText="${deletePublicationConfirmConfirmLabel}" cancelText="${cancelLabel}" icon="delete">
                        <form action="<c:url value='/admin/reports/${report.id}/delete-publication' />" method="post" class="m-0">
                          <input type="hidden" name="page" value="${reportPage.page}" />
                          <input type="hidden" name="pageSize" value="${adminReportsSearch.pageSize}" />
                          <input type="hidden" name="sortBy" value="${adminReportsSearch.sortBy}" />
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
      </c:otherwise>
    </c:choose>

    <paw:pagination
        currentPage="${reportPage.page}"
        totalPages="${reportPage.totalPages}"
        hasPrevious="${reportPage.hasPrevious}"
        hasNext="${reportPage.hasNext}"
        previousPageUrl="${previousPageUrl}"
        nextPageUrl="${nextPageUrl}" />
  </section>
</paw:layout>
