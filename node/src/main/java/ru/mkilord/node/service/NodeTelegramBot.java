package ru.mkilord.node.service;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.mkilord.node.TextUtils;
import ru.mkilord.node.common.command.*;

import java.util.*;

import static lombok.AccessLevel.PRIVATE;
import static ru.mkilord.node.common.command.Step.*;


@FieldDefaults(level = PRIVATE, makeFinal = true)
@Slf4j
@Component
@AllArgsConstructor
public class NodeTelegramBot implements CommandRepository {

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

    public void send(MessageContext context, String message) {
        var outMsg = SendMessage.builder().chatId(context.getChatId()).text(message).build();
        producerService.produceAnswer(outMsg);
    }

    public void send(MessageContext context, InlineKeyboardMarkup keyboardMarkup) {
        var outMsg = SendMessage.builder().chatId(context.getChatId()).replyMarkup(keyboardMarkup).build();
        producerService.produceAnswer(outMsg);
    }

    public void send(MessageContext context, String message, InlineKeyboardMarkup keyboardMarkup) {
        var outMsg = SendMessage.builder().chatId(context.getChatId()).text(message).replyMarkup(keyboardMarkup).build();
        producerService.produceAnswer(outMsg);
    }

    // TODO 2024-12-20 13:03: Что нужно от юзера.
    // 1 регистрация - есть.
    // 2 профиль - изменение профиля. Пользователь вводит команду /profile потом ему
    //   предлагается ввести команду /edit_profile, и срабатывает изменение.
    // Просмотр встреч: Пользователь получает список доступных ему встреч. Затем
    // выбирает нужную встречу.
    // Пользователь может подписываться на клубы.
    // 3 запись на встречу, пользователь вводит команду встречи, и ему выдаёт
    /*У меня есть телеграмм бот. А так же команды к нему. При этом библиотека команд, работает следующим образом:
     * Есть команда которая служит точкой входа, затем когда я получаю команды, бот входит в режим получения ввода
     * пользователя от для этой команды. При этом, бот может и не входить в режим пользовательского, ввода если это не нужно.
     * Так устроены мои команды для моего телеграмм бота.
     * Теперь сам телеграмм бот. Телеграмм бот будет используется для разговорных клубов.
     * И в нём есть четыре роли. Это участник, организатор, модератор, администратор.
     * От бота требуется отображать участникам информацию о клубах, а так же возможность записываться на встречи, оставлять отзывы
     * и получать уведомления о встречи. Организатору в свою очередь требуются возможности чтобы публиковать встречи, и запускать отзывы.
     * А также получать всех кто записался на встречу. */
    private void addCommands(List<Command> list, Command... commands) {
        list.addAll(List.of(commands));
    }

    @Override
    public List<Command> getCommands() {
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
                    return REPEAT;
                });
        var commands = new ArrayList<Command>();
        //UnregisterCommands
        addCommands(commands,
                Command.create("/start")
                        .access(Role.USER)
                        .info("Используйте, чтобы получить стартовую информацию.")
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
                        .info("Позволяет зарегистрироваться.")
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
                            send(context, "ℹ️ Для того чтобы узнать, что умеет бот, используй команду /help! 🤖");
                            context.setUserRole(Role.MEMBER.toString());
                        }).build(),
                Command.create("/feedback")
                        .info("Чтобы пройти опрос.")
                        .access(Role.MEMBER)
                        .action(context -> {
                            send(context, "Поиск опросов!");
                            var isOk = new Random().nextBoolean();
                            if (isOk) {
                                send(context, "Введите оценку клуба немецкого от 0 до 10:");
                                return NEXT;
                            }
                            send(context, "Доступные опросы не найдены!");
                            return TERMINATE;
                        })
                        // TODO 2024-12-21 20:25: Нужно подумать как сделать POST для реплая.
                        .input(inputRate)
                        .input(context -> {
                            var msg = context.getUpdate().getMessage().getText();
                            if (TextUtils.isRange(msg, 0, 10)) {
                                send(context, "Оценка записана!");
                                return NEXT;
                            }
                            send(context, "Введите число от 0 до 10!");
                            return REPEAT;
                        }).build());


        var end = Command.create("/end")
                .info("Чтобы апнуть права хахахах")
                .access(Role.EMPLOYEES)
                .action(context -> {
                    context.setUserRole(Role.ADMIN.toString());
                    log.debug("Action command /end");
                    return NEXT;
                })
                .input(_ -> {
                    log.debug("/end action reply 1");
                    return NEXT;
                })
                .input(_ -> {
                    log.debug("/end action reply 2");
                    return NEXT;
                }).build();
        var profile = Command.create("/profile")
                .info("Информация о твоём профиле.")
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
                            📧 Email: %s""".formatted(firstName, lastName, middleName, phone, email));
                    send(context, "Чтобы изменить данные используй /edit_profile");
                }).build();

        addCommands(commands, Command.create("/edit_profile")
                .access(Role.MEMBER_EMPLOYEES)
                .action(context -> {
                    send(context, "Познакомимся вновь!");
                })
                .input(usernameInput, phoneInput, emailInput)
                .post(context -> send(context, "✅ Данные записаны!")).build());

        addCommands(commands, profile, end);


        addCommands(commands, Command.create("/clubs")
                .access(Role.USER)
                .action(context -> {
                    // TODO 2024-12-22 14:33: Лучше использовать uuid.
                    var list = new ArrayList<>(List.of("Немецкий", "Русский", "Английский"));
                    var buttons = list.stream().map(string -> {
                        var button = new InlineKeyboardButton(string);
                        button.setCallbackData(string);
                        button.setText(string);
                        return button;
                    }).toList();
                    var wrappedList = buttons.stream()
                            .map(Arrays::asList)
                            .toList();
                    var keyboard = InlineKeyboardMarkup.builder().keyboard(wrappedList).build();
                    send(context, "Выберите клуб", keyboard);
                }).input(context -> {
                    var club = context.getText();
                    send(context, "Вы выбрали клуб: " + club);

                    return NEXT;
                })
                .build());

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
