<%@ tag language="java" pageEncoding="UTF-8" body-content="scriptless" %>
<%@ attribute name="id" required="true" %>
<%@ attribute name="ariaLabel" required="false" %>
<%@ attribute name="alignEnd" required="false" type="java.lang.Boolean" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="kebabAlignEnd" value="${alignEnd ne null ? alignEnd : true}" />
<c:set var="kebabAlignClass" value="${kebabAlignEnd ? 'dropdown-end' : ''}" />
<c:set var="kebabAriaLabel" value="${not empty ariaLabel ? ariaLabel : 'Actions'}" />

<div class="dropdown ${kebabAlignClass} relative focus-within:z-[1000]">
  <button
      type="button"
      id="${id}"
      tabindex="0"
      aria-haspopup="menu"
      aria-label="${kebabAriaLabel}"
      class="btn btn-ghost btn-sm btn-circle">
    <span class="material-symbols-outlined text-xl">more_vert</span>
  </button>
  <ul
      tabindex="0"
      role="menu"
      aria-labelledby="${id}"
      class="dropdown-content menu menu-sm z-[1000] mt-2 w-52 rounded-box bg-base-100 p-2 shadow-xl">
    <jsp:doBody />
  </ul>
</div>
