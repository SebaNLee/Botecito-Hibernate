<%@ tag language="java" pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="bundles" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="resolvedBundles" value=",${bundles}," />

<script src="<c:url value='/js/dismissible-alerts.js' />"></script>
<script src="<c:url value='/js/utc-datetime.js' />"></script>
<script src="<c:url value='/js/nav-filters.js' />"></script>
<script src="<c:url value='/js/search-bar.js' />"></script>

<c:if test="${fn:contains(resolvedBundles, ',toast,')}">
  <script src="<c:url value='/js/toast.js' />"></script>
</c:if>
<c:if test="${fn:contains(resolvedBundles, ',date-time,')}">
  <script type="module" src="<c:url value='/js/cally-loader.js' />"></script>
  <script src="<c:url value='/js/date-time-picker.js' />"></script>
</c:if>
<c:if test="${fn:contains(resolvedBundles, ',search-filters,')}">
  <script src="<c:url value='/js/search-filters.js' />"></script>
</c:if>
<c:if test="${fn:contains(resolvedBundles, ',pre-booking-draft,')}">
  <script src="<c:url value='/js/pre-booking-draft.js' />"></script>
</c:if>
<c:if test="${fn:contains(resolvedBundles, ',publish-wizard,')}">
  <script src="<c:url value='/js/publish-wizard.js?v=4' />"></script>
</c:if>
<c:if test="${fn:contains(resolvedBundles, ',form-submit,') or fn:contains(resolvedBundles, ',image-gallery,') or fn:contains(resolvedBundles, ',edit-wizard,')}">
  <script src="<c:url value='/js/gallery-file-error.js' />"></script>
</c:if>
<c:if test="${fn:contains(resolvedBundles, ',edit-wizard,')}">
  <script src="<c:url value='/js/edit-wizard.js?v=10' />"></script>
</c:if>
<c:if test="${fn:contains(resolvedBundles, ',form-submit,')}">
  <script src="<c:url value='/js/form-submit-state.js' />"></script>
</c:if>
<c:if test="${fn:contains(resolvedBundles, ',weekly-availability,')}">
  <script src="<c:url value='/js/weekly-availability.js?v=2' />"></script>
</c:if>
<c:if test="${fn:contains(resolvedBundles, ',image-gallery,')}">
  <script src="<c:url value='/js/image-gallery.js?v=12' />"></script>
</c:if>
<c:if test="${fn:contains(resolvedBundles, ',image-carousel,')}">
  <script src="<c:url value='/js/image-carousel.js' />"></script>
</c:if>
<c:if test="${fn:contains(resolvedBundles, ',rating-stars,')}">
  <script src="<c:url value='/js/rating-stars.js' />"></script>
</c:if>
<c:if test="${fn:contains(resolvedBundles, ',manage-availability,')}">
  <script src="<c:url value='/js/manage-availability-date.js' />"></script>
  <script src="<c:url value='/js/manage-availability-timeline.js' />"></script>
</c:if>
