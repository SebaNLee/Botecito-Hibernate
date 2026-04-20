<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<c:url var="logoutUrl" value="/logout" />
<spring:message code="profile.logout" var="logoutLabel" />
<spring:message code="profile.publications.edit" var="editLabel" />
<spring:message code="profile.publications.manageAvailability" var="manageAvailabilityLabel" />
<spring:message code="profile.publications.enable" var="enableLabel" />
<spring:message code="profile.publications.disable" var="disableLabel" />
<spring:message code="profile.publications.delete" var="deleteLabel" />
<spring:message code="profile.publications.actions" var="actionsLabel" />
<spring:message code="profile.publications.delete.confirm.title" var="deleteConfirmTitle" />
<spring:message code="profile.publications.delete.confirm.message" var="deleteConfirmMessage" />
<spring:message code="profile.publications.delete.confirm.confirm" var="deleteConfirmConfirm" />
<spring:message code="profile.publications.delete.confirm.cancel" var="deleteConfirmCancel" />
<spring:message code="profile.bookings.accept" var="acceptLabel" />
<spring:message code="profile.bookings.decline" var="declineLabel" />
<spring:message code="profile.paymentProofs.confirmReceived" var="paymentReceivedLabel" />
<spring:message code="profile.sentBookings.paymentProof.upload" var="uploadPaymentProofLabel" />

<c:set var="initials" value="" />
<c:if test="${not empty user.givenName}">
  <c:set var="initials" value="${fn:substring(user.givenName, 0, 1)}" />
</c:if>
<c:if test="${not empty user.lastName}">
  <c:set var="initials" value="${initials}${fn:substring(user.lastName, 0, 1)}" />
</c:if>
<c:set var="initials" value="${fn:toUpperCase(initials)}" />

<paw:layout title="Botecito" mainClass="pt-24 pb-14 max-w-4xl mx-auto px-6">
  <div class="card bg-base-100 shadow-sm">
    <div class="card-body p-8 gap-8">

      <div class="flex items-center gap-5 pb-6 border-b border-outline-variant/20">
        <div class="avatar placeholder">
          <div class="bg-primary text-primary-content rounded-full w-16 h-16 flex items-center justify-center shrink-0">
            <span class="font-bold text-2xl font-headline">${initials}</span>
          </div>
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
        <div class="rounded-xl bg-base-200 p-4">
          <p class="text-[11px] uppercase tracking-wider font-bold text-outline m-0">
            <spring:message code="profile.givenName" />
          </p>
          <p class="text-base font-bold text-on-surface mt-1 mb-0"><c:out value="${user.givenName}" /></p>
        </div>
        <div class="rounded-xl bg-base-200 p-4">
          <p class="text-[11px] uppercase tracking-wider font-bold text-outline m-0">
            <spring:message code="profile.lastName" />
          </p>
          <p class="text-base font-bold text-on-surface mt-1 mb-0"><c:out value="${user.lastName}" /></p>
        </div>
        <div class="rounded-xl bg-base-200 p-4">
          <p class="text-[11px] uppercase tracking-wider font-bold text-outline m-0">
            <spring:message code="profile.email" />
          </p>
          <p class="text-base font-bold text-on-surface mt-1 mb-0"><c:out value="${user.email}" /></p>
        </div>
        <div class="rounded-xl bg-base-200 p-4">
          <p class="text-[11px] uppercase tracking-wider font-bold text-outline m-0">
            <spring:message code="profile.memberSince" />
          </p>
          <p class="text-base font-bold text-on-surface mt-1 mb-0"><c:out value="${user.createdAt}" /></p>
        </div>
      </div>

      <div id="my-publications" class="scroll-mt-24 space-y-4">
        <h2 class="text-xl font-extrabold tracking-tight m-0"><spring:message code="profile.publications.title" /></h2>
        <c:if test="${param.publishAction == 'deleted'}">
          <paw:alertMessage type="success"><spring:message code="profile.publications.deleted" /></paw:alertMessage>
        </c:if>
        <c:if test="${param.publishAction == 'updated'}">
          <paw:alertMessage type="success"><spring:message code="profile.publications.updated" /></paw:alertMessage>
        </c:if>
        <c:if test="${param.publishAction == 'disabled'}">
          <paw:alertMessage type="success"><spring:message code="profile.publications.disabled" /></paw:alertMessage>
        </c:if>
        <c:if test="${param.publishAction == 'enabled'}">
          <paw:alertMessage type="success"><spring:message code="profile.publications.enabled" /></paw:alertMessage>
        </c:if>
        <c:if test="${param.publishAction == 'alreadyDeleted'}">
          <paw:alertMessage type="warning"><spring:message code="profile.publications.alreadyDeleted" /></paw:alertMessage>
        </c:if>
        <c:if test="${param.publishAction == 'forbidden' || param.publishAction == 'error'}">
          <paw:alertMessage type="error"><spring:message code="profile.publications.error" /></paw:alertMessage>
        </c:if>
        <c:if test="${param.publishAction == 'deleteBlockedByBookings'}">
          <paw:alertMessage type="error"><spring:message code="profile.publications.deleteBlockedByBookings" /></paw:alertMessage>
        </c:if>
        <c:choose>
          <c:when test="${not empty ownedItems}">
            <c:forEach var="item" items="${ownedItems}">
              <c:url var="editItemUrl" value="/profile/item/${item.id}/edit" />
              <c:url var="manageAvailabilityItemUrl" value="/profile/item/${item.id}/availability" />
              <c:url var="disableItemUrl" value="/profile/item/${item.id}/disable" />
              <c:url var="enableItemUrl" value="/profile/item/${item.id}/enable" />
              <c:url var="deleteItemUrl" value="/profile/item/${item.id}/delete" />
              <c:set var="deleteModalId" value="delete-publication-modal-${item.id}" />
              <c:set var="kebabId" value="publication-kebab-${item.id}" />
              <div class="rounded-xl bg-base-200 p-4 flex items-center justify-between gap-4">
                <div class="${item.active ? '' : 'opacity-75'}">
                  <div class="flex items-center gap-2">
                    <p class="m-0 text-sm font-bold text-on-surface"><c:out value="${item.title}" /></p>
                    <span class="badge ${item.active ? 'badge-success' : 'badge-ghost'} badge-sm font-bold">
                      <spring:message code="${item.active ? 'profile.publications.status.active' : 'profile.publications.status.inactive'}" />
                    </span>
                  </div>
                  <p class="m-0 text-xs text-on-surface-variant"><spring:message code="profile.publications.price" arguments="${item.pricePerHour}" /></p>
                </div>

                <paw:kebabMenu id="${kebabId}" ariaLabel="${actionsLabel}">
                  <li>
                    <a href="${editItemUrl}" class="flex items-center gap-2">
                      <span class="material-symbols-outlined text-base leading-none">edit</span>
                      <span><c:out value="${editLabel}" /></span>
                    </a>
                  </li>
                  <li>
                    <a href="${manageAvailabilityItemUrl}" class="flex items-center gap-2">
                      <span class="material-symbols-outlined text-base leading-none">event_available</span>
                      <span><c:out value="${manageAvailabilityLabel}" /></span>
                    </a>
                  </li>
                  <li>
                    <c:choose>
                      <c:when test="${item.active}">
                        <form action="${disableItemUrl}" method="post" class="m-0 w-full p-0">
                          <button type="submit" class="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left">
                            <span class="material-symbols-outlined text-base leading-none">visibility_off</span>
                            <span><c:out value="${disableLabel}" /></span>
                          </button>
                        </form>
                      </c:when>
                      <c:otherwise>
                        <form action="${enableItemUrl}" method="post" class="m-0 w-full p-0">
                          <button type="submit" class="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left">
                            <span class="material-symbols-outlined text-base leading-none">visibility</span>
                            <span><c:out value="${enableLabel}" /></span>
                          </button>
                        </form>
                      </c:otherwise>
                    </c:choose>
                  </li>
                  <li>
                    <button
                        type="button"
                        class="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-error"
                        onclick="document.getElementById('${deleteModalId}').showModal()">
                      <span class="material-symbols-outlined text-base leading-none">delete</span>
                      <span><c:out value="${deleteLabel}" /></span>
                    </button>
                  </li>
                </paw:kebabMenu>
              </div>

              <paw:confirmModal
                  id="${deleteModalId}"
                  title="${deleteConfirmTitle}"
                  message="${deleteConfirmMessage}"
                  confirmText="${deleteConfirmConfirm}"
                  cancelText="${deleteConfirmCancel}"
                  confirmColor="danger"
                  icon="delete_forever">
                <form action="${deleteItemUrl}" method="post" class="m-0">
                  <paw:button type="submit" color="danger" cssClass="w-full sm:w-auto" text="${deleteConfirmConfirm}" />
                </form>
              </paw:confirmModal>
            </c:forEach>
          </c:when>
          <c:otherwise>
            <p class="m-0 text-sm text-on-surface-variant"><spring:message code="profile.publications.empty" /></p>
          </c:otherwise>
        </c:choose>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div id="received-booking-requests" class="scroll-mt-24 space-y-4">
          <h2 class="text-xl font-extrabold tracking-tight m-0"><spring:message code="profile.bookings.title" /></h2>
          <c:if test="${param.bookingAction == 'accepted'}">
            <paw:alertMessage type="success"><spring:message code="profile.bookings.accepted" /></paw:alertMessage>
          </c:if>
          <c:if test="${param.bookingAction == 'rejected'}">
            <paw:alertMessage type="warning"><spring:message code="profile.bookings.rejected" /></paw:alertMessage>
          </c:if>
          <c:if test="${param.bookingAction == 'forbidden' || param.bookingAction == 'error' || param.bookingAction == 'notFound'}">
            <paw:alertMessage type="error"><spring:message code="profile.bookings.error" /></paw:alertMessage>
          </c:if>
          <c:if test="${param.paymentAction == 'paid'}">
            <paw:alertMessage type="success"><spring:message code="profile.payment.paid" /></paw:alertMessage>
          </c:if>
          <c:if test="${param.paymentAction == 'confirmError'}">
            <paw:alertMessage type="error"><spring:message code="profile.payment.error" /></paw:alertMessage>
          </c:if>
          <c:choose>
            <c:when test="${not empty receivedBookingRequests}">
              <c:forEach var="receivedRequest" items="${receivedBookingRequests}">
                <c:url var="acceptBookingUrl" value="/bookings/${receivedRequest.id}/accept" />
                <c:url var="declineBookingUrl" value="/bookings/${receivedRequest.id}/decline" />
                <c:url var="receivedPaymentProofUrl" value="/bookings/${receivedRequest.id}/payment-proof" />
                <c:url var="confirmPaymentUrl" value="/bookings/${receivedRequest.id}/payment/confirm" />
                <c:set var="receivedStatusClass" value="badge-ghost" />
                <c:if test="${receivedRequest.statusMessageCode == 'profile.sentBookings.status.pending'}">
                  <c:set var="receivedStatusClass" value="badge-warning" />
                </c:if>
                <c:if test="${receivedRequest.statusMessageCode == 'profile.sentBookings.status.confirmed'}">
                  <c:set var="receivedStatusClass" value="badge-success" />
                </c:if>
                <c:if test="${receivedRequest.statusMessageCode == 'profile.sentBookings.status.rejected' || receivedRequest.statusMessageCode == 'profile.sentBookings.status.cancelled'}">
                  <c:set var="receivedStatusClass" value="badge-error" />
                </c:if>
                <c:if test="${receivedRequest.statusMessageCode == 'profile.sentBookings.status.paymentSubmitted'}">
                  <c:set var="receivedStatusClass" value="badge-info" />
                </c:if>
                <c:if test="${receivedRequest.statusMessageCode == 'profile.sentBookings.status.paid'}">
                  <c:set var="receivedStatusClass" value="badge-success" />
                </c:if>
                <div class="rounded-xl bg-base-200 p-4 space-y-4">
                  <div class="flex items-start justify-between gap-3">
                    <p class="m-0 min-w-0 break-words text-sm font-bold text-on-surface"><c:out value="${receivedRequest.itemTitle}" /></p>
                    <span class="badge ${receivedStatusClass} badge-sm shrink-0 font-bold">
                      <spring:message code="${receivedRequest.statusMessageCode}" />
                    </span>
                  </div>
                  <div class="flex items-start gap-2 text-sm">
                    <span class="material-symbols-outlined mt-0.5 text-base leading-none text-primary">event</span>
                    <div>
                      <p class="m-0 font-bold text-on-surface"><c:out value="${receivedRequest.dateLabel}" /></p>
                      <p class="m-0 text-xs text-on-surface-variant"><c:out value="${receivedRequest.timeRangeLabel}" /></p>
                    </div>
                  </div>
                  <div class="rounded-lg bg-base-100 p-3">
                    <p class="m-0 text-[11px] font-bold uppercase tracking-wider text-outline"><spring:message code="profile.bookings.requester.label" /></p>
                    <p class="m-0 mt-1 break-words text-sm font-bold text-on-surface"><c:out value="${receivedRequest.requesterName}" /></p>
                    <p class="m-0 break-all text-xs text-on-surface-variant"><c:out value="${receivedRequest.requesterEmail}" /></p>
                  </div>
                  <c:if test="${receivedRequest.hasPaymentProof}">
                    <div class="rounded-lg bg-base-100 p-3">
                      <p class="m-0 text-[11px] font-bold uppercase tracking-wider text-outline"><spring:message code="profile.paymentProofs.label" /></p>
                      <a href="${receivedPaymentProofUrl}" class="link link-hover mt-1 block max-w-full break-all text-sm font-bold text-primary">
                        <c:out value="${receivedRequest.paymentProofFileName}" />
                      </a>
                    </div>
                  </c:if>
                  <c:if test="${receivedRequest.statusMessageCode == 'profile.sentBookings.status.pending'}">
                    <div class="flex flex-wrap gap-2 border-t border-outline-variant/20 pt-3">
                      <form action="${acceptBookingUrl}" method="post" class="m-0">
                        <paw:button type="submit" color="success" size="sm" text="${acceptLabel}" />
                      </form>
                      <form action="${declineBookingUrl}" method="post" class="m-0">
                        <paw:button type="submit" color="danger" variant="outline" size="sm" text="${declineLabel}" />
                      </form>
                    </div>
                  </c:if>
                  <c:if test="${receivedRequest.statusMessageCode == 'profile.sentBookings.status.paymentSubmitted' && receivedRequest.hasPaymentProof}">
                    <form action="${confirmPaymentUrl}" method="post" class="m-0 border-t border-outline-variant/20 pt-3">
                      <paw:button type="submit" color="success" size="sm" text="${paymentReceivedLabel}" />
                    </form>
                  </c:if>
                </div>
              </c:forEach>
            </c:when>
            <c:otherwise>
              <p class="m-0 text-sm text-on-surface-variant"><spring:message code="profile.bookings.empty" /></p>
            </c:otherwise>
          </c:choose>
        </div>

        <div id="sent-booking-requests" class="scroll-mt-24 space-y-4">
          <h2 class="text-xl font-extrabold tracking-tight m-0"><spring:message code="profile.sentBookings.title" /></h2>
          <c:if test="${param.paymentAction == 'submitted'}">
            <paw:alertMessage type="success"><spring:message code="profile.payment.submitted" /></paw:alertMessage>
          </c:if>
          <c:if test="${param.paymentAction == 'invalidFile'}">
            <paw:alertMessage type="error"><spring:message code="profile.payment.invalidFile" /></paw:alertMessage>
          </c:if>
          <c:if test="${param.paymentAction == 'forbidden' || param.paymentAction == 'submitError' || param.paymentAction == 'error'}">
            <paw:alertMessage type="error"><spring:message code="profile.payment.error" /></paw:alertMessage>
          </c:if>
          <c:choose>
            <c:when test="${not empty sentBookingRequests}">
              <c:forEach var="sentRequest" items="${sentBookingRequests}">
                <c:url var="sentPaymentProofUrl" value="/bookings/${sentRequest.id}/payment-proof" />
                <c:set var="sentStatusClass" value="badge-ghost" />
                <c:if test="${sentRequest.statusMessageCode == 'profile.sentBookings.status.pending'}">
                  <c:set var="sentStatusClass" value="badge-warning" />
                </c:if>
                <c:if test="${sentRequest.statusMessageCode == 'profile.sentBookings.status.confirmed'}">
                  <c:set var="sentStatusClass" value="badge-success" />
                </c:if>
                <c:if test="${sentRequest.statusMessageCode == 'profile.sentBookings.status.rejected' || sentRequest.statusMessageCode == 'profile.sentBookings.status.cancelled'}">
                  <c:set var="sentStatusClass" value="badge-error" />
                </c:if>
                <c:if test="${sentRequest.statusMessageCode == 'profile.sentBookings.status.paymentSubmitted'}">
                  <c:set var="sentStatusClass" value="badge-info" />
                </c:if>
                <c:if test="${sentRequest.statusMessageCode == 'profile.sentBookings.status.paid'}">
                  <c:set var="sentStatusClass" value="badge-success" />
                </c:if>
                <div class="rounded-xl bg-base-200 p-4 space-y-4">
                  <div class="flex items-start justify-between gap-3">
                    <p class="m-0 min-w-0 break-words text-sm font-bold text-on-surface"><c:out value="${sentRequest.itemTitle}" /></p>
                    <span class="badge ${sentStatusClass} badge-sm shrink-0 font-bold">
                      <spring:message code="${sentRequest.statusMessageCode}" />
                    </span>
                  </div>
                  <div class="flex items-start gap-2 text-sm">
                    <span class="material-symbols-outlined mt-0.5 text-base leading-none text-primary">event</span>
                    <div>
                      <p class="m-0 font-bold text-on-surface"><c:out value="${sentRequest.dateLabel}" /></p>
                      <p class="m-0 text-xs text-on-surface-variant"><c:out value="${sentRequest.timeRangeLabel}" /></p>
                    </div>
                  </div>
                  <div class="rounded-lg bg-base-100 p-3">
                    <p class="m-0 text-[11px] font-bold uppercase tracking-wider text-outline"><spring:message code="profile.sentBookings.owner.label" /></p>
                    <p class="m-0 mt-1 break-words text-sm font-bold text-on-surface"><c:out value="${sentRequest.ownerName}" /></p>
                    <p class="m-0 break-all text-xs text-on-surface-variant"><c:out value="${sentRequest.ownerEmail}" /></p>
                  </div>
                  <c:if test="${sentRequest.statusMessageCode == 'profile.sentBookings.status.confirmed'}">
                    <form action="${sentPaymentProofUrl}" method="post" enctype="multipart/form-data" class="space-y-2 border-t border-outline-variant/20 pt-3">
                      <input type="file" name="file" accept="application/pdf,image/png,image/jpeg,image/webp" class="file-input file-input-bordered file-input-sm w-full" required />
                      <paw:button type="submit" color="primary" size="sm" text="${uploadPaymentProofLabel}" />
                    </form>
                  </c:if>
                  <c:if test="${sentRequest.statusMessageCode == 'profile.sentBookings.status.paymentSubmitted' || sentRequest.statusMessageCode == 'profile.sentBookings.status.paid'}">
                    <a href="${sentPaymentProofUrl}" class="link link-hover block border-t border-outline-variant/20 pt-3 text-sm font-bold text-primary">
                      <spring:message code="profile.sentBookings.paymentProof.view" />
                    </a>
                  </c:if>
                </div>
              </c:forEach>
            </c:when>
            <c:otherwise>
              <p class="m-0 text-sm text-on-surface-variant"><spring:message code="profile.sentBookings.empty" /></p>
            </c:otherwise>
          </c:choose>
        </div>
      </div>

      <form action="${logoutUrl}" method="post">
        <paw:button type="submit" color="secondary" fullWidth="true" text="${logoutLabel}" />
      </form>
    </div>
  </div>
</paw:layout>
