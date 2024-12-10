package ru.mkilord.node.service;

import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

import static lombok.AccessLevel.PRIVATE;
import static ru.mkilord.node.service.ProcessCommand.CommandMenuBuilder.Button;

public class ProcessCommand {

    @FieldDefaults(level = PRIVATE, makeFinal = true)
    public static class InlineMenuBuilder {
        @NonFinal
        List<InlineKeyboardButton> currentRow = new ArrayList<>();
        List<List<InlineKeyboardButton>> keyboardRows = new ArrayList<>();

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();

        static InlineKeyboardButton.InlineKeyboardButtonBuilder getButton() {
            return InlineKeyboardButton.builder();
        }

        InlineMenuBuilder with(InlineKeyboardButton.InlineKeyboardButtonBuilder button) {
            currentRow.add(button.build());
            return this;
        }

        InlineMenuBuilder nextRow() {
            keyboardRows.add(currentRow);
            currentRow = new ArrayList<>();
            return this;
        }

        InlineKeyboardMarkup compile() {
            if (!keyboardRows.getLast().equals(currentRow)) {
                keyboardRows.add(currentRow);
            }
            keyboardMarkup.setKeyboard(keyboardRows);
            return keyboardMarkup;
        }
    }

    private void sendUserMenu(long chatId) {
        var keyboard = new CommandMenuBuilder()
                .with(Button()
                        .text("Старт")
                )
                .with(Button()
                        .text("Выбрать клуб"))
                .nextRow()
                .with(Button()
                        .text("Отзывы"))
                .with(Button()
                        .text("Профиль"))
                .compile();
        var outMsg = SendMessage.builder()
                .chatId(chatId)
                .text("Выберите команду:")
                .replyMarkup(keyboard)
                .build();
    }

    private void sendUserMenuWithRepliesButtons(long chatId) {
        var keyboard = new InlineMenuBuilder()
                .with(InlineMenuBuilder.getButton()
                        .text("Старт")
                        .callbackData("/start"))
                .with(InlineMenuBuilder.getButton()
                        .text("Выбрать клуб")
                        .callbackData("/clubs")
                )
                .nextRow()
                .with(InlineMenuBuilder.getButton()
                        .text("Отзывы")
                        .callbackData("/feedback"))
                .with(InlineMenuBuilder.getButton()
                        .text("Профиль")
                        .callbackData("/profile"))
                .compile();
        var outMsg = SendMessage.builder()
                .chatId(chatId)
                .text("Выберите команду:")
                .replyMarkup(keyboard)
                .build();
//        sendMsg(outMsg);
    }

    @FieldDefaults(makeFinal = true, level = PRIVATE)
    public static class CommandMenuBuilder {
        @NonFinal
        KeyboardRow currentRow = new KeyboardRow();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        static KeyboardButton.KeyboardButtonBuilder Button() {
            return KeyboardButton.builder();
        }

        CommandMenuBuilder with(KeyboardButton.KeyboardButtonBuilder button) {
            currentRow.add(button.build());
            return this;
        }

        CommandMenuBuilder nextRow() {
            keyboardRows.add(currentRow);
            currentRow = new KeyboardRow();
            return this;
        }

        ReplyKeyboardMarkup compile() {
            if (!keyboardRows.getLast().equals(currentRow)) {
                keyboardRows.add(currentRow);
            }
            keyboardMarkup.setKeyboard(keyboardRows);
            return keyboardMarkup;
        }
    }
}
