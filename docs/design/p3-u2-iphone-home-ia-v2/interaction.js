(() => {
  "use strict";

  const documentElement = document.documentElement;
  const shell = document.querySelector(".device-shell");
  const appScroll = document.querySelector(".app-scroll");
  const assetButtons = Array.from(document.querySelectorAll("[data-asset-index]"));
  const roleTabs = Array.from(document.querySelectorAll("[data-role]"));
  const rolePanels = Array.from(document.querySelectorAll("[data-role-panel]"));
  const positionSection = document.querySelector("[data-position-independent]");
  const appTitle = document.querySelector("#app-title");
  const homeNav = document.querySelector("[data-home-nav]");
  const positionNav = document.querySelector("[data-position-nav]");
  const reviewNav = document.querySelector("[data-review-nav]");
  const linkedSections = [
    document.querySelector("#execution-advice"),
    document.querySelector("#ai-review")
  ].filter(Boolean);
  const params = new URLSearchParams(window.location.search);

  const allowed = {
    device: new Set(["17pm", "12pm"]),
    theme: new Set(["light", "dark"]),
    text: new Set(["standard", "large"])
  };

  const normalized = (name, fallback) => {
    const value = params.get(name);
    return allowed[name].has(value) ? value : fallback;
  };

  let selectedAssetIndex = 0;
  let activeRole = "gpt";
  let initialPositionMarkup = "";
  let captureEmptyStates = new Map();
  const captureUsesSafeEmptyState = params.get("capture") === "1";

  const setCaptureContract = (status) => {
    documentElement.dataset.captureContract = status;
    document.body.dataset.captureContract = status;
  };

  const renderCaptureFailure = () => {
    const failure = document.createElement("main");
    const content = document.createElement("section");
    const label = document.createElement("p");
    const title = document.createElement("h1");
    const message = document.createElement("p");

    failure.className = "capture-failure";
    failure.dataset.captureFailure = "field-map-unavailable";
    failure.setAttribute("role", "alert");
    content.className = "capture-failure-content";
    label.className = "fixture-label";
    label.textContent = "STATIC_LAYOUT_FIXTURE";
    title.textContent = "原型数据不可用";
    message.textContent = "字段映射加载失败，未展示未初始化内容。";
    content.append(label, title, message);
    failure.append(content);
    document.body.replaceChildren(failure);
    setCaptureContract("error");
  };

  const fieldPathCandidates = (fieldPath) => Array.from(new Set([
    fieldPath,
    fieldPath.replace(/\[\d+\]/g, "[]"),
    fieldPath.replace(/\[[^\]]+\]/g, "[]")
  ]));

  const buildCaptureEmptyStates = (fieldMap) => {
    if (!fieldMap || !Array.isArray(fieldMap.fields)) {
      throw new Error("Capture-mode field map is invalid");
    }

    const emptyStates = new Map();
    fieldMap.fields.forEach((field) => {
      if (typeof field.backendField !== "string" || typeof field.emptyState !== "string") {
        return;
      }
      field.backendField.split(/\s+\/\s+/).forEach((fieldPath) => {
        const normalizedPath = fieldPath.trim();
        if (!normalizedPath) {
          return;
        }
        const mappedStates = emptyStates.get(normalizedPath) || new Set();
        mappedStates.add(field.emptyState);
        emptyStates.set(normalizedPath, mappedStates);
      });
    });
    return emptyStates;
  };

  const loadCaptureEmptyStates = async () => {
    const response = await fetch("field-map.json", {
      cache: "no-store",
      headers: { Accept: "application/json" }
    });
    if (!response.ok) {
      throw new Error(`Capture-mode field map request failed: ${response.status}`);
    }
    return buildCaptureEmptyStates(await response.json());
  };

  const fixtureFallback = (fieldPath) => {
    const matchedPath = fieldPathCandidates(fieldPath)
      .find((candidate) => captureEmptyStates.has(candidate));
    if (!matchedPath) {
      throw new Error(`Capture-mode field has no mapped empty state: ${fieldPath}`);
    }
    const mappedStates = captureEmptyStates.get(matchedPath);
    if (mappedStates.size !== 1) {
      throw new Error(`Capture-mode field has conflicting empty states: ${fieldPath}`);
    }
    return mappedStates.values().next().value;
  };

  const applyCaptureSafeEmptyState = () => {
    if (!captureUsesSafeEmptyState) {
      return;
    }
    const tokenPattern = /\{([^{}]+)\}/g;
    const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
    const textNodes = [];
    while (walker.nextNode()) {
      textNodes.push(walker.currentNode);
    }
    textNodes.forEach((textNode) => {
      const source = textNode.nodeValue || "";
      const fieldPaths = Array.from(source.matchAll(tokenPattern), (match) => match[1]);
      if (fieldPaths.length === 0) {
        return;
      }
      textNode.parentElement?.setAttribute("data-field-token", fieldPaths.join(","));
      textNode.nodeValue = source.replace(tokenPattern, (_token, fieldPath) => fixtureFallback(fieldPath));
    });
  };

  const validateCaptureSafeEmptyState = () => {
    if (!captureUsesSafeEmptyState || !positionSection) {
      return;
    }
    if (/\{[^{}]+\}/.test(positionSection.textContent || "")) {
      throw new Error("Capture-mode position safe-empty normalization failed");
    }
  };

  const setPressed = (selector, value, attribute) => {
    document.querySelectorAll(selector).forEach((button) => {
      button.setAttribute("aria-pressed", String(button.getAttribute(attribute) === value));
    });
  };

  const updateQuery = (name, value) => {
    if (document.body.classList.contains("capture-mode")) {
      return;
    }
    const next = new URL(window.location.href);
    next.searchParams.set(name, value);
    window.history.replaceState({}, "", next);
  };

  const applyDevice = (device) => {
    shell.dataset.device = device;
    setPressed("[data-device-control]", device, "data-device-control");
    updateQuery("device", device);
  };

  const applyTheme = (theme) => {
    documentElement.dataset.theme = theme;
    setPressed("[data-theme-control]", theme, "data-theme-control");
    updateQuery("theme", theme);
  };

  const applyTextSize = (textSize) => {
    documentElement.dataset.textSize = textSize;
    setPressed("[data-text-control]", textSize, "data-text-control");
    updateQuery("text", textSize);
  };

  const assetToken = (index) => `{assets[${index}].symbol}`;

  const selectAsset = (index, scrollCard = true) => {
    if (!Number.isInteger(index) || index < 0 || index >= assetButtons.length) {
      return;
    }

    selectedAssetIndex = index;
    assetButtons.forEach((button) => {
      const selected = Number(button.dataset.assetIndex) === index;
      button.setAttribute("aria-checked", String(selected));
      button.tabIndex = selected ? 0 : -1;
      button.setAttribute("data-selected", String(selected));
    });

    document.querySelectorAll("[data-selected-asset-token]").forEach((node) => {
      const selectedAssetField = assetToken(index).slice(1, -1);
      node.dataset.fieldToken = selectedAssetField;
      node.textContent = captureUsesSafeEmptyState
        ? fixtureFallback(selectedAssetField)
        : assetToken(index);
    });
    linkedSections.forEach((section) => {
      section.dataset.selectedAssetIndex = String(index);
    });

    if (scrollCard) {
      assetButtons[index].closest(".asset-card")?.scrollIntoView({
        behavior: "smooth",
        block: "nearest",
        inline: "center"
      });
    }
  };

  const selectRole = (role, moveFocus = false) => {
    const selectedTab = roleTabs.find((tab) => tab.dataset.role === role);
    if (!selectedTab) {
      return;
    }

    activeRole = role;
    roleTabs.forEach((tab) => {
      const selected = tab === selectedTab;
      tab.setAttribute("aria-selected", String(selected));
      tab.tabIndex = selected ? 0 : -1;
    });
    rolePanels.forEach((panel) => {
      panel.hidden = panel.dataset.rolePanel !== role;
    });

    if (moveFocus) {
      selectedTab.focus();
    }
  };

  document.querySelectorAll("[data-device-control]").forEach((button) => {
    button.addEventListener("click", () => applyDevice(button.dataset.deviceControl));
  });
  document.querySelectorAll("[data-theme-control]").forEach((button) => {
    button.addEventListener("click", () => applyTheme(button.dataset.themeControl));
  });
  document.querySelectorAll("[data-text-control]").forEach((button) => {
    button.addEventListener("click", () => applyTextSize(button.dataset.textControl));
  });

  assetButtons.forEach((button) => {
    button.addEventListener("click", () => selectAsset(Number(button.dataset.assetIndex)));
    button.addEventListener("keydown", (event) => {
      if (event.key !== "ArrowLeft" && event.key !== "ArrowRight") {
        return;
      }
      event.preventDefault();
      const direction = event.key === "ArrowRight" ? 1 : -1;
      const next = (Number(button.dataset.assetIndex) + direction + assetButtons.length) % assetButtons.length;
      selectAsset(next);
      assetButtons[next].focus();
    });
  });

  roleTabs.forEach((tab, index) => {
    tab.addEventListener("click", () => selectRole(tab.dataset.role));
    tab.addEventListener("keydown", (event) => {
      if (event.key !== "ArrowLeft" && event.key !== "ArrowRight") {
        return;
      }
      event.preventDefault();
      const direction = event.key === "ArrowRight" ? 1 : -1;
      const next = (index + direction + roleTabs.length) % roleTabs.length;
      selectRole(roleTabs[next].dataset.role, true);
    });
  });

  const setCurrentNavigation = (current) => {
    document.querySelectorAll(".bottom-nav a").forEach((item) => item.removeAttribute("aria-current"));
    current?.setAttribute("aria-current", "page");
  };

  homeNav?.addEventListener("click", (event) => {
    event.preventDefault();
    appScroll.scrollTo({ top: 0, behavior: "auto" });
    appTitle?.focus({ preventScroll: true });
    document.body.dataset.navigationTarget = "/dashboard";
    setCurrentNavigation(homeNav);
  });

  positionNav?.addEventListener("click", (event) => {
    event.preventDefault();
    positionSection?.scrollIntoView({ block: "start", behavior: "auto" });
    document.body.dataset.navigationTarget = "#position-monitor";
    setCurrentNavigation(positionNav);
  });

  reviewNav?.addEventListener("click", (event) => {
    event.preventDefault();
    document.body.dataset.navigationTarget = "/review/dashboard";
    setCurrentNavigation(reviewNav);
  });

  document.querySelectorAll(".bottom-nav a").forEach((link) => {
    link.addEventListener("click", () => setCurrentNavigation(link));
  });

  const runContractChecks = () => {
    const markup = document.documentElement.outerHTML;
    const nestedInteractiveControlCount = document.querySelectorAll(
      '[role="radio"] button, [role="radio"] a, [role="radio"] summary, [role="radio"] details'
    ).length;
    const touchTargets = Array.from(document.querySelectorAll(
      ".asset-select, summary, .bottom-nav a, .route-unresolved, .manual-action"
    ));
    const undersizedTouchTargets = touchTargets.filter((node) => {
      if (node.getClientRects().length === 0) {
        return false;
      }
      const rect = node.getBoundingClientRect();
      return rect.width < 44 || rect.height < 44;
    });
    return Object.freeze({
      invalidConsistencyPlanModePath: markup.includes(
        ["aiDecision", "consistency", "finalPlanMode"].join(".")
      ),
      invalidAssetTokenCount: (markup.match(/\{asset\[\d+\]\./g) || []).length,
      reviewNavigationTarget: reviewNav?.getAttribute("href") || null,
      positionViewAllSelfLink: Boolean(document.querySelector('.position-section a[href="#position-monitor"]')),
      nestedInteractiveControlCount,
      undersizedTouchTargetCount: undersizedTouchTargets.length,
      unresolvedPositionRoute: Boolean(document.querySelector(".route-unresolved[disabled]"))
    });
  };

  const initializePrototype = () => {
    applyCaptureSafeEmptyState();
    validateCaptureSafeEmptyState();
    initialPositionMarkup = positionSection ? positionSection.innerHTML : "";
    applyDevice(normalized("device", "17pm"));
    applyTheme(normalized("theme", "light"));
    applyTextSize(normalized("text", "standard"));
    selectAsset(0, false);
    selectRole("gpt");

    const prototypeApi = Object.freeze({
      selectAsset,
      selectRole,
      getState: () => ({
        device: shell.dataset.device,
        theme: documentElement.dataset.theme,
        textSize: documentElement.dataset.textSize,
        selectedAssetIndex,
        activeRole,
        visibleRoleCount: rolePanels.filter((panel) => !panel.hidden).length,
        executionAssetIndex: document.querySelector("#execution-advice")?.dataset.selectedAssetIndex,
        aiAssetIndex: document.querySelector("#ai-review")?.dataset.selectedAssetIndex,
        positionMarkupUnchanged: !positionSection || positionSection.innerHTML === initialPositionMarkup,
        horizontalOverflow: appScroll.scrollWidth > appScroll.clientWidth,
        appScrollTop: appScroll.scrollTop,
        navigationTarget: document.body.dataset.navigationTarget || null
      }),
      runContractChecks
    });
    if (Object.isExtensible(document)) {
      document.P3U2Prototype = prototypeApi;
    }
    if (Object.isExtensible(window)) {
      window.P3U2Prototype = prototypeApi;
    }
    setCaptureContract(captureUsesSafeEmptyState ? "ready" : "not-requested");
    return prototypeApi;
  };

  if (captureUsesSafeEmptyState) {
    document.body.classList.add("capture-mode");
    setCaptureContract("loading");
  }

  const prototypeReady = captureUsesSafeEmptyState
    ? loadCaptureEmptyStates().then((emptyStates) => {
      captureEmptyStates = emptyStates;
      return initializePrototype();
    })
    : Promise.resolve(initializePrototype());

  if (Object.isExtensible(document)) {
    document.P3U2PrototypeReady = prototypeReady;
  }
  if (Object.isExtensible(window)) {
    window.P3U2PrototypeReady = prototypeReady;
  }
  prototypeReady.catch((error) => {
    renderCaptureFailure();
    console.error("Capture-mode initialization failed", error);
  });
})();
