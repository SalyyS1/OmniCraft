function setupCopyButtons() {
  document.querySelectorAll("pre").forEach((pre) => {
    const button = document.createElement("button");
    button.className = "copy";
    button.type = "button";
    button.textContent = "Sao chép";
    button.addEventListener("click", async () => {
      try {
        await navigator.clipboard.writeText(pre.innerText.replace(/^Sao chép\n?/, ""));
        button.textContent = "Đã sao chép";
      } catch {
        button.textContent = "Không thể sao chép";
      }
      setTimeout(() => { button.textContent = "Sao chép"; }, 1200);
    });
    pre.appendChild(button);
  });
}

function setupSearch() {
  const search = document.getElementById("search");
  if (!search) return;
  search.addEventListener("input", (event) => {
    const query = event.target.value.trim().toLocaleLowerCase("vi");
    document.querySelectorAll(".searchable").forEach((section) => {
      section.classList.toggle("hidden", Boolean(query) && !section.textContent.toLocaleLowerCase("vi").includes(query));
    });
  });
}

setupCopyButtons();
setupSearch();
