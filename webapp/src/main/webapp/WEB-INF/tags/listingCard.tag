<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ attribute name="href" required="true" %>
<%@ attribute name="name" required="true" %>
<%@ attribute name="price" required="true" %>
<%@ attribute name="location" required="true" %>
<%@ attribute name="capacity" required="true" %>
<%@ attribute name="weight" required="true" %>
<%@ attribute name="imageUrl" required="true" %>
<%@ attribute name="cssClass" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="cardCssClass" value="${not empty cssClass ? cssClass : ''}" />

<a href="${href}" class="card bg-base-100 shadow-sm overflow-hidden no-underline text-base-content transition duration-200 hover:-translate-y-1 hover:shadow-md ${cardCssClass}">
  <figure class="aspect-[16/10] overflow-hidden">
    <img
        src="${imageUrl}"
        alt="<c:out value='${name}' />"
        class="h-full w-full object-cover transition duration-300 group-hover:scale-[1.03]" />
  </figure>
  <div class="card-body p-5 gap-3">
    <div class="flex items-start justify-between gap-4">
      <div>
        <h3 class="card-title font-headline text-lg font-extrabold tracking-tight m-0">
          <c:out value="${name}" />
        </h3>
        <p class="text-sm text-on-surface-variant mt-1 m-0"><c:out value="${location}" /></p>
      </div>
      <div class="text-right">
        <p class="font-headline text-2xl font-extrabold text-primary m-0">$<c:out value="${price}" /></p>
        <p class="text-[0.68rem] font-semibold uppercase tracking-[0.14em] text-outline m-0">por hora</p>
      </div>
    </div>
    <div class="card-actions flex items-center justify-between gap-3 border-t border-outline-variant/40 pt-4 text-sm text-on-surface-variant">
      <span><c:out value="${capacity}" /> pers.</span>
      <span><c:out value="${weight}" /> kg</span>
      <span class="font-semibold text-primary">Ver detalles &rarr;</span>
    </div>
  </div>
</a>
