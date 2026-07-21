(() => {
  "use strict";

  const documentElement = document.documentElement;
  const shell = document.querySelector(".device-shell");
  const appScroll = document.querySelector(".app-scroll");
  const assetButtons = Array.from(document.querySelectorAll("[data-asset-index]"));
  const roleTabs = Array.from(document.querySelectorAll("[data-role]"));
  const rolePanels = Array.from(document.querySelectorAll("[data-role-panel]"));
  const positionSection = document.querySelector("[data-position-independent]");
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
  const initialPositionMarkup = positionSection ? positionSection.innerHTML : "";

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

  const assetToken = (index) => `{asset[${index}].symbol}`;

  const selectAsset = (index, scrollCard = true) => {
    if (!Number.isInteger(index) || index < 0 || index >= assetButtons.length) {
      return;
    }

    selectedAssetIndex = index;
    assetButtons.forEach((button) => {
      const selected = Number(button.dataset.assetIndex) === index;
      button.setAttribute("aria-selected", String(selected));
      button.closest(".asset-card")?.setAttribute("data-selected", String(selected));
    });

    document.querySelectorAll("[data-selected-asset-token]").forEach((node) => {
      node.textContent = assetToken(index);
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

  document.querySelectorAll(".bottom-nav a").forEach((link) => {
    link.addEventListener("click", () => {
      document.querySelectorAll(".bottom-nav a").forEach((item) => item.removeAttribute("aria-current"));
      link.setAttribute("aria-current", "page");
    });
  });

  if (params.get("capture") === "1") {
    document.body.classList.add("capture-mode");
  }
  applyDevice(normalized("device", "17pm"));
  applyTheme(normalized("theme", "light"));
  applyTextSize(normalized("text", "standard"));
  selectAsset(0, false);
  selectRole("gpt");

  window.P3U2Prototype = Object.freeze({
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
      horizontalOverflow: appScroll.scrollWidth > appScroll.clientWidth
    })
  });
})();
