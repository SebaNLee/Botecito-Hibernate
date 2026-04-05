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

<a
    href="${href}"
    class="group block overflow-hidden rounded-card border border-brand-border/60 bg-white shadow-soft transition duration-200 hover:-translate-y-1 ${cardCssClass}"
>
  <div class="aspect-[16/10] overflow-hidden">
    <img
        src="${imageUrl}"
        alt="<c:out value='${name}' />"
        class="h-full w-full object-cover transition duration-300 group-hover:scale-[1.03]"
    />
  </div>
  <div class="space-y-4 p-5">
    <div class="flex items-start justify-between gap-4">
      <div>
        <h3 class="font-display text-lg font-extrabold tracking-tight text-brand-foreground">
          <c:out value="${name}" />
        </h3>
        <p class="metric-chip mt-2"><span>Ubicacion</span> <span><c:out value="${location}" /></span></p>
      </div>
      <div class="text-right">
        <p class="font-display text-2xl font-extrabold text-brand-primary">$<c:out value="${price}" /></p>
        <p class="text-[0.68rem] font-semibold uppercase tracking-[0.14em] text-brand-muted">por hora</p>
      </div>
    </div>
    <div class="flex items-center justify-between gap-3 border-t border-brand-border/70 pt-4 text-sm text-brand-muted">
      <span><c:out value="${capacity}" /> pers.</span>
      <span><c:out value="${weight}" /> kg</span>
      <span class="font-semibold text-brand-primary">Ver detalles &rarr;</span>
    </div>
  </div>
</a>
