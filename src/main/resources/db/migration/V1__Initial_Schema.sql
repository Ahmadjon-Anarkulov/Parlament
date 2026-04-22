-- V1__Initial_Schema.sql
-- Parlament Bot - Initial Database Schema

CREATE TABLE IF NOT EXISTS bot_users (
    id                BIGSERIAL PRIMARY KEY,
    telegram_id       BIGINT      NOT NULL UNIQUE,
    username          VARCHAR(100),
    first_name        VARCHAR(100),
    last_name         VARCHAR(100),
    phone             VARCHAR(20),
    language_code     VARCHAR(10) DEFAULT 'ru',
    is_admin          BOOLEAN     NOT NULL DEFAULT FALSE,
    is_banned         BOOLEAN     NOT NULL DEFAULT FALSE,
    ban_reason        VARCHAR(500),
    state             VARCHAR(100) DEFAULT 'MAIN',
    created_at        TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP   NOT NULL DEFAULT NOW(),
    last_activity_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    orders_count      INT         NOT NULL DEFAULT 0,
    total_spent       DECIMAL(12,2) NOT NULL DEFAULT 0.00
);

CREATE TABLE IF NOT EXISTS categories (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    emoji       VARCHAR(20)  DEFAULT '🍽️',
    sort_order  INT          NOT NULL DEFAULT 0,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS products (
    id           BIGSERIAL      PRIMARY KEY,
    category_id  BIGINT         REFERENCES categories(id) ON DELETE SET NULL,
    name         VARCHAR(300)   NOT NULL,
    description  TEXT,
    price        DECIMAL(10,2)  NOT NULL,
    old_price    DECIMAL(10,2),
    image_url    VARCHAR(500),
    file_id      VARCHAR(500),
    is_available BOOLEAN        NOT NULL DEFAULT TRUE,
    sort_order   INT            NOT NULL DEFAULT 0,
    created_at   TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS orders (
    id              BIGSERIAL      PRIMARY KEY,
    user_id         BIGINT         NOT NULL REFERENCES bot_users(id),
    status          VARCHAR(50)    NOT NULL DEFAULT 'PENDING',
    total_amount    DECIMAL(12,2)  NOT NULL,
    delivery_type   VARCHAR(50)    NOT NULL DEFAULT 'PICKUP',
    delivery_address TEXT,
    comment         TEXT,
    phone           VARCHAR(20),
    payment_method  VARCHAR(50)    DEFAULT 'CASH',
    admin_comment   TEXT,
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_items (
    id          BIGSERIAL      PRIMARY KEY,
    order_id    BIGINT         NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id  BIGINT         REFERENCES products(id) ON DELETE SET NULL,
    product_name VARCHAR(300)  NOT NULL,
    price       DECIMAL(10,2)  NOT NULL,
    quantity    INT            NOT NULL DEFAULT 1,
    subtotal    DECIMAL(12,2)  NOT NULL
);

CREATE TABLE IF NOT EXISTS cart_items (
    id          BIGSERIAL     PRIMARY KEY,
    user_id     BIGINT        NOT NULL REFERENCES bot_users(id) ON DELETE CASCADE,
    product_id  BIGINT        NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    quantity    INT           NOT NULL DEFAULT 1,
    added_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, product_id)
);

CREATE TABLE IF NOT EXISTS bot_settings (
    key         VARCHAR(200) PRIMARY KEY,
    value       TEXT,
    description VARCHAR(500),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_bot_users_telegram_id  ON bot_users(telegram_id);
CREATE INDEX IF NOT EXISTS idx_orders_user_id         ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status          ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at      ON orders(created_at);
CREATE INDEX IF NOT EXISTS idx_products_category      ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_cart_items_user        ON cart_items(user_id);

-- Seed categories
INSERT INTO categories (name, emoji, sort_order) VALUES
('Горячие блюда',     '🍲', 1),
('Холодные закуски',  '🥗', 2),
('Выпечка',          '🥐', 3),
('Напитки',          '☕', 4),
('Десерты',          '🍰', 5)
ON CONFLICT DO NOTHING;

-- Seed products
INSERT INTO products (category_id, name, description, price, is_available, sort_order) VALUES
(1, 'Плов классический',    'Узбекский плов с говядиной, морковью и специями',              350.00, TRUE, 1),
(1, 'Лагман',               'Традиционный суп с домашней лапшой и мясом',                  290.00, TRUE, 2),
(1, 'Шурпа',                'Наваристый суп из баранины с овощами',                        270.00, TRUE, 3),
(2, 'Ачичук',               'Салат из помидоров с луком и зеленью',                        120.00, TRUE, 1),
(2, 'Редька с маслом',      'Свежая редька с растительным маслом',                         100.00, TRUE, 2),
(3, 'Самса с мясом',        'Слоёная самса с рубленной говядиной',                         80.00,  TRUE, 1),
(3, 'Лепёшка',              'Свежая лепёшка из тандыра',                                   50.00,  TRUE, 2),
(4, 'Чай зелёный',          'Свежезаваренный зелёный чай — чайник',                        80.00,  TRUE, 1),
(4, 'Кофе американо',       'Натуральный кофе',                                            120.00, TRUE, 2),
(5, 'Пахлава',              'Восточная сладость с орехами',                                150.00, TRUE, 1)
ON CONFLICT DO NOTHING;

-- Seed settings
INSERT INTO bot_settings (key, value, description) VALUES
('shop_name',           'Парламент',        'Название заведения'),
('shop_phone',          '+998 XX XXX XX XX','Телефон для связи'),
('shop_address',        'г. Ташкент',       'Адрес заведения'),
('shop_hours',          '09:00 - 22:00',    'Часы работы'),
('delivery_min_order',  '500',              'Минимальная сумма заказа для доставки'),
('delivery_price',      '0',               'Стоимость доставки (0 = бесплатно)'),
('welcome_message',     'Добро пожаловать в Парламент! 🍽️\n\nВыберите раздел меню:', 'Приветственное сообщение'),
('maintenance_mode',    'false',            'Режим обслуживания'),
('bot_active',          'true',             'Бот активен')
ON CONFLICT (key) DO NOTHING;
