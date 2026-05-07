const dictionary = {
  en: {
    title: "Crafting station guide",
    subtitle: "Player usage, admin setup, recipe files, integrations, and anti-dupe safety.",
    navPlayers: "Players",
    navAdmins: "Admins",
    navFiles: "Files",
    navRecipe: "Recipe YAML",
    navHooks: "Integrations",
    navSafety: "Anti-dupe",
    navTrouble: "Troubleshooting",
    badgePlayer: "Player",
    badgeHooks: "Hooks",
    badgeSafety: "Safety",
    badgeHelp: "Help",
    playersTitle: "Open and craft",
    playersBody: "Use /oc, /ocraft, or /craft. Red requirement lines mean missing materials. Green lines mean ready.",
    use: "Use",
    openMain: "Open main menu",
    openCategory: "Open a category",
    adminsTitle: "Admin commands",
    settings: "Settings menu",
    browse: "Recipe browser",
    validate: "Check config errors",
    reload: "Reload files",
    filesTitle: "Server files",
    recipeTitle: "Recipe example",
    hooksTitle: "Integrations",
    hooksBody: "MMOItems is used for MMO item output and matching. AdvancedEnchantments is used to apply custom enchants. Vault handles money. PlaceholderAPI handles custom conditions.",
    safetyTitle: "Anti-dupe model",
    safetyBody: "OmniCraft cancels GUI clicks, ignores risky client actions, locks player recipe transactions, scans inventory server-side, and rolls back when output cannot fit.",
    troubleTitle: "Troubleshooting",
    troubleBody: "Use /oc validate after editing recipes. Use /oc reload after fixing files. Read logs/craft-history.log for success, fail, and rollback details."
  },
  vi: {
    title: "Hướng dẫn trạm chế tạo",
    subtitle: "Cách dùng cho member, setup admin, recipe file, tích hợp plugin và chống dupe.",
    navPlayers: "Member",
    navAdmins: "Admin",
    navFiles: "File",
    navRecipe: "Recipe YAML",
    navHooks: "Tích hợp",
    navSafety: "Chống dupe",
    navTrouble: "Xử lý lỗi",
    badgePlayer: "Member",
    badgeHooks: "Hook",
    badgeSafety: "An toàn",
    badgeHelp: "Hỗ trợ",
    playersTitle: "Mở menu và craft",
    playersBody: "Dùng /oc, /ocraft hoặc /craft. Dòng requirement màu đỏ là thiếu. Màu xanh là đủ.",
    use: "Công dụng",
    openMain: "Mở main menu",
    openCategory: "Mở category",
    adminsTitle: "Lệnh admin",
    settings: "Menu settings",
    browse: "Recipe browser",
    validate: "Kiểm tra lỗi config",
    reload: "Reload file",
    filesTitle: "File server",
    recipeTitle: "Ví dụ recipe",
    hooksTitle: "Tích hợp",
    hooksBody: "MMOItems dùng cho output và matching item MMO. AdvancedEnchantments dùng để apply custom enchant. Vault xử lý money. PlaceholderAPI xử lý condition.",
    safetyTitle: "Cơ chế chống dupe",
    safetyBody: "OmniCraft hủy click GUI, bỏ qua thao tác client rủi ro, khóa transaction theo player/recipe, scan inventory server-side và rollback nếu output không chứa được.",
    troubleTitle: "Xử lý lỗi",
    troubleBody: "Dùng /oc validate sau khi sửa recipe. Dùng /oc reload sau khi fix file. Xem logs/craft-history.log để biết success, fail và rollback."
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
