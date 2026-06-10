(function () {
  const UI_LOCALE = document.documentElement.lang || navigator.language || "es";

  const FORMATTERS = {
    date: new Intl.DateTimeFormat(UI_LOCALE, {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    }),
    datetime: new Intl.DateTimeFormat(UI_LOCALE, {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      hourCycle: "h23",
    }),
    "datetime-medium": new Intl.DateTimeFormat(UI_LOCALE, {
      day: "2-digit",
      month: "short",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      hourCycle: "h23",
    }),
  };

  function parseUtc(value) {
    if (!value) {
      return null;
    }
    const trimmed = String(value).trim();
    if (!trimmed) {
      return null;
    }
    if (trimmed.endsWith("Z") || /[+-]\d{2}:\d{2}$/.test(trimmed)) {
      const zoned = new Date(trimmed);
      return Number.isNaN(zoned.getTime()) ? null : zoned;
    }
    const utc = new Date(trimmed + "Z");
    return Number.isNaN(utc.getTime()) ? null : utc;
  }

  function formatUtcElement(element) {
    const raw = element.getAttribute("data-utc-datetime");
    const formatKey = element.getAttribute("data-utc-format") || "date";
    const formatter = FORMATTERS[formatKey] || FORMATTERS.date;
    const date = parseUtc(raw);
    if (!date) {
      return;
    }
    element.textContent = formatter.format(date);
  }

  function formatAll(root) {
    root.querySelectorAll("[data-utc-datetime]").forEach(formatUtcElement);
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", function () {
      formatAll(document);
    });
  } else {
    formatAll(document);
  }
})();
