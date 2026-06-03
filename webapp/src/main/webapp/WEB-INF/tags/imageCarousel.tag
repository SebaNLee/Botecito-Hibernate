<%@ tag language="java" pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="imageUrls" required="true" type="java.util.List" %>
<%@ attribute name="altText" required="false" %>
<%@ attribute name="aspectClass" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<c:set var="resolvedAspect" value="${not empty aspectClass ? aspectClass : 'aspect-[16/10]'}" />
<c:set var="imageCount" value="${empty imageUrls ? 0 : fn:length(imageUrls)}" />
<spring:message code="carousel.previous" var="carouselPrevLabel" />
<spring:message code="carousel.next" var="carouselNextLabel" />
<spring:message code="carousel.thumbnail" var="carouselThumbLabel" />

<c:if test="${imageCount > 0}">
  <div class="space-y-3" data-image-carousel>
    <div class="relative card bg-base-100 shadow-sm overflow-hidden">
      <img src="${imageUrls[0]}"
           alt="$<c:out value="${altText}" />"
           class="w-full ${resolvedAspect} object-cover block"
           data-carousel-main />
      <c:if test="${imageCount > 1}">
        <button type="button"
                class="btn btn-circle btn-sm absolute left-3 top-1/2 -translate-y-1/2 bg-base-100/90 hover:bg-base-100 border-0 shadow-md"
                aria-label="${carouselPrevLabel}"
                data-carousel-prev>
          <span class="material-symbols-outlined">chevron_left</span>
        </button>
        <button type="button"
                class="btn btn-circle btn-sm absolute right-3 top-1/2 -translate-y-1/2 bg-base-100/90 hover:bg-base-100 border-0 shadow-md"
                aria-label="${carouselNextLabel}"
                data-carousel-next>
          <span class="material-symbols-outlined">chevron_right</span>
        </button>
        <div class="absolute bottom-3 right-3 badge badge-neutral text-xs font-bold" data-carousel-counter>
          <span data-carousel-index>1</span>/<c:out value="${imageCount}" />
        </div>
      </c:if>
    </div>

    <c:if test="${imageCount > 1}">
      <div class="flex gap-2 overflow-x-auto pb-1">
        <c:forEach var="url" items="${imageUrls}" varStatus="loop">
          <button type="button"
                  class="shrink-0 rounded-lg overflow-hidden border-2 transition-colors ${loop.first ? 'border-primary' : 'border-transparent hover:border-outline-variant'}"
                  data-carousel-thumb
                  data-carousel-index-value="${loop.index}"
                  data-carousel-url="${url}"
                  aria-label="${carouselThumbLabel}">
            <img src="${url}" alt="" class="w-20 h-20 object-cover block" />
          </button>
        </c:forEach>
      </div>
    </c:if>
  </div>
</c:if>
