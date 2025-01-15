package ru.mkilord.node.controller;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.mkilord.node.common.Command;
import ru.mkilord.node.common.CommandCatalog;
import ru.mkilord.node.common.CommandHandler;
import ru.mkilord.node.common.Reply;
import ru.mkilord.node.common.context.ContextFlow;
import ru.mkilord.node.common.context.MessageContext;
import ru.mkilord.node.common.menu.Item;
import ru.mkilord.node.common.menu.Menu;
import ru.mkilord.node.config.BotConfig;
import ru.mkilord.node.model.Club;
import ru.mkilord.node.model.Meet;
import ru.mkilord.node.model.User;
import ru.mkilord.node.model.enums.MeetStatus;
import ru.mkilord.node.model.enums.Role;
import ru.mkilord.node.service.ProducerService;
import ru.mkilord.node.service.impl.ClubService;
import ru.mkilord.node.service.impl.MeetService;
import ru.mkilord.node.service.impl.UserService;
import ru.mkilord.node.util.TextUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static lombok.AccessLevel.PRIVATE;
import static ru.mkilord.node.common.Step.*;
import static ru.mkilord.node.util.MeetFormatter.formatForItemWithStatus;
import static ru.mkilord.node.util.MeetFormatter.formatMeetWithOutStatus;


@FieldDefaults(level = PRIVATE, makeFinal = true)
@Slf4j
@Component
@AllArgsConstructor
public class BotController implements CommandCatalog {

    CommandHandler commandHandler;
    BotConfig botConfig;

    ProducerService producerService;
    ContextFlow contextFlow;

    UserService userService;
    ClubService clubService;
    MeetService meetService;

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
     *  /profile - профиль пользователя.
     *  /feedback - текущие опросы.
     *  /meets - текущие встречи.
     *  /clubs - просмотр клубов, подписка на уведомления и на встречу
     *
     *ORGANIZER - Это тот кто создаёт встречи. Закреплён за клубом.
     * /manage_clubs - отображает список клубов которые он контролирует
     *      К: Создать встречу. -> Тема встречи -> Дата -> Время. ?> Запустить рассылку
     * /control_meeting - отображает все предстоящие встречи.
     *      В: Запустить рассылку. (Уведомить подписчиков клуба)
     *      В: Получить список с участниками.
     *      В: Отметить проведённой. -> Запустить опрос.
     *      В: Отменить.
     *      В: Удалить.
     * /control_feedback - отображает общую оценку клуба. И оценки последних 5 встреч.
     *MANAGER - Это тот кто создаёт клубы. И закрепляет за ними сотрудников.
     * /create_club - позволяет создать клуб.
     *  */


    private void addCommands(List<Command> list, Command... commands) {
        list.addAll(List.of(commands));
    }

    @Override
    public List<Command> setCommands() {
        //Common Inputs:
        var usernameInput = Reply.builder()
                .preview((Consumer<MessageContext>)
                        context -> send(context, "Пожалуйста, введи своё ФИО через пробел. ✍️"))
                .action(context -> {
                    var msg = context.getText();
                    if (msg.length() > 250) {
                        send(context, "❗ ФИО не может быть длиннее 250 символов!");
                        return REPEAT;
                    }
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
                .preview((Consumer<MessageContext>)
                        context -> send(context, "Пожалуйста, введи свой email. 📧"))
                .action(context -> {
                    var email = context.getText();

                    var emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
                    var pattern = Pattern.compile(emailRegex);

                    if (email == null || !pattern.matcher(email).matches()) {
                        send(context, "❗ Неверный формат email! Пожалуйста, введите корректный email.");
                        return REPEAT;
                    }
                    if (email.length() > 250) {
                        send(context, "❗ Email не может быть длиннее 250 символов!");
                        return REPEAT;
                    }
                    context.put("email", email);
                    return NEXT;
                });
        var phoneInput = Reply.builder()
                .preview((Consumer<MessageContext>)
                        context -> send(context, "Пожалуйста, введи свой телефон. 📱"))
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
                .preview((Consumer<MessageContext>)
                        context -> send(context, "Введите число от 0 до 10!"))
                .action(context -> {
                    var msg = context.getText();
                    if (!TextUtils.isRange(msg, 0, 10)) {
                        send(context, "❗ Введите число от 0 до 10!");
                        return REPEAT;
                    }
                    context.put("rate", msg);
                    return NEXT;
                });
        var inputClubName = Reply.builder().preview((Consumer<MessageContext>)
                        context -> send(context, "Введите название клуба."))
                .action(context -> {
                    var msg = context.getText();
                    if (msg.isBlank() || msg.length() < 2) {
                        send(context, "❗ Введите имя клуба длиннее 2х символов!");
                        return REPEAT;
                    }
                    if (msg.length() > 50) {
                        send(context, "❗ Имя не может быть длиннее 50 символов!");
                        return REPEAT;
                    }
                    context.put("clubName", msg);
                    return NEXT;
                });
        var inputClubDescription = Reply.builder().preview((Consumer<MessageContext>)
                        context -> send(context, "Введите описание клуба."))
                .action(context -> {
                    var msg = context.getText();
                    if (msg.isBlank() || msg.length() < 20) {
                        send(context, "❗ Введите Описание клуба 20ти символов!");
                        return REPEAT;
                    }
                    if (msg.length() > 250) {
                        send(context, "❗ Описание должно быть не длиннее 250 символов!");
                        return REPEAT;
                    }
                    context.put("clubDescription", msg);
                    return NEXT;
                });

        var inputSelectClub = Reply.builder()
                .preview(context -> {
                    var clubs = new ArrayList<>(clubService.getAll());
                    var items = clubs.stream()
                            .map(club -> new Item(String.valueOf(club.getId()), club.getName()))
                            .toList();
                    if (items.isEmpty()) {
                        send(context, "Здесь пока не доступных клубов!");
                        return TERMINATE;
                    }
                    var menu = Menu.builder()
                            .items(items)
                            .build();
                    context.put("clubs", clubs);
                    send(context, "Выберите клуб:", menu.showMenu(context));
                    return NEXT;
                })
                .action(context -> {
                    if (Menu.isInvalidInput(context)) return INVALID;

                    var clubId = Long.parseLong(context.getText());
                    var clubs = context.getValues("clubs", Club.class);

                    clubs.stream()
                            .filter(club -> club.getId() == clubId)
                            .findFirst()
                            .ifPresentOrElse(club -> context.put("club", club),
                                    () -> send(context, "Клуб не найден"));
                    return NEXT;
                });

        var commands = new ArrayList<Command>();

        var helpMenu = Menu.builder().items(new Item("/help", "Команды")).build();

        /*USER - просто любой рандомный пользователь не прошедший регистрацию
         *   /start - выводит начальную информацию.
         *   /register - позволяет зарегистрироваться и получить права участника.
         *   /help - выводит список доступных команд.
         */

        var startCommand = Command.create("/start")
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
                })).build();

        var registerCommand = Command.create("/register")
                .help("Позволяет зарегистрироваться.")
                .action(context -> {
                    send(context, "Привет! Давай познакомимся! 😊");
                })
                .input(usernameInput, phoneInput, emailInput)
                .post(context -> {
                    var user = context.getUser();
                    user.setFirstName(context.getValue("firstName"));
                    user.setLastName(context.getValue("lastName"));
                    user.setMiddleName(context.getValue("middleName"));
                    user.setEmail(context.getValue("email"));
                    user.setPhone(context.getValue("phone"));

                    if (Long.parseLong(botConfig.getAdminId()) == context.getUser().getTelegramId())
                        user.setRole(Role.MODERATOR);
                    else
                        user.setRole(Role.MEMBER);
                    userService.update(user);
                    send(context, """
                            ✅ Данные записаны!
                            
                            Ваши данные:
                            📛 ФИО: %s %s %s
                            📱 Телефон: %s
                            📧 Email: %s""".formatted(user.getFirstName(), user.getLastName(), user.getMiddleName(),
                            user.getPhone(), user.getEmail()));
                    send(context, "ℹ️ Чтобы узнать команды бота, используй /help\nили кнопку ниже! 🤖",
                            helpMenu.showMenu(context));
                }).build();

        /*ALL - все пользователи
         *   /help - выводит список доступных команд.
         */

        var help = Command.create("/help")
                .access(Role.ALL)
                .action(context -> {
                    var strBuilder = new StringBuilder();
                    var userRole = context.getUser().getRole();
                    commands.stream()
                            .filter(command ->
                                    (command.getRoles().contains(userRole.toString()))
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
        var profileMenu = Menu.builder()
                .items(new Item("/edit_profile", "Изменить"))
                .build();

        /*MEMBER_AND_EMPLOYEES - все участники и сотрудники
         *   /profile - Информация о профиле.
         *   /edit_profile - позволяет редактировать профиль.
         *   /delete_account - удаляет аккаунт пользователя.
         */

        var profile = Command.create("/profile")
                .help("Информация о твоём профиле.")
                .access(Role.MEMBER_AND_EMPLOYEES)
                .action(context -> {
                    var user = context.getUser();
                    send(context, """
                            Ваши данные:
                            
                            📛 ФИО: %s %s %s
                            📱 Телефон: %s
                            📧 Email: %s""".formatted(user.getFirstName(), user.getLastName(),
                            user.getMiddleName(), user.getPhone(), user.getEmail()), profileMenu.showMenu(context));
                }).build();

        var editProfile = Command.create("/edit_profile")
                .access(Role.MEMBER_AND_EMPLOYEES)
                .action(context -> {
                    send(context, "Познакомимся вновь!");
                })
                .input(usernameInput, phoneInput, emailInput)
                .post(context -> {
                    var user = context.getUser();
                    user.setFirstName(context.getValue("firstName"));
                    user.setLastName(context.getValue("lastName"));
                    user.setMiddleName(context.getValue("middleName"));
                    user.setEmail(context.getValue("email"));
                    user.setPhone(context.getValue("phone"));
                    userService.update(user);
                    send(context, "✅ Данные записаны!");
                }).build();

        var deleteUserCommand = Command.create("/delete_account")
                .help("Удаляет твои данные на всегда.")
                .access(Role.MEMBER_AND_EMPLOYEES)
                .input(Reply.builder().preview(context -> {
                            var dialogMenu = Menu.builder().items(
                                    new Item("Да", _ -> {
                                        userService.deleteById(context.getUser().getTelegramId());
                                        send(context, "Ваши данные удалены! 👋");
                                        send(context, "Используйте команду /start чтобы начать сначала!");
                                        contextFlow.disposeContext(context);
                                        return TERMINATE;
                                    }),
                                    new Item("Нет", _ -> {
                                        send(context, "Ничего не удалено 😊" +
                                                "\n\uD83D\uDCD8 Используй /help для чтобы узнать команды бота!");
                                        return TERMINATE;
                                    })
                            ).build();
                            send(context, "🚮 Вы действительно хотите удалить данные?", dialogMenu.showMenu(context));
                        })
                        .action(context -> context.getMenu().onClick(context)
                        )).build();

        /* MEMBER - это уже зарегистрированный пользователь, который может полноценно взаимодействовать с клубами.
         *  /clubs - просмотр клубов, подписка на уведомления и на встречу
         *  /meets - текущие встречи.
         *  /feedback - текущие опросы.
         */

        var clubsCommand = Command.create("/clubs")
                .access(Role.MEMBER_AND_EMPLOYEES)
                .help("Записаться на встречу, получить информацию о клубе.")
                .input(inputSelectClub)
                .input(Reply.builder().preview(context -> {
                            var club = context.getValue("club", Club.class);
                            var isUserSubscribed = userService.isUserSubscribeToClub(context.getUser().getTelegramId(), club.getId());
                            var items = new ArrayList<Item>();
                            items.add(new Item("Записаться", _ -> {
                                var meets = meetService.getPublishedMeetsByClubId(club.getId());
                                if (meets.isEmpty()) {
                                    send(context, "Нет доступных встреч!");
                                    return TERMINATE;
                                }
                                var itemsMeets = meets.stream()
                                        .map(meet -> new Item(String.valueOf(meet.getId()), formatMeetWithOutStatus(meet))).toList();
                                var menu = Menu.builder()
                                        .items(itemsMeets)
                                        .build();
                                send(context, "Доступные встречи: ", menu.showMenu(context));
                                return NEXT;
                            }));
                            if (isUserSubscribed) {
                                items.add(new Item("Отписаться", _ -> {
                                    clubService.removeSubscriber(club.getId(), context.getUser());
                                    send(context, "📴 Вы отписались! \n Теперь никаких уведомлений от этого клуба!");
                                    return TERMINATE;
                                }));
                            } else {
                                items.add(new Item("Подписаться", _ -> {
                                    clubService.addSubscriber(club.getId(), context.getUser());
                                    send(context, "\uD83D\uDFE2 Подписка оформлена! \n\n\uD83D\uDCDD Теперь когда клуб опубликует новую встречу, мы пришлём уведомление!");
                                    send(context, "\uD83D\uDCD8 Используй /clubs чтобы выбрать клуб и записаться на встречу.");
                                    return TERMINATE;
                                }));
                            }
                            items.add(new Item("О клубе", _ -> {
                                send(context, "ℹ️ Информация о клубе:\n\n"
                                        + club.getName() + "\n\n"
                                        + club.getDescription() + "\n"
                                        + "Рейтинг: " + club.getAverageRating() +
                                        "\n\n\uD83D\uDCD8 Используй /clubs чтобы выбрать клуб и записаться на встречу.");
                                return TERMINATE;
                            }));

                            var menu = Menu.builder()
                                    .items(items)
                                    .build();
                            send(context, club.getName() + " клуб:", menu.showMenu(context));

                        })
                        .action(context -> context.getMenu().onClick(context)))
                .input(context -> {
                    if (Menu.isInvalidInput(context)) return INVALID;
                    var meetId = context.getText();
                    meetService.addUserToMeet(Long.valueOf(meetId), context.getUser());
                    send(context, "✔️ Отметил что ты придёшь на встречу.\n\n\uD83D\uDCD8 Чтобы просмотреть свои встречи используй /meets");
                    return TERMINATE;
                })
                .build();

        var meetsCommand = Command.create("/meets")
                .help("Позволяет просматривать запланированные встречи.")
                .access(Role.MEMBER_AND_EMPLOYEES)
                .input(Reply.builder().preview(context -> {
                            var meets = userService.getRegisteredMeetsWithStatus(context.getUser().getTelegramId(), MeetStatus.PUBLISHED);
                            if (meets.isEmpty()) {
                                send(context, "У тебя нет встреч!\n\uD83D\uDCD8 Используй /clubs, чтобы записаться на встречу!");
                                return TERMINATE;
                            }
                            var items = meets.stream().map(meet -> new Item(String.valueOf(meet.getId()), formatForItemWithStatus(meet))).toList();
                            var menu = Menu.builder()
                                    .items(items)
                                    .build();
                            context.put("meets", meets);
                            send(context, "Ваши встречи: ", menu.showMenu(context));
                            return NEXT;
                        })
                        .action(context -> {
                            if (Menu.isInvalidInput(context)) return INVALID;
                            var meets = context.getValues("meets", Meet.class);
                            var meetId = Long.parseLong(context.getText());
                            meets.stream()
                                    .filter(meet -> meet.getId() == meetId)
                                    .findFirst()
                                    .ifPresentOrElse(meet -> context.put("meet", meet),
                                            () -> send(context, "Встреча не найдена!"));
                            return NEXT;
                        }))
                .input(Reply.builder().preview(context -> {
                            var meet = context.getValue("meet", Meet.class);
                            var items = new ArrayList<Item>();
                            if (meet.getStatus() == MeetStatus.PUBLISHED) {
                                items.add(new Item("Отменить регистрацию", _ -> {
                                    meetService.removeUserFromMeet(meet.getId(), context.getUser());
                                    send(context, "Отменил твою регистрацию!\n\uD83D\uDCD8 Чтобы просмотреть свои встречи используй /meets");
                                    return TERMINATE;
                                }));
                            }
                            var menu = Menu.builder()
                                    .items(items)
                                    .build();
                            send(context, "Выберите действие встречи:\n\n" + formatForItemWithStatus(meet), menu.showMenu(context));
                            return NEXT;
                        })
                        .action(context -> context.getMenu().onClick(context)))
                .build();

        var feedback = Command.create("/feedback")
                .help("Чтобы оставить оценку клубу.")
                .access(Role.MEMBER_AND_EMPLOYEES)
                .input(inputSelectClub)
                .input(inputRate)
                .input(Reply.builder().preview(context -> {
                    var club = context.getValue("club", Club.class);
                    var rate = Integer.parseInt(context.getValue("rate"));
                    clubService.addRating(club.getId(), rate);
                    send(context, "Оценка записана!");
                })).build();

        /* ORGANIZER - Это тот кто создаёт встречи. Закреплён за клубом.
         * /control_meeting - отображает список клубов
         *
         *      В: Встречи -> Показывает меню со встречами.
         *          В*: Опубликовать.
         *          В: Получить список с участниками.
         *          В*: Отметить проведённой. ?> Запустить опрос.
         *          В*: Отменить.
         *          В*: Удалить.
         * /create_meeting - позволяет создать встречу.
         *      К: Создать встречу. -> Тема встречи -> Дата -> Время. ?> Опубликовать
         */

        var inputMeetName = Reply.builder().preview((Consumer<MessageContext>) context ->
                        send(context, "Введите тему встречи:"))
                .action(context -> {
                    var msg = context.getText();
                    if (msg.isBlank() || msg.length() < 20) {
                        send(context, "❗ Введите тему встречи от 20-ти символов!");
                        return REPEAT;
                    }
                    if (msg.length() > 250) {
                        send(context, "❗ Название встречи не может быть длиннее 250 символов!");
                        return REPEAT;
                    }
                    context.put("meetName", msg);
                    return NEXT;
                });

        var inputMeetDate = Reply.builder().preview((Consumer<MessageContext>) context ->
                        send(context, "Введите дату встречи в формате ДД:ММ:ГГГГ"))
                .action(context -> {
                    var msg = context.getText();
                    try {
                        var formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                        var date = LocalDate.parse(msg, formatter);
                        var now = LocalDate.now();
                        if (date.isBefore(now) || date.isAfter(now.plusMonths(1))) {
                            send(context, "❗ Введите дату в пределах от сегодняшнего дня до 1-го месяца вперёд.");
                            return REPEAT;
                        }
                        context.put("meetDate", date.toString());
                        return NEXT;
                    } catch (DateTimeParseException e) {
                        send(context, "❗ Неверный формат даты! Убедитесь, что используете формат DD.MM.YYYY.");
                        return REPEAT;
                    }
                });

        var inputMeetTime = Reply.builder().preview((Consumer<MessageContext>) context ->
                        send(context, "Введите время встречи в формате ЧЧ:ММ (24-часовой формат):"))
                .action(context -> {
                    var msg = context.getText();
                    try {
                        var formatter = new DateTimeFormatterBuilder()
                                .appendPattern("[H:mm][HH:mm][H:m][HH:m]")
                                .toFormatter();
                        var time = LocalTime.parse(msg, formatter);

                        var meetDateStr = context.getValue("meetDate");
                        LocalDate meetDate = null;
                        if (meetDateStr != null) meetDate = LocalDate.parse(meetDateStr);


                        var now = LocalDateTime.now();
                        var start = LocalTime.of(8, 0);
                        var end = LocalTime.of(22, 0);

                        if (time.isBefore(start) || time.isAfter(end)) {
                            send(context, "❗ Введите время в диапазоне от 08:00 до 22:00!");
                            return REPEAT;
                        }

                        if (meetDate != null && meetDate.equals(LocalDate.now())) {
                            var todayTime = LocalDateTime.of(LocalDate.now(), time);
                            if (todayTime.isBefore(now)) {
                                send(context, "❗ Время уже прошло. Введите время, которое ещё не наступило.");
                                return REPEAT;
                            }
                        }
                        context.put("meetTime", time.toString());
                        return NEXT;
                    } catch (DateTimeParseException e) {
                        send(context, "❗ Неверный формат времени! Убедитесь, что используете формат, например, 8:30 или 09:40.");
                        return REPEAT;
                    }
                });

        var organizerClubMenu = Menu.builder()
                .items(new Item("Создать встречу", context -> {
                    send(context, "Создаю встречу");
                    return NEXT;
                }))
                .build();

        var createMeeting = Command.create("/create_meeting")
                .access(Role.EMPLOYEES)
                .help("Позволяет создавать встречи для клубов.")
                .input(inputSelectClub)
                .input(Reply.builder()
                        .preview(context -> {
                            var club = context.getValue("club", Club.class);
                            send(context, "Вы выбрали клуб: " + club.getName(), organizerClubMenu.showMenu(context));
                        })
                        .action(organizerClubMenu::onClick))
                .input(inputMeetName, inputMeetDate, inputMeetTime)
                .input(Reply.builder().preview(context -> {
                            var meet = Meet.builder()
                                    .name(context.getValue("meetName"))
                                    .time(LocalTime.parse(context.getValue("meetTime")))
                                    .date(LocalDate.parse(context.getValue("meetDate")))
                                    .status(MeetStatus.HIDDEN)
                                    .build();
                            var club = context.getValue("club", Club.class);
                            meet.setClub(club);
                            meet = meetService.save(meet);
                            send(context, "Встреча создана!");
                            var finalMeet = meet;
                            var dialogMenu = Menu.builder().items(
                                    new Item("Да", _ -> {
                                        meetService.publicMeetByIdWithNotification(finalMeet.getId());
                                        send(context, "Встреча опубликована!" +
                                                "\n\uD83D\uDCD8 Используй /control_meeting для управления встречей.");
                                        return TERMINATE;
                                    }),
                                    new Item("Нет", _ -> {
                                        send(context, "Встреча не опубликована." +
                                                "\n\uD83D\uDCD8 Используй /control_meeting для управления встречей.");
                                        return TERMINATE;
                                    })
                            ).build();
                            send(context, "Хотите опубликовать встречу?", dialogMenu.showMenu(context));
                            return NEXT;
                        })
                        .action(context -> context.getMenu().onClick(context))).build();

        var controlMeeting = Command.create("/control_meeting")
                .help("Позволяет управлять встречами ")
                .access(Role.EMPLOYEES)
                .input(inputSelectClub)
                .input(Reply.builder().preview(context -> {
                    var club = context.getValue("club", Club.class);
                    var meets = meetService.getMeetsByClubIdAndStatus(club.getId(), Set.of(MeetStatus.PUBLISHED, MeetStatus.HIDDEN));
                    if (meets.isEmpty()) {
                        send(context, "Нет доступных встреч! \n\uD83D\uDCD8 Используй /create_meeting чтобы создать встречу.");
                        return TERMINATE;
                    }
                    var items = meets.stream().map(meet -> new Item(String.valueOf(meet.getId()), formatForItemWithStatus(meet))).toList();
                    var menu = Menu.builder()
                            .items(items)
                            .build();
                    send(context, "Встречи клуба: " + club.getName(), menu.showMenu(context));
                    return NEXT;
                }))
                .input(Reply.builder().preview(context -> {
                            if (Menu.isInvalidInput(context)) return INVALID;

                            var meetId = Long.parseLong(context.getText());
                            var meetOpt = meetService.getMeetById(meetId);

                            if (meetOpt.isEmpty()) {
                                send(context, "Данные о встрече устарели! Попробуйте снова!");
                                return TERMINATE;
                            }
                            var meet = meetOpt.get();

                            var items = new ArrayList<Item>();

                            if (meet.getStatus() == MeetStatus.HIDDEN) {
                                items.add(new Item("Опубликовать", _ -> {
                                    meetService.publicMeetByIdWithNotification(meet.getId());
                                    send(context, "Встреча опубликована! \n\uD83D\uDCD8 Используй /control_meeting чтобы управлять встречами");
                                    return TERMINATE;
                                }));
                                items.add(new Item("Удалить", _ -> {
                                    var isDelete = meetService.deleteMeetById(meet.getId());
                                    if (isDelete)
                                        send(context, "Встреча удалена! \n\uD83D\uDCD8 Используй /control_meeting чтобы управлять встречами");
                                    else
                                        send(context, "Не удалось удалить встречу.");
                                    return TERMINATE;
                                }));
                            }
                            if (meet.getStatus() == MeetStatus.PUBLISHED) {
                                items.add(new Item("Отметить проведённой", _ -> {
                                    meetService.updateMeetStatus(meet.getId(), MeetStatus.COMPLETED)
                                            .ifPresentOrElse(_ -> send(context, "Отметил встречу как проведённую! " +
                                                            "\n\uD83D\uDCD8 Используй /control_meeting чтобы управлять встречами"),
                                                    () -> send(context, "Не удалось встречи не существует!"));
                                    return NEXT;
                                }));
                                items.add(new Item("Отменить", _ -> {
                                    meetService.cancelMeetByIdWithNotification(meet.getId())
                                            .ifPresentOrElse(_ -> send(context, "Отменил встречу! " +
                                                            "\n\uD83D\uDCD8 Используй /control_meeting чтобы управлять встречами"),
                                                    () -> send(context, "Не удалось встречи не существует!"));
                                    meetService.deleteMeetById(meet.getId());
                                    return TERMINATE;
                                }));
                            }
                            items.add(new Item("Участники", _ -> {
                                meetService.getMeetWithRegisteredUsersById(meet.getId()).ifPresentOrElse(meet1 -> {
                                    var users = meet1.getRegisteredUsers();
                                    if (users.isEmpty()) {
                                        send(context, "Зарегистрировавшиеся участники не найдены!");
                                        send(context, "\n\uD83D\uDCD8 Используй /control_meeting чтобы управлять встречами");
                                        return;
                                    }
                                    var outStrBuilder = new StringBuilder("Список участников:\n");
                                    users.forEach(u -> outStrBuilder.append(u.getFirstName()).append(" ").append(u.getLastName()).append("\n"));
                                    outStrBuilder.append("Всего: ").append(users.size());

                                    send(context, outStrBuilder.toString());
                                    send(context, "\n\uD83D\uDCD8 Используй /control_meeting чтобы управлять встречами");

                                }, () -> send(context, "Не удалось встречи не существует!"));
                                return TERMINATE;
                            }));
                            var menu = Menu.builder()
                                    .items(items)
                                    .build();
                            send(context, "Выберите действие встречи:\n\n" + meet.getName(), menu.showMenu(context));
                            return NEXT;
                        })
                        .action(context -> context.getMenu().onClick(context)))
                .build();

        /* MODERATOR - Это тот кто создаёт встречи. Закреплён за клубом.
         * /control_meeting - отображает список клубов
         *      К: Создать встречу. -> Тема встречи -> Дата -> Время. ?> Опубликовать
         *      В: Встречи -> Показывает меню со встречами.
         *          В*: Опубликовать.
         *          В: Получить список с участниками.
         *          В*: Отметить проведённой. ?> Запустить опрос.
         *          В*: Отменить.
         *          В*: Удалить.
         */

        var createClub = Command.create("/create_club")
                .access(Role.MODERATOR)
                .input(inputClubName, inputClubDescription)
                .help("Позволяет создать новый клуб.")
                .input(Reply.builder().preview(context -> {
                            var clubName = context.getValue("clubName");
                            var clubDescription = context.getValue("clubDescription");
                            send(context, """
                                    Название: %s
                                    Описание:
                                    %s
                                    """.formatted(clubName, clubDescription));
                            var club = Club.builder()
                                    .name(clubName)
                                    .description(clubDescription)
                                    .build();
                            clubService.save(club);
                            send(context, "Клуб создан!\n\uD83D\uDCD8 Используй /control_clubs чтобы управлять клубами.");
                        })
                ).build();

        var controlClubs = Command.create("/control_clubs")
                .help("Позволяет управлять клубами")
                .access(Role.MODERATOR)
                .input(inputSelectClub)
                .input(Reply.builder().preview(context -> {
                            var club = context.getValue("club", Club.class);
                            var items = new ArrayList<Item>();
                            items.add(new Item("Удалить", _ -> {
                                var isDelete = clubService.deleteById(club.getId());
                                if (isDelete) {
                                    send(context, """
                                            Клуб удалён!\
                                            
                                            
                                            \uD83D\uDCD8 Используй /control_clubs чтобы управлять клубами.""");
                                } else {
                                    send(context, "Не удалось удалить клуб.");
                                }
                                return TERMINATE;
                            }));
                            items.add(new Item("Изменить", _ -> {
                                send(context, "Отметил встречу как проведённую! " +
                                        "\n\uD83D\uDCD8 Используй /control_meeting чтобы управлять встречами");
                                return NEXT;
                            }));
                            var menu = Menu.builder()
                                    .items(items)
                                    .build();
                            send(context, "Выберите действие клуба:\n\n" + club.getName(), menu.showMenu(context));
                            return NEXT;
                        })
                        .action(context -> context.getMenu().onClick(context)))
                .input(inputClubName, inputClubDescription)
                .input(Reply.builder().preview(context -> {
                    var club = context.getValue("club", Club.class);
                    var clubName = context.getValue("clubName");
                    var clubDescription = context.getValue("clubDescription");
                    club.setName(clubName);
                    club.setDescription(clubDescription);
                    clubService.update(club.getId(), club);
                    send(context, "Клуб изменён!");
                })).build();

        var inputEmployeesNickname = Reply.builder()
                .preview((Consumer<MessageContext>) context ->
                        send(context, "Введите имя пользователя:"))
                .action(context -> {
                    var msg = context.getText();
                    if (msg.isBlank() || msg.length() < 5) {
                        send(context, "❗ Введите тему встречи от 5-ти символов!");
                        return REPEAT;
                    }
                    if (msg.length() > 250) {
                        send(context, "❗ Имя пользователя не может длиннее 250 символов!");
                        return REPEAT;
                    }
                    context.put("userNickname", msg);
                    return NEXT;
                });

        var create_employ = Command.create("/create_employ")
                .access(Role.MODERATOR)
                .help("Позволяет создавать сотрудников")
                .input(inputEmployeesNickname)
                .input(Reply.builder().preview(context -> {
                    var userNickname = context.getValue("userNickname");
                    var userOpt = userService.getUserByNickname(userNickname);
                    var moderator = context.getUser();
                    if (userNickname.equals(moderator.getUsername())) {
                        send(context, "Вы уже являетесь сотрудником.");
                        return TERMINATE;
                    }
                    userOpt.ifPresentOrElse(user -> {
                                userService.grantRole(user.getTelegramId(), Role.ORGANIZER);
                                send(context, "Пользователь " + userNickname + " повышен до сотрудника!");
                                contextFlow.disposeContext(user.getChatId());
                            },
                            () -> send(context, "Пользователь c никнеймом: " + userNickname + " не найден!"));
                    return TERMINATE;
                })).build();

        var control_employees = Command.create("/control_employees")
                .access(Role.MODERATOR)
                .help("Позволяет управлять сотрудниками")
                .input(Reply.builder().preview(context -> {
                    var users = userService.getUsersByRole(Role.ORGANIZER);

                    var items = users.stream()
                            .map(user -> new Item(String.valueOf(user.getTelegramId()), user.getFirstName() + " "
                                    + user.getLastName() + " " + user.getMiddleName()))
                            .toList();
                    if (items.isEmpty()) {
                        send(context, "У вас пока нет сотрудников!");
                        return TERMINATE;
                    }

                    var menu = Menu.builder()
                            .items(items)
                            .build();
                    context.put("users", users);
                    send(context, "Выберите сотрудника:", menu.showMenu(context));
                    return NEXT;
                }))
                .input(Reply.builder().preview(context -> {
                            if (Menu.isInvalidInput(context)) {
                                return INVALID;
                            }
                            var users = context.getValues("users", User.class);
                            var userId = Long.parseLong(context.getText());
                            var userOpt = users.stream()
                                    .filter(user -> user.getTelegramId().equals(userId))
                                    .findFirst();
                            if (userOpt.isEmpty()) return TERMINATE;
                            var user = userOpt.get();
                            var items = new ArrayList<Item>();
                            items.add(new Item("Профиль", _ -> {
                                send(context, """
                                        Данные сотрудника:
                                        
                                        📛 ФИО: %s %s %s
                                        📱 Телефон: %s
                                        📧 Email: %s"""
                                        .formatted(user.getFirstName(), user.getLastName(), user.getMiddleName(),
                                                user.getPhone(), user.getEmail()));
                                return TERMINATE;
                            }));
                            items.add(new Item("Понизить", _ -> {
                                userService.grantRole(user.getTelegramId(), Role.MEMBER);
                                contextFlow.disposeContext(user.getChatId());
                                send(context, """
                                        Сотрудник понижен!\
                                        
                                        
                                        \uD83D\uDCD8 Используй /control_employees чтобы управлять сотрудниками.""");
                                return TERMINATE;
                            }));
                            var menu = Menu.builder()
                                    .items(items)
                                    .build();
                            send(context, "Выберите действие для сотрудника:\n\n" + user.getFirstName() + " " +
                                    user.getMiddleName() + " " + user.getLastName(), menu.showMenu(context));
                            return NEXT;
                        })
                        .action(context -> context.getMenu().onClick(context)))
                .build();

        addCommands(commands, startCommand, registerCommand);
        addCommands(commands, clubsCommand, meetsCommand);
        addCommands(commands, createMeeting, controlMeeting);
        addCommands(commands, createClub, controlClubs);
        addCommands(commands, create_employ, control_employees);
        addCommands(commands, feedback);
        addCommands(commands, profile, editProfile, deleteUserCommand);
        addCommands(commands, help);

        return commands;
    }
}
