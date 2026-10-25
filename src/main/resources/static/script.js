"use strict";

const $ = (selector) => document.querySelector(selector);

const initialArticle = {
  title: $("#article-heading") ? $("#article-heading").innerHTML : "",
  summary: $("#article-summary") ? $("#article-summary").innerHTML : "",
  conversation: $("#conversation") ? $("#conversation").innerHTML : "",
};

// ==========================================
// 1. ЗАГРУЗКА НОВОСТЕЙ С БЭКЕНДА (JAVA)
// ==========================================
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

      // Красивое форматирование даты
      let displayDate = 'Дата неизвестна';
      if (item.publishedAt) {
        try {
          displayDate = new Date(item.publishedAt).toLocaleDateString('ru-RU', {
            day: 'numeric', month: 'long', year: 'numeric'
          });
        } catch (e) {
          displayDate = item.publishedAt;
        }
      }

      // Определение цвета и текста бейджа
      const isHigh = item.relevanceLevel === 'HIGH';
      const badgeClass = isHigh ? 'critical' : 'opportunity';
      const badgeText = isHigh ? 'критично' : 'возможность';

      row.dataset.impact = isHigh ? 'risk' : 'opportunity';
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
              <span class="badge ${badgeClass}">${badgeText}</span>
            </span>
            <time datetime="${item.publishedAt || '2026-10-24'}">${displayDate}</time>
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

// ==========================================
// 2. ОБРАБОТКА КЛИКА ПО НОВОСТИ (INSIGHT)
// ==========================================
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
        // 1. Делаем запрос ко второму эндпоинту
        const response = await fetch(`/api/clients/${clientId}/news/${newsId}/insight`);
        if (!response.ok) throw new Error("Ошибка загрузки инсайта");

        // 2. Получаем JSON
        const insight = await response.json();
        console.log("Детальный инсайт:", insight);

        // 3. Обновляем заголовок и описание
        if ($("#article-heading")) $("#article-heading").textContent = insight.title;
        if ($("#article-summary")) $("#article-summary").textContent = insight.summary;

        // 4. Позиция банка
        if ($("#bank-position-text")) {
          $("#bank-position-text").textContent = insight.bankPosition || "Позиция банка не предоставлена.";
        }

        // 5. Вывод ИИ (почему важно)
        if ($("#ai-conclusion-text")) {
          $("#ai-conclusion-text").textContent = insight.whyImportantForClient || "Анализ ИИ недоступен.";
        }

        // 6. Риски
        const risksList = $("#risks-list");
        if (risksList) {
          risksList.innerHTML = ''; // Очищаем старые
          if (insight.risks && insight.risks.length > 0) {
            insight.risks.forEach(risk => {
              const li = document.createElement('li');
              li.textContent = risk;
              risksList.appendChild(li);
            });
          } else {
            risksList.innerHTML = '<li>Риски не выявлены</li>';
          }
        }

        // 7. Возможности
        const oppsList = $("#opportunities-list");
        if (oppsList) {
          oppsList.innerHTML = ''; // Очищаем старые
          if (insight.opportunities && insight.opportunities.length > 0) {
            insight.opportunities.forEach(opp => {
              const li = document.createElement('li');
              li.textContent = opp;
              oppsList.appendChild(li);
            });
          } else {
            oppsList.innerHTML = '<li>Возможности не выявлены</li>';
          }
        }

      } catch (error) {
        console.error(error);
        if ($("#article-heading")) $("#article-heading").textContent = "Ошибка загрузки";
        if ($("#article-summary")) $("#article-summary").textContent = "Не удалось получить разбор от AI-сервиса.";
      }
    });
  });
}

// Инициализация
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
// 3. ОСТАЛЬНОЙ ИНТЕРФЕЙС (ЧАТ, ФИЛЬТРЫ)
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

// Чат
function appendMessage(text, sender) {
  const conversation = $("#conversation");
  if (!conversation) return;

  const message = document.createElement("div");
  message.className = `message ${sender}-message`;
  const avatar = document.createElement("span");
  avatar.className = `avatar ${sender === "user" ? "user" : "ai"}-avatar`;

  const image = document.createElement("img");
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
  $("#chat-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const textInput = $("#chat-input");
    if (!textInput) return;

    const message = textInput.value.trim();
    if (!message) { textInput.focus(); return; }

    // 1. Показываем сообщение пользователя
    appendMessage(message, "user");
    textInput.value = "";
    $("#conversation").scrollTop = $("#conversation").scrollHeight;

    // 2. Получаем текущий ID клиента и выбранной новости
    const clientId = document.getElementById('client-id-input').value.trim() || 'U_80';
    const selectedBtn = document.querySelector('.news-item.selected');
    const newsId = selectedBtn ? selectedBtn.dataset.news : 'news_0131ff569124bbb0'; // Если новость не выбрана, берем ID по умолчанию

    try {
      // 3. Отправляем POST-запрос на Java
      const response = await fetch(`/api/clients/${clientId}/news/${newsId}/chat`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ message: message })
      });

      if (!response.ok) {
        throw new Error(`Ошибка сервера: ${response.status}`);
      }

      // 4. Получаем ответ и показываем его
      const data = await response.json();
      console.log("Ответ от ИИ:", data);

      appendMessage(data.content, "ai");

    } catch (error) {
      console.error(error);
      appendMessage("Извините, произошла ошибка при связи с сервером.", "ai");
    }

    $("#conversation").scrollTop = $("#conversation").scrollHeight;
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