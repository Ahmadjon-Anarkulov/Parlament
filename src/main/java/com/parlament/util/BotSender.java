package com.parlament.util;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
public class BotSender {

    public static void send(AbsSender bot, Long chatId, String text) {
        send(bot, chatId, text, null);
    }

    public static void send(AbsSender bot, Long chatId, String text, ReplyKeyboard keyboard) {
        try {
            SendMessage msg = SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .parseMode("Markdown")
                    .replyMarkup(keyboard)
                    .build();
            bot.execute(msg);
        } catch (TelegramApiException e) {
            log.warn("send failed to {}: {}", chatId, e.getMessage());
        }
    }

    public static void sendPhoto(AbsSender bot, Long chatId, String fileIdOrUrl,
                                  String caption, InlineKeyboardMarkup kb) {
        try {
            SendPhoto sp = SendPhoto.builder()
                    .chatId(chatId)
                    .photo(new InputFile(fileIdOrUrl))
                    .caption(caption)
                    .parseMode("Markdown")
                    .replyMarkup(kb)
                    .build();
            bot.execute(sp);
        } catch (TelegramApiException e) {
            log.warn("sendPhoto failed: {}", e.getMessage());
        }
    }

    public static void edit(AbsSender bot, Long chatId, Integer msgId, String text,
                             InlineKeyboardMarkup kb) {
        try {
            bot.execute(EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(msgId)
                    .text(text)
                    .parseMode("Markdown")
                    .replyMarkup(kb)
                    .build());
        } catch (TelegramApiException e) {
            log.warn("edit failed: {}", e.getMessage());
        }
    }

    public static void answerCallback(AbsSender bot, String callbackId, String text, boolean alert) {
        try {
            bot.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackId)
                    .text(text)
                    .showAlert(alert)
                    .build());
        } catch (TelegramApiException e) {
            log.warn("answerCallback failed: {}", e.getMessage());
        }
    }

    public static void delete(AbsSender bot, Long chatId, Integer msgId) {
        try {
            bot.execute(DeleteMessage.builder().chatId(chatId).messageId(msgId).build());
        } catch (TelegramApiException e) {
            log.warn("delete failed: {}", e.getMessage());
        }
    }
}
