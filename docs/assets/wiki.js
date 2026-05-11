const state = {
  lang: localStorage.getItem("omnicraft-lang") || "en",
  dark: localStorage.getItem("omnicraft-theme") === "dark"
};

const translations = {
  vi: {
    "Search wiki": "T�m trong wiki",
    "Dark": "T?i",
    "Light": "S�ng",
    "Overview": "T?ng quan",
    "Installation": "C�i d?t",
    "Commands": "L?nh",
    "Player Guide": "Ngu?i choi",
    "Admin GUI": "GUI admin",
    "Config": "Config",
    "Recipes": "Recipe",
    "Integrations": "T�ch h?p",
    "Anti-dupe": "Ch?ng dupe",
    "Examples": "V� d?",
    "Troubleshooting": "X? l� l?i",
    "FAQ": "FAQ",
    "Changelog": "Changelog",
    "Menus": "Menu",
    "Craft Clicks": "Click craft",
    "Main menu": "Menu ch�nh",
    "Vanilla recipe": "Recipe vanilla",
    "MMOItems recipe": "Recipe MMOItems",
    "Craft time countdown": "�?m ngu?c craft",
    "AdvancedEnchantments extraction": "T�ch AdvancedEnchantments",
    "Recipe does not craft": "Recipe kh�ng craft du?c",
    "MMOItems item missing": "Thi?u item MMOItems",
    "AE enchants disappear": "Enchant AE b? m?t",
    "Players spam craft": "Ngu?i choi spam craft",
    "Can members use /oc open?": "Member d�ng /oc open du?c kh�ng?",
    "Can stack previews show more than 64?": "Preview stack c� hi?n hon 64 kh�ng?",
    "Can I edit all text?": "C� ch?nh du?c to�n b? text kh�ng?"
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
  document.getElementById("search").placeholder = state.lang === "en" ? "Search wiki" : "T�m trong wiki";
  localStorage.setItem("omnicraft-lang", state.lang);
}

function applyTheme() {
  document.body.classList.toggle("dark", state.dark);
  document.getElementById("themeToggle").textContent = state.dark
    ? (state.lang === "en" ? "Light" : "S�ng")
    : (state.lang === "en" ? "Dark" : "T?i");
  localStorage.setItem("omnicraft-theme", state.dark ? "dark" : "light");
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
  applyTheme();
});

document.getElementById("themeToggle").addEventListener("click", () => {
  state.dark = !state.dark;
  applyTheme();
});

setupCopyButtons();
setupSearch();
applyLanguage();
applyTheme();
