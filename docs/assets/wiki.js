function setupCopyButtons() {
  document.querySelectorAll("pre").forEach((pre) => {
    const label = pre.dataset.copyLabel || "Sao chép";
    const success = pre.dataset.copySuccess || "Đã sao chép";
    const error = pre.dataset.copyError || "Không thể sao chép";
    const button = document.createElement("button");
    button.className = "copy";
    button.type = "button";
    button.textContent = label;
    button.addEventListener("click", async () => {
      try {
        await navigator.clipboard.writeText(pre.innerText.replace(new RegExp(`^${label}\\n?`), ""));
        button.textContent = success;
      } catch {
        button.textContent = error;
      }
      setTimeout(() => { button.textContent = label; }, 1200);
    });
    pre.appendChild(button);
  });
}

function escapeHtml(value) {
  return value.replace(/[&<>]/g, (character) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;" })[character]);
}

function splitYamlComment(line) {
  let quote = null;
  for (let index = 0; index < line.length; index += 1) {
    const character = line[index];
    if (quote) {
      if (character === quote && line[index - 1] !== "\\") quote = null;
      continue;
    }
    if (character === '"' || character === "'") {
      quote = character;
    } else if (character === "#" && (index === 0 || /\s/.test(line[index - 1]))) {
      return [line.slice(0, index), line.slice(index)];
    }
  }
  return [line, ""];
}

function highlightYamlValue(rawValue) {
  const leading = rawValue.match(/^\s*/)[0];
  const value = rawValue.slice(leading.length);
  if (!value) return escapeHtml(rawValue);
  const tokenClass = /^("(?:[^"\\]|\\.)*"|'(?:[^'\\]|\\.)*')$/.test(value)
    ? "yaml-string"
    : /^(true|false|null)$/i.test(value)
      ? "yaml-boolean"
      : /^-?\d+(?:\.\d+)?$/.test(value)
        ? "yaml-number"
        : "yaml-value";
  return `${escapeHtml(leading)}<span class="${tokenClass}">${escapeHtml(value)}</span>`;
}

function highlightYamlLine(line) {
  const [content, rawComment] = splitYamlComment(line);
  const comment = rawComment ? `<span class="yaml-comment">${escapeHtml(rawComment)}</span>` : "";
  const match = content.match(/^(\s*)([\w-]+)(:)(\s*)(.*)$/);
  if (!match) return `${escapeHtml(content)}${comment}`;

  const [, indent, key, colon, spacing, rawValue] = match;
  const value = highlightYamlValue(rawValue);
  return `${escapeHtml(indent)}<span class="yaml-key">${key}</span><span class="yaml-punctuation">${colon}</span>${escapeHtml(spacing)}${value}${comment}`;
}

function setupYamlHighlighting() {
  document.querySelectorAll("code.yaml").forEach((code) => {
    const source = code.textContent;
    code.innerHTML = source.split("\n").map(highlightYamlLine).join("\n");
  });
}

function setupSearch() {
  const search = document.getElementById("search");
  if (!search) return;
  const locale = document.documentElement.lang || "vi";
  search.addEventListener("input", (event) => {
    const query = event.target.value.trim().toLocaleLowerCase(locale);
    document.querySelectorAll(".searchable").forEach((section) => {
      section.classList.toggle("hidden", Boolean(query) && !section.textContent.toLocaleLowerCase(locale).includes(query));
    });
  });
}

setupYamlHighlighting();
setupCopyButtons();
setupSearch();
