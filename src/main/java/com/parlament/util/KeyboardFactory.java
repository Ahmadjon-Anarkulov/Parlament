package com.parlament.util;

import com.parlament.model.CartItem;
import com.parlament.model.Category;
import com.parlament.model.Order;
import com.parlament.model.Product;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

public class KeyboardFactory {

    // ─── Reply keyboards ──────────────────────────────────────────────────────

    public static ReplyKeyboardMarkup mainMenu() {
        return reply(
                row("🍽️ Меню", "🛒 Корзина"),
                row("📋 Мои заказы", "ℹ️ О нас"),
                row("📞 Контакты", "⚙️ Профиль")
        );
    }

    public static ReplyKeyboardMarkup adminMenu() {
        return reply(
                row("📊 Статистика", "📦 Заказы"),
                row("🛍️ Каталог", "👥 Пользователи"),
                row("⚙️ Настройки", "📢 Рассылка"),
                row("◀️ Главное меню")
        );
    }

    public static ReplyKeyboardMarkup backOnly() {
        return reply(row("◀️ Назад"));
    }

    public static ReplyKeyboardMarkup cancelOnly() {
        return reply(row("❌ Отмена"));
    }

    public static ReplyKeyboardMarkup contactKeyboard() {
        KeyboardButton btn = new KeyboardButton("📱 Поделиться номером");
        btn.setRequestContact(true);
        KeyboardRow row = new KeyboardRow();
        row.add(btn);
        KeyboardRow back = new KeyboardRow();
        back.add("◀️ Назад");
        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup(List.of(row, back));
        kb.setResizeKeyboard(true);
        return kb;
    }

    public static ReplyKeyboardMarkup deliveryTypeKeyboard() {
        return reply(
                row("🏪 Самовывоз", "🚴 Доставка"),
                row("◀️ Отмена")
        );
    }

    // ─── Inline keyboards ─────────────────────────────────────────────────────

    public static InlineKeyboardMarkup categories(List<Category> cats) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Category c : cats) {
            rows.add(List.of(btn(c.getDisplayName(), "cat:" + c.getId())));
        }
        return inline(rows);
    }

    public static InlineKeyboardMarkup products(List<Product> products, Long categoryId) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Product p : products) {
            String label = p.getName() + " — " + p.formatPrice();
            rows.add(List.of(btn(label, "prod:" + p.getId())));
        }
        rows.add(List.of(btn("◀️ К категориям", "back:cats")));
        return inline(rows);
    }

    public static InlineKeyboardMarkup productActions(Product p) {
        return inline(List.of(
                List.of(
                        btn("➕ В корзину", "add:" + p.getId()),
                        btn("◀️ Назад", "back:cat:" + p.getCategory().getId())
                )
        ));
    }

    public static InlineKeyboardMarkup cart(List<CartItem> items) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (CartItem item : items) {
            rows.add(List.of(
                    btn("➖", "cart:dec:" + item.getProduct().getId()),
                    btn(item.getProduct().getName() + " ×" + item.getQuantity(), "cart:info:" + item.getProduct().getId()),
                    btn("➕", "cart:inc:" + item.getProduct().getId()),
                    btn("🗑", "cart:del:" + item.getProduct().getId())
            ));
        }
        if (!items.isEmpty()) {
            rows.add(List.of(
                    btn("✅ Оформить заказ", "checkout"),
                    btn("🗑 Очистить", "cart:clear")
            ));
        }
        rows.add(List.of(btn("🍽️ В меню", "back:cats")));
        return inline(rows);
    }

    public static InlineKeyboardMarkup orderStatus(Long orderId) {
        return inline(List.of(
                List.of(btn("🔄 Обновить статус", "order:refresh:" + orderId))
        ));
    }

    public static InlineKeyboardMarkup adminOrderActions(Long orderId, Order.Status current) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> statusRow = new ArrayList<>();
        for (Order.Status s : Order.Status.values()) {
            if (s != current) {
                statusRow.add(btn(s.getLabel(), "admin:status:" + orderId + ":" + s.name()));
                if (statusRow.size() == 2) { rows.add(new ArrayList<>(statusRow)); statusRow.clear(); }
            }
        }
        if (!statusRow.isEmpty()) rows.add(statusRow);
        rows.add(List.of(btn("💬 Комментарий", "admin:comment:" + orderId)));
        return inline(rows);
    }

    public static InlineKeyboardMarkup adminCatalogMenu() {
        return inline(List.of(
                List.of(btn("📂 Категории", "admin:cat:list"), btn("🍽️ Товары", "admin:prod:list")),
                List.of(btn("➕ Новая категория", "admin:cat:new"), btn("➕ Новый товар", "admin:prod:new"))
        ));
    }

    public static InlineKeyboardMarkup adminProductActions(Product p) {
        return inline(List.of(
                List.of(
                        btn(p.isAvailable() ? "🔴 Скрыть" : "🟢 Показать", "admin:prod:toggle:" + p.getId()),
                        btn("✏️ Изменить цену", "admin:prod:price:" + p.getId())
                ),
                List.of(btn("◀️ К списку", "admin:prod:list"))
        ));
    }

    public static InlineKeyboardMarkup pagination(String prefix, int page, long total, int pageSize) {
        int totalPages = (int) Math.ceil((double) total / pageSize);
        List<InlineKeyboardButton> row = new ArrayList<>();
        if (page > 0) row.add(btn("◀️", prefix + ":" + (page - 1)));
        row.add(btn((page + 1) + "/" + totalPages, "noop"));
        if (page < totalPages - 1) row.add(btn("▶️", prefix + ":" + (page + 1)));
        return inline(List.of(row));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static ReplyKeyboardMarkup reply(KeyboardRow... rows) {
        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup(List.of(rows));
        kb.setResizeKeyboard(true);
        kb.setSelective(true);
        return kb;
    }

    private static KeyboardRow row(String... labels) {
        KeyboardRow r = new KeyboardRow();
        for (String l : labels) r.add(new KeyboardButton(l));
        return r;
    }

    private static InlineKeyboardMarkup inline(List<List<InlineKeyboardButton>> rows) {
        InlineKeyboardMarkup kb = new InlineKeyboardMarkup();
        kb.setKeyboard(rows);
        return kb;
    }

    private static InlineKeyboardButton btn(String text, String data) {
        InlineKeyboardButton b = new InlineKeyboardButton(text);
        b.setCallbackData(data);
        return b;
    }
}
