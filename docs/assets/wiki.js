const state = {
  lang: localStorage.getItem("omnicraft-lang") || "en"
};

const translations = {
  vi: {
    "Search wiki": "Tìm trong wiki",
    "Overview": "Tổng quan",
    "Installation": "Cài đặt",
    "Commands": "Lệnh",
    "Player Guide": "Người chơi",
    "Admin GUI": "GUI admin",
    "Config": "Config",
    "Recipes": "Recipe",
    "Integrations": "Tích hợp",
    "Anti-dupe": "Chống dupe",
    "Examples": "Ví dụ",
    "Troubleshooting": "Xử lý lỗi",
    "FAQ": "FAQ",
    "Changelog": "Changelog",
    "Menus": "Menu",
    "Craft Clicks": "Click craft",
    "Main menu": "Menu chính",
    "Vanilla recipe": "Recipe vanilla",
    "MMOItems recipe": "Recipe MMOItems",
    "Craft time countdown": "Đếm ngược craft",
    "AdvancedEnchantments extraction": "Tách AdvancedEnchantments",
    "Recipe does not craft": "Recipe không craft được",
    "MMOItems item missing": "Thiếu item MMOItems",
    "AE enchants disappear": "Enchant AE bị mất",
    "Players spam craft": "Người chơi spam craft",
    "Can members use /oc open?": "Member dùng /oc open được không?",
    "Can stack previews show more than 64?": "Preview stack có hiện hơn 64 không?",
    "Can I edit all text?": "Có chỉnh được toàn bộ text không?"
  }
};

function applyLanguage() {
  document.documentElement.lang = state.lang;
  document.querySelectorAll("[data-en]").forEach((node) => {
    node.textContent = node.dataset[state.lang] || node.dataset.en;
  });
  document.querySelectorAll("[data-en-list]").forEach((node) => {
    const values = (node.dataset[state.lang + "List"] || node.dataset.enList).split("|");
    node.querySelectorAll("li").forEach((li, index) => {
      if (values[index]) li.textContent = values[index];
    });
  });
  document.querySelectorAll(".sidebar a, h3, summary").forEach((node) => {
    node.dataset.original ??= node.textContent;
    node.textContent = translations[state.lang]?.[node.dataset.original] || node.dataset.original;
  });
  document.getElementById("langToggle").textContent = state.lang === "en" ? "VI" : "EN";
  document.getElementById("search").placeholder = state.lang === "en" ? "Search wiki" : "Tìm trong wiki";
  localStorage.setItem("omnicraft-lang", state.lang);
}

function setupCopyButtons() {
  document.querySelectorAll("pre").forEach((pre) => {
    const button = document.createElement("button");
    button.className = "copy";
    button.type = "button";
    button.textContent = "Copy";
    button.addEventListener("click", async () => {
      await navigator.clipboard.writeText(pre.innerText.replace(/^Copy\n?/, ""));
      button.textContent = "Copied";
      setTimeout(() => button.textContent = "Copy", 900);
    });
    pre.appendChild(button);
  });
}

function setupSearch() {
  document.getElementById("search").addEventListener("input", (event) => {
    const query = event.target.value.trim().toLowerCase();
    document.querySelectorAll(".searchable").forEach((section) => {
      section.classList.toggle("hidden", query.length > 0 && !section.textContent.toLowerCase().includes(query));
    });
  });
}

document.getElementById("langToggle").addEventListener("click", () => {
  state.lang = state.lang === "en" ? "vi" : "en";
  applyLanguage();
});

setupCopyButtons();
setupSearch();
applyLanguage();
