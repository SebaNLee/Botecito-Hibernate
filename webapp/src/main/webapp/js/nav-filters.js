(function () {
  const MARKETPLACE_STORAGE_KEY = "paw.marketplaceFilters";
  const MARKETPLACE_DRAFT_KEY = "paw.marketplaceFilterDraft";

  const MARKETPLACE_PARAM_KEYS = [
    "searchQuery",
    "location",
    "itemType",
    "date",
    "startTime",
    "endTime",
    "capacity",
    "weight",
    "difficulty",
    "minAvgRating",
    "sortBy",
    "pageSize",
  ];

  const LIST_PAGE_PRESETS = [
    {
      id: "favourites",
      pathSuffix: "/favourites",
      formSelector: "#favourites-filters-form",
      storageKey: "paw.favouritesFilters",
      paramKeys: ["searchQuery", "sortBy", "pageSize"],
    },
    {
      id: "myBoats",
      pathSuffix: "/my-boats",
      formSelector: "#my-boats-filters-form",
      storageKey: "paw.myBoatsFilters",
      paramKeys: ["searchQuery", "status", "sortBy", "pageSize"],
    },
    {
      id: "requestsOutgoing",
      pathSuffix: "/requests/outgoing",
      formSelector: "#requests-outgoing-filters-form",
      storageKey: "paw.requestsOutgoingFilters",
      draftStorageKey: "paw.requestsOutgoingFilterDraft",
      paramKeys: ["searchQuery", "date", "status", "sortBy", "pageSize"],
      sidebarParamKeys: ["date", "status"],
    },
    {
      id: "requestsIncoming",
      pathSuffix: "/requests/incoming",
      formSelector: "#requests-incoming-filters-form",
      storageKey: "paw.requestsIncomingFilters",
      draftStorageKey: "paw.requestsIncomingFilterDraft",
      paramKeys: ["searchQuery", "date", "status", "sortBy", "pageSize"],
      sidebarParamKeys: ["date", "status"],
    },
    {
      id: "adminReports",
      pathSuffix: "/admin/reports",
      formSelector: "#admin-reports-filters-form",
      storageKey: "paw.adminReportsFilters",
      paramKeys: ["sortBy", "pageSize"],
    },
  ];

  const NAV_LINK_SELECTOR = "[data-nav-filter-page]";
  const APPLY_SIDEBAR_CLASS = "js-apply-list-filters";

  function parseJson(value, fallback) {
    try {
      return value ? JSON.parse(value) : fallback;
    } catch (_error) {
      return fallback;
    }
  }

  function trimParam(value) {
    return (value || "").toString().trim();
  }

  function normalizeParams(state, paramKeys) {
    const normalized = {};
    paramKeys.forEach((key) => {
      normalized[key] = state && state[key] ? trimParam(state[key]) : "";
    });
    return normalized;
  }

  function hasAnyParam(state, paramKeys) {
    return paramKeys.some((key) => trimParam(state[key]));
  }

  function readStoredState(storageKey, paramKeys) {
    try {
      const raw = parseJson(sessionStorage.getItem(storageKey), {}) || {};
      return normalizeParams(raw, paramKeys);
    } catch (_error) {
      return normalizeParams({}, paramKeys);
    }
  }

  function writeStoredState(storageKey, state, paramKeys) {
    const normalized = normalizeParams(state, paramKeys);
    try {
      if (hasAnyParam(normalized, paramKeys)) {
        sessionStorage.setItem(storageKey, JSON.stringify(normalized));
      } else {
        sessionStorage.removeItem(storageKey);
      }
    } catch (_error) {
      // Ignore storage failures.
    }
    return normalized;
  }

  function readUrlParams(paramKeys) {
    const params = new URLSearchParams(window.location.search);
    const state = {};
    paramKeys.forEach((key) => {
      state[key] = params.get(key) || "";
    });
    return normalizeParams(state, paramKeys);
  }

  function readFieldValue(form, key) {
    const optionHidden = form.querySelector(`[data-option-value][name="${key}"]`);
    if (optionHidden) {
      return optionHidden.value;
    }

    const inForm = form.querySelector(`[name="${key}"]`);
    if (inForm) {
      return inForm.value;
    }

    if (form.id) {
      const associated = document.querySelector(
        `[form="${form.id}"][name="${key}"]`,
      );
      if (associated) {
        return associated.value;
      }
    }

    return "";
  }

  function readFormParams(form, paramKeys) {
    if (!form) {
      return normalizeParams({}, paramKeys);
    }
    const state = {};
    paramKeys.forEach((key) => {
      state[key] = readFieldValue(form, key);
    });
    return normalizeParams(state, paramKeys);
  }

  function buildUrlWithParams(baseHref, state, paramKeys) {
    const url = new URL(baseHref, window.location.origin);
    const normalized = normalizeParams(state, paramKeys);

    paramKeys.forEach((key) => {
      if (normalized[key]) {
        url.searchParams.set(key, normalized[key]);
      } else {
        url.searchParams.delete(key);
      }
    });

    return url.pathname + url.search;
  }

  function pathAndSearch(href) {
    const url = new URL(href, window.location.origin);
    return url.pathname + url.search;
  }

  function readStoredMarketplaceFilters() {
    return readStoredState(MARKETPLACE_STORAGE_KEY, MARKETPLACE_PARAM_KEYS);
  }

  function captureMarketplaceFilters() {
    if (!window.location.pathname.endsWith("/marketplace")) {
      return;
    }
    const snapshot = readUrlParams(MARKETPLACE_PARAM_KEYS);
    writeStoredState(MARKETPLACE_STORAGE_KEY, snapshot, MARKETPLACE_PARAM_KEYS);
  }

  function normalizePathname(pathname) {
    if (!pathname) {
      return "/";
    }
    const trimmed = pathname.replace(/\/+$/, "");
    return trimmed || "/";
  }

  function findActiveListPreset() {
    const path = normalizePathname(window.location.pathname);
    return (
      LIST_PAGE_PRESETS.find((preset) =>
        path.endsWith(normalizePathname(preset.pathSuffix)),
      ) || null
    );
  }

  function usesSidebarApply(preset) {
    return Boolean(preset?.draftStorageKey && preset?.sidebarParamKeys?.length);
  }

  function readListPageAppliedOnSubmit(preset, form, applySidebarFilters) {
    const urlState = readUrlParams(preset.paramKeys);
    const formState = readFormParams(form, preset.paramKeys);

    if (!usesSidebarApply(preset) || applySidebarFilters) {
      return formState;
    }

    const applied = { ...formState };
    preset.sidebarParamKeys.forEach((key) => {
      applied[key] = urlState[key] || "";
    });
    return applied;
  }

  function captureActiveListPageFilters() {
    const preset = findActiveListPreset();
    if (!preset) {
      return;
    }
    const snapshot = readUrlParams(preset.paramKeys);
    writeStoredState(preset.storageKey, snapshot, preset.paramKeys);
  }

  function captureCurrentPageFilters() {
    captureMarketplaceFilters();
    captureActiveListPageFilters();
  }

  function storedFiltersForNavPage(pageId) {
    if (pageId === "marketplace") {
      return readStoredMarketplaceFilters();
    }
    const preset = LIST_PAGE_PRESETS.find((entry) => entry.id === pageId);
    if (!preset) {
      return normalizeParams({}, []);
    }
    return readStoredState(preset.storageKey, preset.paramKeys);
  }

  function paramKeysForNavPage(pageId) {
    if (pageId === "marketplace") {
      return MARKETPLACE_PARAM_KEYS;
    }
    const preset = LIST_PAGE_PRESETS.find((entry) => entry.id === pageId);
    return preset ? preset.paramKeys : [];
  }

  function resolveNavHref(link) {
    const pageId = link.getAttribute("data-nav-filter-page");
    const baseHref = link.getAttribute("href") || "";
    if (!pageId || !baseHref) {
      return baseHref;
    }
    const paramKeys = paramKeysForNavPage(pageId);
    const stored = storedFiltersForNavPage(pageId);
    if (!hasAnyParam(stored, paramKeys)) {
      return baseHref;
    }
    return buildUrlWithParams(baseHref, stored, paramKeys);
  }

  function applyNavLinkHrefs() {
    document.querySelectorAll(NAV_LINK_SELECTOR).forEach((link) => {
      const resolved = resolveNavHref(link);
      if (resolved) {
        link.setAttribute("href", resolved);
      }
    });
  }

  function bindNavLinks() {
    document.querySelectorAll(NAV_LINK_SELECTOR).forEach((link) => {
      link.addEventListener(
        "click",
        (event) => {
          captureCurrentPageFilters();
          const targetHref = resolveNavHref(link);
          const currentHref = link.getAttribute("href") || "";
          if (targetHref && pathAndSearch(targetHref) !== pathAndSearch(currentHref)) {
            event.preventDefault();
            window.location.assign(targetHref);
          }
        },
        true,
      );
    });
  }

  function syncListPageStorageFromUrl(preset) {
    const urlState = readUrlParams(preset.paramKeys);
    writeStoredState(preset.storageKey, urlState, preset.paramKeys);
    if (preset.draftStorageKey) {
      writeStoredState(preset.draftStorageKey, urlState, preset.paramKeys);
    }
  }

  function syncListPageUrlFromStorageIfNeeded(preset) {
    const urlState = readUrlParams(preset.paramKeys);
    if (hasAnyParam(urlState, preset.paramKeys)) {
      return false;
    }

    const stored = readStoredState(preset.storageKey, preset.paramKeys);
    if (!hasAnyParam(stored, preset.paramKeys)) {
      return false;
    }

    const target = buildUrlWithParams(window.location.pathname, stored, preset.paramKeys);
    if (pathAndSearch(window.location.href) !== pathAndSearch(target)) {
      window.location.replace(target);
      return true;
    }
    return false;
  }

  function persistApplied() {
    const preset = findActiveListPreset();
    if (!preset) {
      return;
    }
    const form = document.querySelector(preset.formSelector);
    if (!form) {
      return;
    }
    const snapshot = readFormParams(form, preset.paramKeys);
    writeStoredState(preset.storageKey, snapshot, preset.paramKeys);
  }

  function bindClearListFiltersLinks() {
    document.querySelectorAll("[data-clear-list-filters]").forEach((link) => {
      link.addEventListener("click", () => {
        const preset = findActiveListPreset();
        if (!preset) {
          return;
        }
        try {
          sessionStorage.removeItem(preset.storageKey);
          if (preset.draftStorageKey) {
            sessionStorage.removeItem(preset.draftStorageKey);
          }
        } catch (_error) {
          // Ignore storage failures.
        }
      });
    });
  }

  function persistSidebarDraft(preset, form) {
    if (!usesSidebarApply(preset)) {
      return;
    }
    const draft = readFormParams(form, preset.sidebarParamKeys);
    writeStoredState(preset.draftStorageKey, draft, preset.sidebarParamKeys);
  }

  function hydrateSidebarFromDraft(preset) {
    if (!usesSidebarApply(preset)) {
      return;
    }

    const form = document.querySelector(preset.formSelector);
    if (!form) {
      return;
    }

    const urlState = readUrlParams(preset.paramKeys);
    if (hasAnyParam(urlState, preset.sidebarParamKeys)) {
      return;
    }

    const draft = readStoredState(preset.draftStorageKey, preset.sidebarParamKeys);
    if (!hasAnyParam(draft, preset.sidebarParamKeys)) {
      return;
    }

    if (draft.date) {
      const dateRoot = form.querySelector("[data-date-picker]");
      const datePicker = dateRoot?.__datePicker;
      if (datePicker) {
        datePicker.setValue(draft.date);
      } else {
        const dateInput = form.querySelector("[data-picker-input]");
        if (dateInput) {
          dateInput.value = draft.date;
        }
      }
    } else {
      const dateRoot = form.querySelector("[data-date-picker]");
      dateRoot?.__datePicker?.clear();
    }

    const statusPicker = form.querySelector("[data-options-picker]")?.__optionsPicker;
    if (statusPicker) {
      statusPicker.setSelectedValue(draft.status || "");
    }
  }

  function bindSidebarDraftPersistence(preset, form) {
    if (!usesSidebarApply(preset)) {
      return;
    }

    const persistDraft = () => persistSidebarDraft(preset, form);

    const dateInput = form.querySelector("[data-picker-input]");
    if (dateInput) {
      dateInput.addEventListener("change", persistDraft);
    }

    const statusInput = form.querySelector('[data-option-value][name="status"]');
    if (statusInput) {
      statusInput.addEventListener("change", persistDraft);
    }
  }

  function bindListPagePersistence(preset) {
    const form = document.querySelector(preset.formSelector);
    if (!form) {
      return;
    }

    let applySidebarFilters = false;
    const applyButton = form.querySelector("." + APPLY_SIDEBAR_CLASS);
    if (applyButton) {
      applyButton.addEventListener("click", () => {
        applySidebarFilters = true;
      });
    }

    form.addEventListener("submit", (event) => {
      const applySidebar = applySidebarFilters;
      applySidebarFilters = false;
      const snapshot = readListPageAppliedOnSubmit(preset, form, applySidebar);

      writeStoredState(preset.storageKey, snapshot, preset.paramKeys);
      if (preset.draftStorageKey) {
        writeStoredState(preset.draftStorageKey, snapshot, preset.paramKeys);
      }

      if (!usesSidebarApply(preset) || applySidebar) {
        return;
      }

      event.preventDefault();
      const action = form.getAttribute("action") || window.location.pathname;
      const target = new URL(action, window.location.origin);
      preset.paramKeys.forEach((key) => {
        if (snapshot[key]) {
          target.searchParams.set(key, snapshot[key]);
        } else {
          target.searchParams.delete(key);
        }
      });
      const page = trimParam(readFieldValue(form, "page"));
      if (page) {
        target.searchParams.set("page", page);
      } else {
        target.searchParams.delete("page");
      }
      window.location.assign(target.toString());
    });

    if (usesSidebarApply(preset)) {
      bindSidebarDraftPersistence(preset, form);
      return;
    }

    form.addEventListener("change", persistApplied);
  }

  function initializeListPages() {
    const preset = findActiveListPreset();
    if (!preset) {
      return;
    }
    if (syncListPageUrlFromStorageIfNeeded(preset)) {
      return;
    }
    syncListPageStorageFromUrl(preset);
    bindListPagePersistence(preset);
    bindClearListFiltersLinks();
    if (usesSidebarApply(preset)) {
      setTimeout(() => hydrateSidebarFromDraft(preset), 0);
    }
  }

  document.addEventListener("DOMContentLoaded", () => {
    applyNavLinkHrefs();
    bindNavLinks();
    initializeListPages();
  });

  window.addEventListener("pageshow", (event) => {
    if (!event.persisted) {
      return;
    }
    applyNavLinkHrefs();
    const preset = findActiveListPreset();
    if (preset && usesSidebarApply(preset)) {
      setTimeout(() => hydrateSidebarFromDraft(preset), 0);
    }
  });
})();
