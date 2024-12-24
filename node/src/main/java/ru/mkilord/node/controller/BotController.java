package ru.mkilord.node.controller;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.mkilord.node.command.*;
import ru.mkilord.node.command.menu.Item;
import ru.mkilord.node.command.menu.Menu;
import ru.mkilord.node.model.Role;
import ru.mkilord.node.service.ProducerService;
import ru.mkilord.node.util.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static lombok.AccessLevel.PRIVATE;
import static ru.mkilord.node.command.Step.*;


@FieldDefaults(level = PRIVATE, makeFinal = true)
@Slf4j
@Component
@AllArgsConstructor
public class BotController implements CommandCatalog {

    CommandHandler commandHandler;
    ProducerService producerService;

    @PostConstruct
    public void init() {
        commandHandler.registerCommands(this,
                context -> send(context, """
                        ❌ Неверная команда!\s
                        Для получения справки используйте команду /help. 📘"""));
    }

    public void onMessageReceived(Update update) {
        commandHandler.process(update);
    }

    public void onCallbackQueryReceived(Update update) {
        commandHandler.process(update);
    }

    private void send(MessageContext context, String message) {
        var outMsg = SendMessage.builder().chatId(context.getChatId()).text(message).build();
        producerService.produceAnswer(outMsg);
    }

    private void send(MessageContext context, String message, InlineKeyboardMarkup keyboardMarkup) {
        var outMsg = SendMessage.builder().chatId(context.getChatId()).text(message).replyMarkup(keyboardMarkup).build();
        producerService.produceAnswer(outMsg);
    }

    /*USER - просто любой рандомный пользователь не прошедший регистрацию
     *   /start - выводит начальную информацию.
     *   /register - позволяет зарегистрироваться и получить права участника.
     *   /help - выводит список доступных команд.
     *
     *MEMBER - это уже зарегистрированный пользователь, который может полноценно взаимодействовать с клубами.
     *  /profile
     *  */

    private void addCommands(List<Command> list, Command... commands) {
        list.addAll(List.of(commands));
    }

    @Override
    public List<Command> setCommands() {
        //Common Inputs:
        var usernameInput = Reply.builder()
                .preview(context -> send(context, "Пожалуйста, введи своё ФИО через пробел. ✍️"))
                .action(context -> {
                    var msg = context.getText();
                    if (msg.isBlank() || !msg.contains(" ")) {
                        send(context, "❗ Введите ФИО (имя, фамилию и отчество) через пробел.");
                        return REPEAT;
                    }
                    String[] fullName = msg.split(" ");
                    if (fullName.length < 3) {
                        send(context, "❗ Пожалуйста, укажите полное ФИО (имя, фамилию и отчество).");
                        return REPEAT;
                    }
                    context.put("firstName", fullName[0]);
                    context.put("lastName", fullName[1]);
                    context.put("middleName", fullName[2]);
                    return NEXT;
                });
        var emailInput = Reply.builder()
                .preview(context -> send(context, "Пожалуйста, введи свой email. 📧"))
                .action(context -> {
                    context.put("email", context.getText());
                    return NEXT;
                });
        var phoneInput = Reply.builder()
                .preview(context -> send(context, "Пожалуйста, введи свой телефон. 📱"))
                .action(context -> {
                    var msg = context.getText();
                    if (!TextUtils.isProneNumber(msg)) {
                        send(context, "❗ Введи номер телефона в формате: \"79106790783\".");
                        return REPEAT;
                    }
                    context.put("phone", msg);
                    return NEXT;
                });
        var inputRate = Reply.builder()
                .preview(context -> send(context, "Введите число от 0 до 10!"))
                .action(context -> {
                    var msg = context.getUpdate().getMessage().getText();
                    if (TextUtils.isRange(msg, 0, 10)) {
                        send(context, "Оценка записана!");
                        return NEXT;
                    }
                    send(context, "❗ Введите число от 0 до 10!");
                    return REPEAT;
                });
        var commands = new ArrayList<Command>();

        var helpMenu = Menu.builder().items(new Item("/help", "Команды")).build();
        //UnregisterCommands
        addCommands(commands,
                Command.create("/start")
                        .access(Role.USER)
                        .help("Используйте, чтобы получить стартовую информацию.")
                        .action((context -> {
                            send(context, """
                                    👋 Привет! Это бот для разговорных клубов.
                                    
                                    Здесь ты можешь:
                                    
                                    - 📅 Записаться на встречи клубов.
                                    - ✍️ Оставить отзыв.
                                    - 🔔 Подписаться на уведомления о встречах клубов.
                                    
                                    Для начала, зарегистрируйся с помощью команды /register.
                                    
                                    📘 Чтобы получить справку используй /help.
                                    """);
                        })).build(),
                Command.create("/register")
                        .help("Позволяет зарегистрироваться.")
                        .action(context -> {
                            send(context, "Привет! Давай познакомимся! 😊");
                        })
                        .input(usernameInput/*, phoneInput, emailInput*/)
                        .post(context -> {
                            var firstName = context.getValue("firstName");
                            var lastName = context.getValue("lastName");
                            var middleName = context.getValue("middleName");
                            var email = context.getValue("email");
                            var phone = context.getValue("phone");
                            send(context, """
                                    ✅ Данные записаны!
                                    
                                    Ваши данные:
                                    📛 ФИО: %s %s %s
                                    📱 Телефон: %s
                                    📧 Email: %s""".formatted(firstName, lastName, middleName, phone, email));
                            send(context, "ℹ️ Чтобы узнать команды бота, используй /help\nили кнопку ниже! 🤖", helpMenu.showMenu());
                            context.setUserRole(Role.MEMBER.toString());
                        }).build());


        //Member Commands
        var clubMenu = Menu.builder()
                .items(new Item("Встречи", context -> {
                    //Запрашиваем доступные встречи.
                    var meets = new ArrayList<>(List.of("20.11.2024 18:00", "24.11.2024 12:20"));
                    var items = meets.stream().map(string -> new Item(string, string)).toList();
                    var menu = Menu.builder()
                            .items(items)
                            .build();
                    send(context, "Доступные встречи: ", menu.showMenu());
                    return NEXT;
                }), new Item("Подписаться", context -> {
                    //Подписываем его на клуб.
                    send(context, "Подписка оформлена!");
                    return TERMINATE;
                }), new Item("О клубе", context -> {
                    //Отправляем информацию о клубе.
                    send(context, "Информация о клубе: " + context.getValue("club"));
                    return TERMINATE;
                }))
                .build();
        var clubsCommand = Command.create("/clubs")
                .access(Role.MEMBER)
                .help("Записаться на встречу, получить информацию о клубе.")
                .action(context -> {
                    var clubs = new ArrayList<>(List.of("Немецкий", "Русский", "Английский"));
                    // Запросить информацию о клубах
                    var items = clubs.stream().map(string -> new Item(string, string)).toList();
                    var menu = Menu.builder()
                            .items(items)
                            .build();
                    send(context, "Выберите клуб", menu.showMenu());
                })
                .input(context -> {
                    var club = context.getText();
                    send(context, "Вы выбрали клуб: " + club);
                    send(context, club + " клуб:", clubMenu.showMenu());
                    // Запросить выбранный клуб.
                    context.put("club", club);
                })
                .input(clubMenu::onClick)
                .input(context -> {
                    //Зарегистрировать встречу
                    send(context, "Отметил что ты придёшь на встречу. " + context.getText());
                    return TERMINATE;
                })
                .build();

        var feedback = Command.create("/feedback")
                .help("Чтобы пройти опрос.")
                .access(Role.MEMBER)
                .action(context -> {
                    send(context, "Поиск опросов!");
                    //Запросить информацию об опросах.
                    var isOk = new Random().nextBoolean();
                    if (isOk) {
                        send(context, "Введите оценку клуба немецкого от 0 до 10:");
                        return NEXT;
                    }
                    send(context, "Доступные опросы не найдены!");
                    return TERMINATE;
                })
                .input(inputRate)
                .input(context -> {
                    var msg = context.getUpdate().getMessage().getText();
                    if (TextUtils.isRange(msg, 0, 10)) {
                        send(context, "Оценка записана!");
                        //Записать оценку.
                        return NEXT;
                    }
                    send(context, "Введите число от 0 до 10!");
                    return REPEAT;
                }).build();

        var profileMenu = Menu.builder()
                .items(new Item("/edit_profile", "Изменить"))
                .build();
        var profile = Command.create("/profile")
                .help("Информация о твоём профиле.")
                .access(Role.MEMBER_EMPLOYEES)
                .action(context -> {
                    send(context, "Твоя роль: %s".formatted(context.getUserRole()));
                    var firstName = context.getValue("firstName");
                    var lastName = context.getValue("lastName");
                    var middleName = context.getValue("middleName");
                    var email = context.getValue("email");
                    var phone = context.getValue("phone");
                    send(context, """
                            Ваши данные:
                            📛 ФИО: %s %s %s
                            📱 Телефон: %s
                            📧 Email: %s""".formatted(firstName, lastName, middleName, phone, email), profileMenu.showMenu());
                }).build();
        var editProfile = Command.create("/edit_profile")
                .access(Role.MEMBER_EMPLOYEES)
                .action(context -> {
                    send(context, "Познакомимся вновь!");
                })
                .input(usernameInput, phoneInput, emailInput)
                .post(context -> send(context, "✅ Данные записаны!")).build();

        var debugMenu = Menu.builder().items(
                new Item("Получить Админа", context -> {
                    context.setUserRole(Role.ADMIN.toString());
                    return TERMINATE;
                }),
                new Item("Получить Участника", context -> {
                    context.setUserRole(Role.MEMBER.toString());
                    return TERMINATE;
                }),
                new Item("Получить Организатор", context -> {
                    context.setUserRole(Role.ORGANIZER.toString());
                    return TERMINATE;
                })
        ).build();

        var debug = Command.create("/debug").access(Role.ALL).action(context -> {
            send(context, "Выберите функцию:", debugMenu.showMenu());
        }).input(debugMenu::onClick).build();

        addCommands(commands, debug, clubsCommand, feedback, profile, editProfile);
        var help = Command.create("/help")
                .access(Role.ALL)
                .action(context -> {
                    var strBuilder = new StringBuilder();
                    commands.stream()
                            .filter(command ->
                                    (command.getRoles().contains(context.getUserRole()))
                                            && Objects.nonNull(command.getInfo()))
                            .forEach(command -> strBuilder
                                    .append(" - ")
                                    .append(command.getName())
                                    .append(" - ")
                                    .append(command.getInfo())
                                    .append("\n"));
                    var outMsg = "📘 Команды:\n\n";
                    if (strBuilder.isEmpty()) {
                        outMsg = "Пока что для вас нет доступных команд!";
                    } else {
                        outMsg += strBuilder.toString();
                    }
                    send(context, outMsg);
                    return TERMINATE;
                }).build();

        addCommands(commands, help);
        return commands;
    }
}
