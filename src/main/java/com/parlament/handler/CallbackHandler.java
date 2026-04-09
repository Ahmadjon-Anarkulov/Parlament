package com.parlament.handler;

import com.parlament.bot.ParlamentBot;
import com.parlament.data.ProductCatalog;
import com.parlament.model.*;
import com.parlament.service.CartService;
import com.parlament.service.OrderService;
import com.parlament.service.SessionService;
import com.parlament.util.KeyboardFactory;
import com.parlament.util.MessageFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

/**
 * Handles all inline keyboard (callback query) interactions.
 */
public class CallbackHandler {

    private static final Logger log = LoggerFactory.getLogger(CallbackHandler.class);

    private final ParlamentBot bot;
    private final CartService cartService;
    private final OrderService orderService;
    private final SessionService sessionService;

    public CallbackHandler(ParlamentBot bot, CartService cartService,
                           OrderService orderService, SessionService sessionService) {
        this.bot = bot;
        this.cartService = cartService;
        this.orderService = orderService;
        this.sessionService = sessionService;
    }

    public void handle(Update update) {
        CallbackQuery cb = update.getCallbackQuery();
        String data  = cb.getData();
        long chatId  = cb.getMessage().getChatId();
        long userId  = cb.getFrom().getId();
        String cbId  = cb.getId();

        log.debug("Callback from user {}: {}", userId, data);
        bot.answerCallback(cbId);

        if (data.equals(KeyboardFactory.CB_BACK_MAIN)) {
            showMainMenu(chatId, cb.getFrom().getFirstName(), userId);
        } else if (data.equals(KeyboardFactory.CB_CATALOG)) {
            showCatalog(chatId);
        } else if (data.startsWith(KeyboardFactory.CB_CAT_PREFIX)) {
            showCategory(chatId, data.substring(KeyboardFactory.CB_CAT_PREFIX.length()));
        } else if (data.startsWith(KeyboardFactory.CB_PROD_PREFIX)) {
            showProduct(chatId, data.substring(KeyboardFactory.CB_PROD_PREFIX.length()));
        } else if (data.startsWith(KeyboardFactory.CB_ADD_PREFIX)) {
            addToCart(chatId, userId, data.substring(KeyboardFactory.CB_ADD_PREFIX.length()), cbId);
        } else if (data.startsWith(KeyboardFactory.CB_REMOVE_PREFIX)) {
            removeFromCart(chatId, userId, data.substring(KeyboardFactory.CB_REMOVE_PREFIX.length()));
        } else if (data.equals(KeyboardFactory.CB_CART)) {
            showCart(chatId, userId);
        } else if (data.equals(KeyboardFactory.CB_CLEAR_CART)) {
            clearCart(chatId, userId);
        } else if (data.equals(KeyboardFactory.CB_CHECKOUT)) {
            startCheckout(chatId, userId);
        } else if (data.equals(KeyboardFactory.CB_ORDERS)) {
            showOrders(chatId, userId);
        } else if (data.equals(KeyboardFactory.CB_SUPPORT)) {
            showSupport(chatId);
        } else if (data.equals(KeyboardFactory.CB_CANCEL)) {
            cancelCheckout(chatId, userId);
        } else {
            log.warn("Unknown callback data: {}", data);
        }
    }

    private void showMainMenu(long chatId, String firstName, long userId) {
        sessionService.resetCheckout(userId);
        SendMessage msg = buildMessage(chatId, MessageFormatter.mainMenuMessage());
        msg.setReplyMarkup(KeyboardFactory.mainMenuKeyboard());
        bot.sendText(msg);
    }

    private void showCatalog(long chatId) {
        SendMessage msg = buildMessage(chatId, MessageFormatter.catalogMessage());
        msg.setReplyMarkup(KeyboardFactory.catalogKeyboard());
        bot.sendText(msg);
    }

    private void showCategory(long chatId, String catKey) {
        Category category = Category.fromCallbackPrefix(catKey);
        if (category == null) return;
        List<Product> products = ProductCatalog.findByCategory(category);
        SendMessage msg = buildMessage(chatId,
                MessageFormatter.categoryMessage(category.getDisplayName(), products.size()));
        msg.setReplyMarkup(KeyboardFactory.productListKeyboard(products, category));
        bot.sendText(msg);
    }

    private void showProduct(long chatId, String productId) {
        ProductCatalog.findById(productId).ifPresent(product -> {
            try {
                SendPhoto photo = new SendPhoto();
                photo.setChatId(chatId);
                photo.setPhoto(new InputFile(product.getImageUrl()));
                photo.setCaption(MessageFormatter.productDetailMessage(product));
                photo.setParseMode("HTML");
                photo.setReplyMarkup(KeyboardFactory.productDetailKeyboard(product));
                bot.sendPhoto(photo);
            } catch (Exception e) {
                log.warn("Photo failed for {}, using text fallback", productId);
                SendMessage msg = buildMessage(chatId, MessageFormatter.productDetailMessage(product));
                msg.setReplyMarkup(KeyboardFactory.productDetailKeyboard(product));
                bot.sendText(msg);
            }
        });
    }

    private void addToCart(long chatId, long userId, String productId, String cbId) {
        ProductCatalog.findById(productId).ifPresent(product -> {
            cartService.addToCart(userId, product);
            bot.answerCallback(cbId, "✅ Добавлено в корзину!");
            SendMessage msg = buildMessage(chatId, MessageFormatter.productAddedMessage(product));
            msg.setReplyMarkup(KeyboardFactory.cartKeyboard(true));
            bot.sendText(msg);
        });
    }

    private void removeFromCart(long chatId, long userId, String productId) {
        ProductCatalog.findById(productId).ifPresent(product -> {
            cartService.removeFromCart(userId, productId);
            if (cartService.isCartEmpty(userId)) {
                SendMessage msg = buildMessage(chatId,
                        MessageFormatter.itemRemovedMessage(product.getName())
                                + "\n\n" + MessageFormatter.emptyCartMessage());
                msg.setReplyMarkup(KeyboardFactory.cartKeyboard(false));
                bot.sendText(msg);
            } else {
                showCart(chatId, userId);
            }
        });
    }

    private void showCart(long chatId, long userId) {
        var items = cartService.getCartItems(userId);
        SendMessage msg;
        if (items.isEmpty()) {
            msg = buildMessage(chatId, MessageFormatter.emptyCartMessage());
            msg.setReplyMarkup(KeyboardFactory.cartKeyboard(false));
        } else {
            msg = buildMessage(chatId,
                    MessageFormatter.cartMessage(items, cartService.getCartTotal(userId)));
            msg.setReplyMarkup(KeyboardFactory.cartWithItemsKeyboard(items));
        }
        bot.sendText(msg);
    }

    private void clearCart(long chatId, long userId) {
        cartService.clearCart(userId);
        SendMessage msg = buildMessage(chatId, MessageFormatter.cartClearedMessage());
        msg.setReplyMarkup(KeyboardFactory.cartKeyboard(false));
        bot.sendText(msg);
    }

    private void startCheckout(long chatId, long userId) {
        if (cartService.isCartEmpty(userId)) {
            SendMessage msg = buildMessage(chatId, MessageFormatter.emptyCartMessage());
            msg.setReplyMarkup(KeyboardFactory.cartKeyboard(false));
            bot.sendText(msg);
            return;
        }
        sessionService.setState(userId, UserSession.State.AWAITING_NAME);
        SendMessage msg = buildMessage(chatId, MessageFormatter.checkoutStartMessage());
        msg.setReplyMarkup(KeyboardFactory.cancelKeyboard());
        bot.sendText(msg);
    }

    private void showOrders(long chatId, long userId) {
        var orders = orderService.getOrdersForUser(userId);
        SendMessage msg;
        if (orders.isEmpty()) {
            msg = buildMessage(chatId, MessageFormatter.noOrdersMessage());
        } else {
            msg = buildMessage(chatId, MessageFormatter.orderHistoryMessage(orders));
        }
        msg.setReplyMarkup(KeyboardFactory.backToMainKeyboard());
        bot.sendText(msg);
    }

    private void showSupport(long chatId) {
        SendMessage msg = buildMessage(chatId, MessageFormatter.supportMessage());
        msg.setReplyMarkup(KeyboardFactory.backToMainKeyboard());
        bot.sendText(msg);
    }

    private void cancelCheckout(long chatId, long userId) {
        sessionService.resetCheckout(userId);
        SendMessage msg = buildMessage(chatId, MessageFormatter.checkoutCancelledMessage());
        msg.setReplyMarkup(KeyboardFactory.mainMenuKeyboard());
        bot.sendText(msg);
    }

    private SendMessage buildMessage(long chatId, String text) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText(text);
        msg.setParseMode("HTML");
        msg.disableWebPagePreview();
        return msg;
    }
}