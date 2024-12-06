package ru.mkilord.dispatcher.controller;

import jakarta.annotation.PostConstruct;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static lombok.AccessLevel.PRIVATE;
import static ru.mkilord.dispatcher.controller.TelegramBot.CommandMenuBuilder.Button;

@FieldDefaults(makeFinal = true, level = PRIVATE)
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {
    String userName;
    @NonFinal
    UpdateController updateController;

//    private final Map<Long, Roles> userRoles = new HashMap<>();
//    private final Map<Long, SubStates> userStates = new HashMap<>();
//    private final Map<Long, Map<String, String>> userData = new HashMap<>();
//
//    enum SubStates {
//        WAITING_NAME,
//        WAITING_SURNAME,
//        WAITING_EMAIL,
//        WAITING_PHONE,
//        WAITING_DATA,
//        IDLE
//    }
//
//    enum Roles {
//        USER,
//        ADMIN,
//        ORGANIZER,
//        MANAGER
//    }


    public TelegramBot(String token, String username, UpdateController updateController) {
        super(token);
        this.userName = username;
        this.updateController = updateController;

        log.debug("Bot initialized with username %s".formatted(username));
    }

    @PostConstruct
    public void init() {
        updateController.registerBot(this);
    }

    @Override
    public String getBotUsername() {
        return userName;
    }


    @Override
    public void onUpdateReceived(Update update) {
        updateController.processUpdate(update);
        long chatId;
        if (update.hasMessage()) {
            chatId = update.getMessage().getChatId();
        } else if (update.hasCallbackQuery()) {
            chatId = update.getCallbackQuery().getMessage().getChatId();
        } else return;
        sendUserMenuWithRepliesButtons(chatId);


//        long chatId;
//        String messageText;
//
//        if (isMessage(update)) {
//            chatId = update.getMessage().getChatId();
//            messageText = update.getMessage().getText();
//            log.debug("Message text: " + messageText);
//        } else if (isButtonCallback(update)) {
//            chatId = update.getCallbackQuery().getMessage().getChatId();
//            messageText = update.getCallbackQuery().getData();
//            answerCallbackQuery(update.getCallbackQuery().getId());
//            log.debug("Button command: " + messageText);
//        } else {
//            return;
//        }
//
//        if (userNotHaveRole(chatId)) {
//            log.debug("User doesn't have role: " + chatId);
//            registerProcess(chatId);
//            return;
//        }
//
//        if (messageText.isBlank()) return;
//
//        switch (userRoles.get(chatId)) {
//            case USER:
//                processUserCommands(chatId, messageText);
//                break;
//            case MANAGER:
//                break;
//            case ADMIN:
//                break;
//            case ORGANIZER:
//                break;
//        }
    }

//    private boolean isButtonCallback(Update update) {
//        return update.hasCallbackQuery();
//    }
//
//    private boolean isMessage(Update update) {
//        return update.hasMessage() && update.getMessage().hasText();
//    }
//
//    private boolean userNotHaveRole(long chatId) {
//        return !userRoles.containsKey(chatId);
//    }
//
//    private void registerProcess(long chatId) {
//        userRoles.put(chatId, Roles.USER);
//        sendMsg(chatId, "Привет! Давай познакомимся!");
//        sendMsg(chatId, "Введите имя: ");
//        userStates.put(chatId, WAITING_NAME);
//    }

    //    private void processUserCommands(long chatId, String messageText) {
//        switch (messageText) {
//            case "/start":
//                sendMsg(chatId, "Добро пожаловать в бота!");
//                sendUserMenu(chatId);
//                userStates.put(chatId, IDLE);
//                break;
//            case "/role":
//                Roles role = userRoles.getOrDefault(chatId, Roles.USER);
//                sendMsg(chatId, "Ваша роль: " + role);
//                break;
//            case "/state":
//                SubStates state = userStates.getOrDefault(chatId, IDLE);
//                sendMsg(chatId, "Ваше состояние: " + state);
//                break;
//            case "/setorganizer":
//                if (isAdmin(chatId)) {
//                    userRoles.put(chatId, Roles.ORGANIZER);
//                    sendMsg(chatId, "Ваша роль обновлена до организатора.");
//                } else {
//                    sendMsg(chatId, "У вас нет прав для изменения роли.");
//                }
//                break;
//            case "/inputdata":
//                userStates.put(chatId, WAITING_DATA);
//                sendMsg(chatId, "Пожалуйста, введите данные:");
//                break;
//            default:
//                handleUserInput(chatId, messageText);
//                break;
//        }
//    }

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

    //
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
        sendMsg(chatId, "Выберите команду:", keyboard);
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
        sendMsg(chatId, "Выберите команду:", keyboard);
    }

    public void answerCallbackQuery(String callbackQueryId) {
        var answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackQueryId);
        execute(answer);
    }
//
//
//    private void handleUserInput(long chatId, String messageText) {
//        var state = userStates.getOrDefault(chatId, IDLE);
//        var currentUserData = userData.getOrDefault(chatId, new HashMap<>());
//        switch (state) {
//            case WAITING_NAME:
//                userStates.put(chatId, WAITING_SURNAME);
//                currentUserData.put("name", messageText);
//                userData.put(chatId, currentUserData);
//                sendMsg(chatId, "Введите фамилию:");
//                break;
//            case WAITING_SURNAME:
//                userStates.put(chatId, WAITING_EMAIL);
//                currentUserData.put("surname", messageText);
//                userData.put(chatId, currentUserData);
//                sendMsg(chatId, "Введите email");
//                break;
//            case WAITING_EMAIL:
//                userStates.put(chatId, WAITING_PHONE);
//                currentUserData.put("email", messageText);
//                userData.put(chatId, currentUserData);
//                sendMsg(chatId, "Введите телефон");
//                break;
//            case WAITING_PHONE:
//                userStates.put(chatId, IDLE);
//                var phone = messageText;
//                var name = currentUserData.get("name");
//                var surname = currentUserData.get("surname");
//                var email = currentUserData.get("email");
//                sendMsg(chatId, ("""
//                        Вы зарегистрированы!\s
//                        Имя: %s
//                        Фамилия: %s\s
//                        Телефон: %s\s
//                        Email: %s\s
//                        Ваши данные сохранены в базу данных.""").formatted(name, surname, phone, email));
//                break;
//            default:
//                sendMsg(chatId, "Неизвестная команда.\nИспользуйте /start для начала.");
//                break;
//        }
//    }
//
//    private boolean isAdmin(long chatId) {
//        return userRoles.getOrDefault(chatId, null).equals(Roles.ADMIN);
//    }
//

    public <T extends Serializable, Method extends BotApiMethod<T>> T execute(Method method) {
        try {
            return super.execute(method);
        } catch (TelegramApiException e) {
            log.error("Could not execute bot API method", e);
            return null;
        }
    }

    private void sendMsg(long chatId, String text, ReplyKeyboardMarkup keyboardMarkup) {
        var message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setReplyMarkup(keyboardMarkup);
        execute(message);
    }

    private void sendMsg(long chatId, String text, InlineKeyboardMarkup keyboardMarkup) {
        var message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setReplyMarkup(keyboardMarkup);
        execute(message);
    }

    public void sendMsg(long chatId, String text) {
        var message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        execute(message);
    }
}
