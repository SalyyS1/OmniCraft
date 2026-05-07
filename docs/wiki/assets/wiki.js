const dictionary = {
  en: {
    title: "Crafting stations for MMO servers",
    subtitle: "A compact guide for installing, configuring, and operating OmniCraft safely.",
    navOverview: "Overview",
    navInstall: "Installation",
    navCommands: "Commands",
    navRecipes: "Recipes",
    navSafety: "Anti-dupe safety",
    navAdmin: "Admin tools",
    navTrouble: "Troubleshooting",
    badgeCore: "Core",
    badgeSetup: "Setup",
    badgeAccess: "Access",
    badgeSafety: "Safety",
    badgeAdmin: "Admin",
    badgeHelp: "Help",
    overviewTitle: "What OmniCraft does",
    overviewBody: "OmniCraft creates MMOItems-style crafting menus with category browsing, recipe previews, requirements, craft delays, and server-side transactions.",
    installTitle: "Installation",
    install1: "Build the jar with Gradle.",
    install2: "Place the matching jar in the server plugins folder.",
    install3: "Start the server once, then edit config files.",
    commandsTitle: "Commands and permissions",
    recipesTitle: "Recipe file shape",
    safetyTitle: "Anti-dupe model",
    safetyBody: "OmniCraft cancels GUI clicks, ignores risky client actions, locks player recipe transactions, checks inventory server-side, and rolls back if output cannot be delivered.",
    adminTitle: "Admin tools",
    adminBody: "Use settings, browse, reload, and debug commands to inspect recipes, reload YAML, and dry-run requirements before players craft.",
    troubleTitle: "Troubleshooting",
    troubleBody: "Run /oc reload after editing files. If MMOItems or Vault is missing, recipes using those hooks should be disabled or adjusted."
  },
  vi: {
    title: "Trạm chế tạo cho server MMO",
    subtitle: "Wiki gọn để cài đặt, cấu hình và vận hành OmniCraft an toàn.",
    navOverview: "Tổng quan",
    navInstall: "Cài đặt",
    navCommands: "Lệnh",
    navRecipes: "Recipe",
    navSafety: "Chống dupe",
    navAdmin: "Công cụ admin",
    navTrouble: "Xử lý lỗi",
    badgeCore: "Cốt lõi",
    badgeSetup: "Cài đặt",
    badgeAccess: "Quyền",
    badgeSafety: "An toàn",
    badgeAdmin: "Admin",
    badgeHelp: "Hỗ trợ",
    overviewTitle: "OmniCraft làm gì",
    overviewBody: "OmniCraft tạo GUI chế tạo giống MMOItems station, có category, preview recipe, yêu cầu chế tạo, delay và transaction server-side.",
    installTitle: "Cài đặt",
    install1: "Build jar bằng Gradle.",
    install2: "Đặt jar đúng phiên bản vào thư mục plugins.",
    install3: "Khởi động server một lần, sau đó chỉnh config.",
    commandsTitle: "Lệnh và quyền",
    recipesTitle: "Cấu trúc recipe",
    safetyTitle: "Cơ chế chống dupe",
    safetyBody: "OmniCraft hủy click GUI, bỏ qua thao tác client rủi ro, khóa transaction theo player/recipe, check inventory server-side và rollback nếu không phát được output.",
    adminTitle: "Công cụ admin",
    adminBody: "Dùng settings, browse, reload và debug để kiểm tra recipe, reload YAML và dry-run yêu cầu trước khi member craft.",
    troubleTitle: "Xử lý lỗi",
    troubleBody: "Chạy /oc reload sau khi sửa file. Nếu thiếu MMOItems hoặc Vault, hãy tắt hoặc chỉnh recipe dùng hook đó."
  }
};

function applyLanguage(lang) {
  const messages = dictionary[lang] || dictionary.en;
  document.documentElement.lang = lang;
  document.querySelectorAll("[data-i18n]").forEach((node) => {
    node.textContent = messages[node.dataset.i18n] || node.textContent;
  });
  localStorage.setItem("omnicraft-lang", lang);
}

document.querySelectorAll("[data-lang]").forEach((button) => {
  button.addEventListener("click", () => applyLanguage(button.dataset.lang));
});

applyLanguage(localStorage.getItem("omnicraft-lang") || "en");
