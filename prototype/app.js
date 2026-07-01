function setActiveInGroup(elements, activeElement) {
  elements.forEach((element) => element.classList.toggle("active", element === activeElement));
}

function updateSwitcherActive() {
  const sections = [...document.querySelectorAll("[id]")].filter((section) =>
    ["dashboard", "reader", "feeds", "parser", "mobile"].includes(section.id),
  );
  const switcherLinks = [...document.querySelectorAll(".prototype-switcher a")];
  const viewportLeft = window.scrollX + 160;
  const activeSection =
    sections.find((section) => section.offsetLeft + section.offsetWidth > viewportLeft) ?? sections[0];

  switcherLinks.forEach((link) => {
    link.classList.toggle("active", link.getAttribute("href") === `#${activeSection.id}`);
  });
}

document.querySelectorAll(".prototype-switcher a").forEach((link) => {
  link.addEventListener("click", (event) => {
    event.preventDefault();
    const target = document.querySelector(link.getAttribute("href"));
    target?.scrollIntoView({ behavior: "smooth", inline: "start", block: "nearest" });
    setActiveInGroup([...document.querySelectorAll(".prototype-switcher a")], link);
  });
});

document.querySelectorAll(".article-list .article-row, .stream-panel .article-row").forEach((row) => {
  row.addEventListener("click", () => {
    const parentRows = [...row.parentElement.querySelectorAll(".article-row")];
    setActiveInGroup(parentRows, row);
  });
});

document.querySelectorAll(".filter").forEach((filter) => {
  filter.addEventListener("click", () => {
    setActiveInGroup([...filter.parentElement.querySelectorAll(".filter")], filter);
  });
});

document.querySelectorAll(".reader-tools button").forEach((button) => {
  button.addEventListener("click", () => {
    const fontMode = button.dataset.font;
    const widthMode = button.dataset.width;

    if (fontMode === "small") {
      document.body.classList.remove("prototype-large");
      document.body.classList.add("prototype-compact");
    }

    if (fontMode === "large") {
      document.body.classList.remove("prototype-compact");
      document.body.classList.add("prototype-large");
    }

    if (widthMode === "narrow") {
      document.body.classList.remove("prototype-wide");
    }

    if (widthMode === "wide") {
      document.body.classList.add("prototype-wide");
    }

    const relatedButtons = [...button.parentElement.querySelectorAll(`button[data-${fontMode ? "font" : "width"}]`)];
    setActiveInGroup(relatedButtons, button);
  });
});

document.querySelectorAll(".template-item").forEach((item) => {
  item.addEventListener("click", () => {
    setActiveInGroup([...document.querySelectorAll(".template-item")], item);
    const score = document.querySelector("#templateScore");
    const templateName = item.dataset.template;
    const scores = {
      "通用 RSS": "94%",
      "媒体全文": "88%",
      RSSHub: "97%",
      "自定义时间": "76%",
      "正文首图": "91%",
    };

    if (score) {
      score.textContent = scores[templateName] ?? "90%";
    }
  });
});

document.querySelector("#previewTemplate")?.addEventListener("click", () => {
  const input = document.querySelector("#testSource");
  const status = document.querySelector("#previewStatus");
  const value = input?.value?.trim() || "https://example.com/feed.xml";

  if (status) {
    status.textContent = `已预览：${value} · 命中 7/7 · 可保存`;
  }
});

window.addEventListener("scroll", updateSwitcherActive, { passive: true });
window.addEventListener("resize", updateSwitcherActive);
updateSwitcherActive();
