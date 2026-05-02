<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ attribute name="path" required="true" %>
<%@ attribute name="label" required="true" %>
<%@ attribute name="type" required="false" %>
<%@ attribute name="placeholder" required="false" %>
<%@ attribute name="min" required="false" %>
<%@ attribute name="max" required="false" %>
<%@ attribute name="step" required="false" %>
<%@ attribute name="maxlength" required="false" %>
<%@ attribute name="required" required="false" type="java.lang.Boolean" %>
<%@ attribute name="readonly" required="false" type="java.lang.Boolean" %>
<%@ attribute name="cssClass" required="false" %>
<%@ attribute name="containerClass" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<c:set var="isReadonly" value="${readonly ne null and readonly}" />
<c:set var="inputType" value="${not empty type ? type : 'text'}" />
<c:set var="inputCssClass" value="input w-full ${isReadonly ? 'cursor-default bg-base-200/30' : ''} ${not empty cssClass ? cssClass : ''}" />
<c:set var="inputErrorCssClass" value="${inputCssClass} input-error" />
<c:set var="fieldContainerClass" value="fieldset ${not empty containerClass ? containerClass : ''}" />

<fieldset class="${fieldContainerClass}">
  <legend class="fieldset-legend text-xs font-semibold uppercase tracking-wider text-on-surface-variant">
    <c:out value="${label}" />
    <c:if test="${required ne null and required}"><span class="text-error" aria-hidden="true">*</span></c:if>
  </legend>
  <c:choose>
    <c:when test="${inputType == 'password'}">
      <c:choose>
        <c:when test="${isReadonly}">
          <form:password
              id="${path}"
              path="${path}"
              cssClass="${inputCssClass}"
              cssErrorClass="${inputErrorCssClass}"
              placeholder="${placeholder}"
              readonly="true"
          />
        </c:when>
        <c:otherwise>
          <form:password
              id="${path}"
              path="${path}"
              cssClass="${inputCssClass}"
              cssErrorClass="${inputErrorCssClass}"
              placeholder="${placeholder}"
          />
        </c:otherwise>
      </c:choose>
    </c:when>
    <c:otherwise>
      <c:choose>
        <c:when test="${inputType == 'number' && not empty min && not empty max}">
          <c:choose>
            <c:when test="${isReadonly}">
              <form:input
                  id="${path}"
                  path="${path}"
                  type="number"
                  min="${min}"
                  max="${max}"
                  step="${not empty step ? step : '1'}"
                  cssClass="${inputCssClass}"
                  cssErrorClass="${inputErrorCssClass}"
                  placeholder="${placeholder}"
                  readonly="true"
              />
            </c:when>
            <c:otherwise>
              <form:input
                  id="${path}"
                  path="${path}"
                  type="number"
                  min="${min}"
                  max="${max}"
                  step="${not empty step ? step : '1'}"
                  cssClass="${inputCssClass}"
                  cssErrorClass="${inputErrorCssClass}"
                  placeholder="${placeholder}"
              />
            </c:otherwise>
          </c:choose>
        </c:when>
        <c:when test="${inputType == 'number' && not empty min}">
          <c:choose>
            <c:when test="${isReadonly}">
              <form:input
                  id="${path}"
                  path="${path}"
                  type="number"
                  min="${min}"
                  step="${not empty step ? step : '1'}"
                  cssClass="${inputCssClass}"
                  cssErrorClass="${inputErrorCssClass}"
                  placeholder="${placeholder}"
                  readonly="true"
              />
            </c:when>
            <c:otherwise>
              <form:input
                  id="${path}"
                  path="${path}"
                  type="number"
                  min="${min}"
                  step="${not empty step ? step : '1'}"
                  cssClass="${inputCssClass}"
                  cssErrorClass="${inputErrorCssClass}"
                  placeholder="${placeholder}"
              />
            </c:otherwise>
          </c:choose>
        </c:when>
        <c:otherwise>
          <c:choose>
            <c:when test="${not empty maxlength}">
              <c:choose>
                <c:when test="${isReadonly}">
                  <form:input
                      id="${path}"
                      path="${path}"
                      type="${inputType}"
                      maxlength="${maxlength}"
                      cssClass="${inputCssClass}"
                      cssErrorClass="${inputErrorCssClass}"
                      placeholder="${placeholder}"
                      readonly="true"
                  />
                </c:when>
                <c:otherwise>
                  <form:input
                      id="${path}"
                      path="${path}"
                      type="${inputType}"
                      maxlength="${maxlength}"
                      cssClass="${inputCssClass}"
                      cssErrorClass="${inputErrorCssClass}"
                      placeholder="${placeholder}"
                  />
                </c:otherwise>
              </c:choose>
            </c:when>
            <c:otherwise>
              <c:choose>
                <c:when test="${isReadonly}">
                  <form:input
                      id="${path}"
                      path="${path}"
                      type="${inputType}"
                      cssClass="${inputCssClass}"
                      cssErrorClass="${inputErrorCssClass}"
                      placeholder="${placeholder}"
                      readonly="true"
                  />
                </c:when>
                <c:otherwise>
                  <form:input
                      id="${path}"
                      path="${path}"
                      type="${inputType}"
                      cssClass="${inputCssClass}"
                      cssErrorClass="${inputErrorCssClass}"
                      placeholder="${placeholder}"
                  />
                </c:otherwise>
              </c:choose>
            </c:otherwise>
          </c:choose>
        </c:otherwise>
      </c:choose>
    </c:otherwise>
  </c:choose>
  <form:errors path="${path}" cssClass="text-error text-xs mt-1" element="p" />
</fieldset>
