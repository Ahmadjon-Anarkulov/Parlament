# 🏛️ Parlament Bot v2.0

Профессиональный Telegram-бот для ресторана на **Java 21 + Spring Boot 3 + PostgreSQL**.

---

## ✨ Возможности

| Пользователь | Администратор |
|---|---|
| 🍽️ Каталог по категориям | 📊 Статистика в реальном времени |
| 🛒 Корзина с изменением кол-ва | 📦 Управление заказами |
| ✅ Оформление заказа (доставка/самовывоз) | 🔄 Смена статуса заказов |
| 📋 История заказов | 🛍️ Управление меню (вкл/выкл блюда) |
| 👤 Профиль с историей трат | 📢 Рассылка всем пользователям |
| 📞 Контакты и информация | 👥 Просмотр пользователей |
| 💬 Комментарий к заказу | ⚙️ Настройки бота |
| 📱 Отправка контакта | 🔔 Уведомления о новых заказах |

---

## 🚀 Быстрый старт

### 1. Подготовка

```bash
git clone <repo-url>
cd parlament-bot
cp .env.example .env
# Заполните .env своими данными
```

### 2. Локальный запуск с Docker

```bash
docker-compose up -d
```

Сервисы:
- 🤖 **Бот** → работает автоматически
- 🐘 **PostgreSQL** → `localhost:5432` / БД: `Parlament`
- 🖥️ **pgAdmin 4** → http://localhost:5050 (email/пароль из `.env`)

### 3. Подключение pgAdmin 4 к базе данных Parlament

При открытии pgAdmin 4 (http://localhost:5050):
1. Сервер `Parlament DB` уже добавлен автоматически
2. Введите пароль: `ваш DATABASE_PASSWORD из .env`
3. Перейдите: `Servers → Parlament DB → Databases → Parlament → Schemas → public → Tables`

Там будут таблицы:
- `bot_users` — все пользователи бота
- `categories` — категории меню
- `products` — товары/блюда
- `orders` — заказы
- `order_items` — позиции заказов
- `cart_items` — корзины пользователей
- `bot_settings` — настройки бота

### 4. Ручное подключение pgAdmin 4 (если сервер не добавился)

1. Откройте pgAdmin → **Add New Server**
2. **General** → Name: `Parlament DB`
3. **Connection**:
   - Host: `localhost` (или `postgres` если pgAdmin в Docker)
   - Port: `5432`
   - Database: `Parlament`
   - Username: `postgres`
   - Password: ваш пароль

---

## ⚙️ Конфигурация (.env)

```env
# Обязательные
BOT_TOKEN=123456789:AAFxxxx          # От @BotFather
BOT_USERNAME=MyParlamentBot           # Без @

# База данных
DATABASE_USER=postgres
DATABASE_PASSWORD=strong_password

# Администраторы (Telegram user ID через запятую)
ADMIN_IDS=123456789,987654321

# pgAdmin
PGADMIN_EMAIL=admin@parlament.uz
PGADMIN_PASSWORD=admin123
```

Чтобы узнать свой Telegram ID: напишите боту `@userinfobot`.

---

## 🔧 Команды администратора

Доступны через кнопки меню или команды:

| Кнопка | Что делает |
|---|---|
| 📊 Статистика | Пользователи, заказы, выручка сегодня и всего |
| 📦 Заказы | Список ожидающих заказов с кнопками смены статуса |
| 🛍️ Каталог | Список товаров, включение/выключение позиций |
| 👥 Пользователи | Статистика по пользователям |
| ⚙️ Настройки | Название, телефон, адрес, режим работы |
| 📢 Рассылка | Отправить сообщение всем пользователям |

---

## 📦 Статусы заказов

```
⏳ PENDING → ✅ CONFIRMED → 👨‍🍳 PREPARING → 🔔 READY → 🚴 DELIVERING → ✔️ COMPLETED
                                                                        ↘ ❌ CANCELLED
```

При смене статуса клиент получает автоматическое уведомление.

---

## 🌐 Деплой на Railway.app

1. Залейте репозиторий на GitHub
2. [railway.app](https://railway.app) → **New Project** → **Deploy from GitHub**
3. Добавьте **PostgreSQL** плагин в Railway → имя БД: `Parlament`
4. Во вкладке **Variables** добавьте:

```
BOT_TOKEN          = токен от @BotFather
BOT_USERNAME       = username бота
ADMIN_IDS          = ваш Telegram ID
DATABASE_URL       = (Railway подставит автоматически из плагина)
DATABASE_USER      = (из плагина)
DATABASE_PASSWORD  = (из плагина)
```

5. Нажмите **Deploy**

---

## 🗄️ Структура проекта

```
src/main/java/com/parlament/
├── ParlamentBotApplication.java     ← Точка входа
├── config/
│   ├── BotProperties.java           ← Конфиг из application.yml
│   └── TelegramConfig.java          ← Регистрация бота
├── model/
│   ├── BotUser.java                 ← Пользователи
│   ├── Category.java                ← Категории меню
│   ├── Product.java                 ← Товары
│   ├── Order.java                   ← Заказы (+ Status, DeliveryType enum)
│   ├── OrderItem.java               ← Позиции заказа
│   ├── CartItem.java                ← Корзина
│   └── BotSetting.java              ← Настройки (key-value)
├── repository/                      ← Spring Data JPA репозитории
├── service/
│   ├── UserService.java             ← Регистрация, состояния, бан
│   ├── CatalogService.java          ← Каталог с кешированием
│   ├── CartService.java             ← Корзина
│   ├── OrderService.java            ← Заказы и статистика
│   ├── SettingsService.java         ← Настройки бота
│   └── NotificationService.java     ← Уведомления администраторам
├── handler/
│   ├── CommandHandler.java          ← /start, /menu, /admin, /help
│   ├── TextHandler.java             ← Кнопки главного меню и адм-панели
│   ├── CallbackHandler.java         ← Inline-кнопки (каталог, корзина, заказы)
│   └── CheckoutHandler.java         ← Многошаговое оформление заказа
├── telegram/
│   └── ParlamentBot.java            ← Главный класс бота
├── controller/
│   ├── WebhookController.java       ← Webhook endpoint
│   └── AdminApiController.java      ← REST API для внешних интеграций
└── util/
    ├── KeyboardFactory.java         ← Все клавиатуры (Reply + Inline)
    ├── MessageFormatter.java        ← Форматирование сообщений
    └── BotSender.java               ← Безопасная отправка сообщений

src/main/resources/
├── application.yml                  ← Конфигурация Spring Boot
└── db/migration/
    └── V1__Initial_Schema.sql       ← Flyway: схема БД + начальные данные
```

---

## 🛠️ Технологии

- **Java 21** + **Spring Boot 3.2**
- **TelegramBots 6.9** — Long Polling & Webhook
- **PostgreSQL 16** — основная БД
- **Flyway** — миграции БД
- **Spring Data JPA** + **Hibernate**
- **Caffeine Cache** — кеш категорий и настроек
- **Lombok** — меньше boilerplate
- **pgAdmin 4** — администрирование БД
- **Docker + Docker Compose** — окружение
- **Railway.app** — деплой в облако
