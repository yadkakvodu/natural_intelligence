"use strict";

const $ = (selector) => document.querySelector(selector);

const initialArticle = {
  title: $("#article-heading") ? $("#article-heading").innerHTML : "",
  summary: $("#article-summary") ? $("#article-summary").innerHTML : "",
  conversation: $("#conversation") ? $("#conversation").innerHTML : "",
};

// ЗАГРУЗКА НОВОСТЕЙ С БЭКЕНДА (JAVA)
async function loadNewsFromBackend() {
  const inputField = document.getElementById('client-id-input');
  let clientId = inputField ? inputField.value.trim() : '';

  if (!clientId) {
    clientId = 'U_80';
    if (inputField) inputField.value = clientId;
  }

  try {
    const response = await fetch(`/api/clients/${clientId}/news-feed?limit=5`);

    if (!response.ok) {
      if (response.status === 500) {
        throw new Error(`Клиент с ID ${clientId} не найден или произошла ошибка базы.`);
      }
      throw new Error(`Ошибка сервера: ${response.status}`);
    }

    const data = await response.json();
    console.log(`Данные от Java для клиента ${clientId}:`, data);

    const newsList = document.querySelector('.news-list');
    if (!newsList) return;

    newsList.innerHTML = '';

    if (!data.items || data.items.length === 0) {
      newsList.innerHTML = '<p style="padding: 20px;">Новостей нет (Python еще не подключен)</p>';
      return;
    }

    data.items.forEach((item, index) => {
      const row = document.createElement('div');
      row.className = 'news-row';

      row.dataset.impact = index === 0 ? 'risk' : 'opportunity';
      row.dataset.score = Math.round((item.relevanceScore || 0) * 100);
      row.dataset.date = item.publishedAt || "2026-10-24";
      row.dataset.news = item.newsId || `news-${index}`;
      const imageUrl = item.imageUrl ? item.imageUrl : '/assets/bank-building.png';

      row.innerHTML = `
        <button type="button" class="news-item ${index === 0 ? 'selected' : ''}" data-news="${item.newsId}" aria-pressed="${index === 0}">
          <img class="news-thumbnail" src="${imageUrl}" alt="Новость" width="98" height="100" onerror="this.src='/assets/bank-building.png'">
          <span class="news-copy">
            <span class="news-title">${item.title || 'Без заголовка'}</span>
            <span class="news-info">
              Релевантность <strong>${Math.round((item.relevanceScore || 0) * 100)}%</strong>
              <span class="badge ${index === 0 ? 'critical' : 'opportunity'}">
                ${index === 0 ? 'критично' : 'возможность'}
              </span>
            </span>
            <time datetime="${item.publishedAt || '2026-10-24'}">${item.publishedAt || '24 октября 2026'}</time>
          </span>
        </button>
      `;

      newsList.appendChild(row);
    });

    attachClickListenersToNews();

  } catch (error) {
    console.error("Ошибка:", error);
    document.querySelector('.news-list').innerHTML = `<p style="color: red; padding: 20px;">${error.message}</p>`;
  }
}

function attachClickListenersToNews() {
  document.querySelectorAll("[data-news]").forEach((button) => {
    button.addEventListener("click", async () => {
      // Визуальное выделение
      document.querySelectorAll("[data-news]").forEach((item) => {
        const selected = item === button;
        item.classList.toggle("selected", selected);
        item.setAttribute("aria-pressed", String(selected));
      });

      const clientId = document.getElementById('client-id-input').value.trim() || 'U_80';
      const newsId = button.dataset.news;

      try {
        // Делаем запрос ко второму эндпоинту
        const response = await fetch(`/api/clients/${clientId}/news/${newsId}/insight`);
        if (!response.ok) throw new Error("Ошибка загрузки инсайта");

        const insight = await response.json();
        console.log("Детальный инсайт:", insight);

        // Обновляем правую колонку
        if ($("#article-heading")) $("#article-heading").textContent = insight.title;
        if ($("#article-summary")) $("#article-summary").textContent = insight.summary;

        // TODO: Обновить блоки Риски/Возможности (для этого нужно добавить ID в HTML)

      } catch (error) {
        console.error(error);
        if ($("#article-heading")) $("#article-heading").textContent = "Ошибка загрузки";
      }
    });
  });
}

document.addEventListener('DOMContentLoaded', () => {
  loadNewsFromBackend();

  const loadBtn = document.getElementById('load-client-btn');
  if (loadBtn) loadBtn.addEventListener('click', loadNewsFromBackend);

  const inputField = document.getElementById('client-id-input');
  if (inputField) {
    inputField.addEventListener('keypress', (event) => {
      if (event.key === 'Enter') loadNewsFromBackend();
    });
  }
});

// ==========================================
// 2. ЧАТ И ИНТЕРФЕЙС (ЧАТ, ФИЛЬТРЫ, ПОПОВЕРЫ)
// ==========================================
function togglePopover(button, menu) {
  if (!button || !menu) return;
  const open = menu.hidden;
  document.querySelectorAll(".popover").forEach((popover) => { popover.hidden = true; });
  document.querySelectorAll("[aria-controls]").forEach((trigger) => trigger.setAttribute("aria-expanded", "false"));
  menu.hidden = !open;
  button.setAttribute("aria-expanded", String(open));
}

[["#filter-button", "#filter-menu"], ["#company-button", "#company-menu"]].forEach(([trigger, menu]) => {
  const triggerEl = $(trigger);
  const menuEl = $(menu);
  if (triggerEl && menuEl) {
    triggerEl.addEventListener("click", () => togglePopover(triggerEl, menuEl));
  }
});

document.addEventListener("click", (event) => {
  if (event.target.closest(".popover, [aria-controls]")) return;
  document.querySelectorAll(".popover").forEach((menu) => { menu.hidden = true; });
  document.querySelectorAll("[aria-controls]").forEach((button) => button.setAttribute("aria-expanded", "false"));
});

document.addEventListener("keydown", (event) => {
  if (event.key !== "Escape") return;
  document.querySelectorAll(".popover").forEach((menu) => { menu.hidden = true; });
  document.querySelectorAll("[aria-controls]").forEach((button) => button.setAttribute("aria-expanded", "false"));
});

function filterNews() {
  const filterEl = $("#impact-filter");
  const sortEl = $("#news-sort");
  if (!filterEl || !sortEl) return;

  const filter = filterEl.value;
  const rows = [...document.querySelectorAll(".news-row")];
  rows.forEach((row) => { row.hidden = filter !== "all" && row.dataset.impact !== filter; });
  rows.sort((a, b) => sortEl.value === "date" ? b.dataset.date.localeCompare(a.dataset.date) : Number(b.dataset.score) - Number(a.dataset.score));
  rows.forEach((row) => $(".news-list")?.append(row));
}

if ($("#impact-filter")) $("#impact-filter").addEventListener("change", filterNews);
if ($("#news-sort")) $("#news-sort").addEventListener("change", filterNews);
if ($("#filter-menu")) $("#filter-menu").addEventListener("submit", (event) => event.preventDefault());
if ($("#filter-menu")) $("#filter-menu").addEventListener("reset", () => queueMicrotask(filterNews));

function appendMessage(text, sender) {
  const conversation = $("#conversation");
  if (!conversation) return;

  const message = document.createElement("div");
  message.className = `message ${sender}-message`;
  const avatar = document.createElement("span");
  avatar.className = `avatar ${sender === "user" ? "user" : "ai"}-avatar`;

  const image = document.createElement("img");
  // ВАЖНО: Добавлена косая черта перед assets
  image.src = sender === "user" ? "/assets/chat-user.png" : "/assets/chat-alfa.png";
  image.width = sender === "user" ? 17 : 24;
  image.height = sender === "user" ? 20 : 30;
  image.alt = sender === "user" ? "Вы" : "ИИ";

  avatar.append(image);
  const body = document.createElement("div");
  body.className = "message-body";
  const bubble = document.createElement("div");
  bubble.className = "bubble";
  bubble.textContent = text;
  const time = document.createElement("time");
  time.dateTime = new Date().toISOString();
  time.textContent = new Date().toLocaleTimeString("ru-RU", { hour: "2-digit", minute: "2-digit" });

  body.append(bubble, time);
  message.append(avatar, body);
  conversation.append(message);
}

if ($("#chat-form")) {
  $("#chat-form").addEventListener("submit", (event) => {
    event.preventDefault();
    const textInput = $("#chat-input");
    if (!textInput) return;

    const text = textInput.value.trim();
    if (!text) { textInput.focus(); return; }

    appendMessage(text, "user");
    appendMessage("Это демо-режим. Бэкенд для чата еще не подключен.", "ai");
    textInput.value = "";
    $("#conversation").scrollTop = $("#conversation").scrollHeight;
    textInput.focus();
  });
}

if ($("#chat-input")) {
  $("#chat-input").addEventListener("keydown", (event) => {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      $("#chat-form").requestSubmit();
    }
  });
}

function showInfo(title, copy) {
  if ($("#dialog-title")) $("#dialog-title").textContent = title;
  if ($("#dialog-copy")) $("#dialog-copy").textContent = copy;
  if ($("#info-dialog")) $("#info-dialog").showModal();
}

document.querySelectorAll("[data-section]").forEach((button) => button.addEventListener("click", () => {
  showInfo(button.dataset.section, "В исходном макете представлен только раздел «Новостной консультант».");
}));

if ($("#logout-button")) {
  $("#logout-button").addEventListener("click", () => showInfo("Выход", "Страница работает локально и не использует авторизацию."));
}

if ($("#info-dialog")) {
  $("#info-dialog").addEventListener("click", (event) => {
    if (event.target === $("#info-dialog")) $("#info-dialog").close();
  });
}