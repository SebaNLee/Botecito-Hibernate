<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<paw:layout
  title="Botecito"
  mainClass="pt-24 pb-14 max-w-3xl mx-auto px-6"
  headerCtaMessageCode="nav.rent"
  headerCtaHref="/marketplace"
  headerCtaVariant="rent">
  <div class="flex flex-col items-center justify-center min-h-[40vh] gap-4 text-center">
    <span class="loading loading-spinner loading-lg text-secondary"></span>
    <p class="text-on-surface-variant m-0"><spring:message code="editPublication.loading" /></p>
  </div>
  <script id="edit-draft-bootstrap" type="application/json">${draftJson}</script>
  <script>
    (function () {
      var storageKey = "botecito.editDraft.v1";
      localStorage.removeItem(storageKey);
      var node = document.getElementById("edit-draft-bootstrap");
      if (!node) {
        window.location.assign("<c:url value='/my-boats' />");
        return;
      }
      try {
        var draft = JSON.parse(node.textContent || "null");
        if (!draft) {
          throw new Error("missing draft");
        }
        localStorage.setItem(storageKey, JSON.stringify(draft));
        window.location.assign("<c:url value='${detailsUrl}' />");
      } catch (_e) {
        window.location.assign("<c:url value='/my-boats' />");
      }
    })();
  </script>
</paw:layout>
