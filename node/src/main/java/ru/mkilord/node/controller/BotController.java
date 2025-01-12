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
import ru.mkilord.node.model.Club;
import ru.mkilord.node.model.Meet;
import ru.mkilord.node.model.enums.MeetStatus;
import ru.mkilord.node.model.enums.Role;
import ru.mkilord.node.service.ProducerService;
import ru.mkilord.node.service.impl.ClubService;
import ru.mkilord.node.service.impl.MeetService;
import ru.mkilord.node.service.impl.UserService;
import ru.mkilord.node.util.TextUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;

import static lombok.AccessLevel.PRIVATE;
import static ru.mkilord.node.command.Step.*;


@FieldDefaults(level = PRIVATE, makeFinal = true)
@Slf4j
@Component
@AllArgsConstructor
public class BotController implements CommandCatalog {

    CommandHandler commandHandler;
    ProducerService producerService;
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
     *  /my_meets - текущие встречи.
     *  /clubs - просмотр клубов, подписка на уведомления и на встречу
     *
     *
     *ORGANIZER - Это тот кто создаёт встречи. Закреплён за клубом.
     * /control_clubs - отображает список клубов которые он контролирует
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
                .preview((Consumer<MessageContext>) context -> send(context, "Пожалуйста, введи своё ФИО через пробел. ✍️"))
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
                .preview((Consumer<MessageContext>) context -> send(context, "Пожалуйста, введи свой email. 📧"))
                .action(context -> {
                    context.put("email", context.getText());
                    return NEXT;
                });
        var phoneInput = Reply.builder()
                .preview((Consumer<MessageContext>) context -> send(context, "Пожалуйста, введи свой телефон. 📱"))
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
                .preview((Consumer<MessageContext>) context -> send(context, "Введите число от 0 до 10!"))
                .action(context -> {
                    var msg = context.getText();
                    if (!TextUtils.isRange(msg, 0, 10)) {
                        send(context, "❗ Введите число от 0 до 10!");
                        return REPEAT;
                    }
                    context.put("rate", msg);
                    return NEXT;
                });
        var inputClubName = Reply.builder().preview((Consumer<MessageContext>) context -> send(context, "Введите название клуба."))
                .action(context -> {
                    var msg = context.getText();
                    if (msg.isBlank() || msg.length() < 2) {
                        send(context, "❗ Введите имя клуба длиннее 2х символов!");
                        return REPEAT;
                    }
                    context.put("clubName", msg);
                    return NEXT;
                });
        var inputClubDescription = Reply.builder().preview((Consumer<MessageContext>) context -> send(context, "Введите описание клуба."))
                .action(context -> {
                    var msg = context.getText();
                    if (msg.isBlank() || msg.length() < 20) {
                        send(context, "❗ Введите Описание клуба 20ти символов!");
                        return REPEAT;
                    }
                    context.put("clubDescription", msg);
                    return NEXT;
                });
        var inputSelectClub = Reply.builder()
                .preview(context -> {
                    var clubs = new ArrayList<>(clubService.getAll());
                    var items = clubs.stream().map(club -> new Item(String.valueOf(club.getId()), club.getName())).toList();
                    if (items.isEmpty()) {
                        send(context, "Здесь пока не доступных клубов!");
                        return TERMINATE;
                    }
                    var menu = Menu.builder()
                            .items(items)
                            .build();
                    context.setMenu(menu);
                    send(context, "Выберите клуб", menu.showMenu());
                    return NEXT;
                }).action(context -> {
                    if (Menu.invalidItem(context.getMenu(), context.getText())) return INVALID;
                    var clubId = context.getText();
                    context.put("club", clubId);
                    context.put("clubName", context.getMenu().getItemNameByKey(clubId));
                    return NEXT;
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
                        .input(usernameInput, phoneInput, emailInput)
                        .post(context -> {
                            var user = context.getUser();
                            user.setFirstName(context.getValue("firstName"));
                            user.setLastName(context.getValue("lastName"));
                            user.setMiddleName(context.getValue("middleName"));
                            user.setEmail(context.getValue("email"));
                            user.setPhone(context.getValue("phone"));
                            user.setRole(Role.MEMBER);
                            context.setUser(user);
                            userService.update(user);
                            send(context, """
                                    ✅ Данные записаны!
                                    
                                    Ваши данные:
                                    📛 ФИО: %s %s %s
                                    📱 Телефон: %s
                                    📧 Email: %s""".formatted(user.getFirstName(), user.getLastName(), user.getMiddleName(),
                                    user.getPhone(), user.getEmail()));
                            send(context, "ℹ️ Чтобы узнать команды бота, используй /help\nили кнопку ниже! 🤖", helpMenu.showMenu());
                        }).build());


        //Member Commands
        //Проблема в том что когда это динамическое меню мы никак не можем понять
        //Кликнул ли он по меню или ввёл команду.
        //Нужно как то отличить клик.
        var clubOptionsMenu = Menu.builder()
                .items(new Item("Встречи", context -> {
                    var club = context.getValue("club");
                    var meets = meetService.getPublishedMeetsByClubId(Long.parseLong(club));
                    if (meets.isEmpty()) {
                        send(context, "Нет доступных встреч!");
                        return TERMINATE;
                    }
                    var items = meets.stream()
                            .map(meet -> new Item(String.valueOf(meet.getId()), meet.getInfo())).toList();
                    var menu = Menu.builder()
                            .items(items)
                            .build();
                    context.setMenu(menu);
                    send(context, "Доступные встречи: ", menu.showMenu());
                    return NEXT;
                }), new Item("Подписаться", context -> {
                    var clubId = context.getValue("club");
                    clubService.addSubscriber(Long.valueOf(clubId), context.getUser());
                    send(context, "Подписка оформлена!");
                    return TERMINATE;
                }), new Item("О клубе", context -> {
                    var clubId = context.getValue("club");
                    var clubOpt = clubService.getClubById(Long.parseLong(clubId));
                    clubOpt.ifPresentOrElse(club -> send(context, "Информация о клубе: \n" + club.getDescription())
                            , () -> send(context, "Информация не найдена!"));
                    return TERMINATE;
                }))
                .build();


        var clubControlOptionsMenu = Menu.builder()
                .items(new Item("Изменить", context -> {
                            return null;
                        }),
                        new Item("Удалить", context -> {
                            return null;
                        })).build();


        var clubsCommand = Command.create("/clubs")
                .access(Role.MEMBER)
                .help("Записаться на встречу, получить информацию о клубе.")
                .input(inputSelectClub)
                .input(Reply.builder().preview(context -> {
                    var clubName = context.getValue("clubName");
                    send(context, clubName + " клуб:", clubOptionsMenu.showMenu());
                }).action(clubOptionsMenu::onClick))
                .input(context -> {
                    if (Menu.invalidItem(context.getMenu(), context.getText())) return INVALID;
                    var meetId = context.getText();
                    meetService.addUserToMeet(Long.valueOf(meetId), context.getUser());
                    send(context, "Отметил что ты придёшь на встречу. ");
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
                        return NEXT;
                    }
                    send(context, "Доступные опросы не найдены!");
                    return TERMINATE;
                })
                .input(inputRate)
                .post(context -> {
                    var rate = context.getValue("rate");
                    send(context, "Оценка записана: " + rate);
                }).build();

        var profileMenu = Menu.builder()
                .items(new Item("/edit_profile", "Изменить"))
                .build();

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
                            user.getMiddleName(), user.getPhone(), user.getEmail()), profileMenu.showMenu());
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
                    context.setUser(user);
                    userService.update(user);
                    send(context, "✅ Данные записаны!");
                }).build();

        /*
         *  MODERATOR
         * */
        var createClub = Command.create("/create_club").access(Role.MEMBER)
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
                            send(context, "Клуб создан!");
                        })
                ).build();


        /*     *ORGANIZER - Это тот кто создаёт встречи. Закреплён за клубом.
         * /control_clubs - отображает список клубов которые он контролирует
         *      К: Создать встречу. -> Тема встречи -> Дата -> Время. ?> Опубликовать
         *      В: Встречи -> Показывает меню со встречами.
         *          В: Опубликовать.
         *          В: Получить список с участниками.
         *          В: Отметить проведённой. -> Запустить опрос.
         *          В: Отменить.
         *          В: Удалить.*/
        var meetMenu = Menu.builder().items(
                new Item("Опубликовать", context -> {
                    var meetId = context.getValue("meetId");
                    var meetOpt = meetService.publicMeetByIdWithNotification(Long.parseLong(meetId));
                    meetOpt.ifPresentOrElse(_ -> send(context, "Встреча опубликована!"),
                            () -> send(context, "Не удалось встречи не существует!"));
                    return TERMINATE;
                }),
                new Item("Участники", context -> {
                    var meetId = context.getValue("meetId");
                    var meetOpt = meetService.getMeetWithRegisteredUsersById(Long.parseLong(meetId));
                    meetOpt.ifPresentOrElse(meet -> {
                        var users = meet.getRegisteredUsers();
                        var outStrBuilder = new StringBuilder("Список участников:\n");
                        users.forEach(u -> outStrBuilder.append(u.getFirstName()).append(" ").append(u.getLastName()).append("\n"));
                        outStrBuilder.append("Всего: ").append(users.size());
                        send(context, outStrBuilder.toString());
                    }, () -> send(context, "Не удалось встречи не существует!"));
                    return TERMINATE;
                }),
                new Item("Отметить проведённой", context -> {
                    var meetId = context.getValue("meetId");
                    var meetOpt = meetService.updateMeetStatus(Long.parseLong(meetId), MeetStatus.COMPLETED);
                    meetOpt.ifPresentOrElse(_ -> send(context, "Отметил встречу как проведённую!")
                            , () -> send(context, "Не удалось встречи не существует!"));
                    return NEXT;
                }),
                new Item("Отменить", context -> {
                    var meetId = context.getValue("meetId");
                    var meetOpt = meetService.cancelMeetByIdWithNotification(Long.parseLong(meetId));
                    meetOpt.ifPresentOrElse(_ -> send(context, "Отметил встречу!")
                            , () -> send(context, "Не удалось встречи не существует!"));
                    return TERMINATE;
                }),
                new Item("Удалить", context -> {
                    var meetId = context.getValue("meetId");
                    var isDelete = meetService.deleteMeetById(Long.parseLong(meetId));
                    if (isDelete) {
                        send(context, "Встреча удалена!");
                    } else {
                        send(context, "Не удалось удалить встречу. \n Чтобы удалить встречу нужно её отменить!");
                    }
                    return TERMINATE;
                })
        ).build();

        var controlMeeting = Command.create("/control_meeting")
                .help("Позволяет управлять встречами ")
                .access(Role.MEMBER)
                .input(inputSelectClub)
                .input(Reply.builder().preview(context -> {
                    var clubId = context.getValue("club");
                    var clubName = context.getValue("clubName");
                    var meets = meetService.getMeetsByClubIdAndStatus(Long.parseLong(clubId), MeetStatus.ACTUAL_MEETS);
                    if (meets.isEmpty()) {
                        send(context, "Нет доступных встреч!");
                        return TERMINATE;
                    }
                    var items = meets.stream().map(meet -> new Item(String.valueOf(meet.getId()), meet.getInfo())).toList();
                    var menu = Menu.builder()
                            .items(items)
                            .build();
                    send(context, "Встречи клуба: " + clubName, menu.showMenu());
                    context.setMenu(menu);
                    return NEXT;
                }).action(context -> {
                    if (Menu.invalidItem(context.getMenu(), context.getText())) return INVALID;
                    context.put("meetId", context.getText());
                    context.put("meetName", context.getMenu().getItemNameByKey(context.getText()));
                    return NEXT;
                }))
                .input(Reply.builder().preview(context -> {
                    var meetName = context.getValue("meetName");
                    var meetId = context.getValue("meetId");
                    var meetOpt = meetService.getMeetById(Long.parseLong(meetId));
                    if (meetOpt.isEmpty()) {
                        send(context, "Данные о встрече устарели! Попробуйте снова!");
                        return TERMINATE;
                    }
                    var meet = meetOpt.get();
                    var items = new ArrayList<Item>();
                    if (meet.getStatus() == MeetStatus.HIDDEN) {
                        items.add(new Item("Опубликовать", _ -> {
                            meetService.publicMeetByIdWithNotification(Long.parseLong(meetId));
                            send(context, "Встреча опубликована!");
                            return TERMINATE;
                        }));
                        items.add(new Item("Удалить", _ -> {
                            var isDelete = meetService.deleteMeetById(Long.parseLong(meetId));
                            if (isDelete) {
                                send(context, "Встреча удалена!");
                            } else {
                                send(context, "Не удалось удалить встречу.");
                            }
                            return TERMINATE;
                        }));
                    }
                    if (meet.getStatus() == MeetStatus.PUBLISHED) {
                        items.add(new Item("Отметить проведённой", _ -> {
                            meetService.updateMeetStatus(Long.parseLong(meetId), MeetStatus.COMPLETED)
                                    .ifPresentOrElse(_ -> send(context, "Отметил встречу как проведённую!")
                                            , () -> send(context, "Не удалось встречи не существует!"));
                            return NEXT;
                        }));
                        items.add(new Item("Отменить", _ -> {
                            meetService.cancelMeetByIdWithNotification(Long.parseLong(meetId))
                                    .ifPresentOrElse(_ -> send(context, "Отметил встречу!")
                                            , () -> send(context, "Не удалось встречи не существует!"));
                            return TERMINATE;
                        }));
                    }
                    var menu = Menu.builder()
                            .items(items)
                            .build();
                    send(context, "Выберите действие встречи:\n" + meetName, menu.showMenu());
                    context.setMenu(menu);
                    return NEXT;
                }).action(context -> context.getMenu().onClick(context)))
                .build();


        var inputMeetName = Reply.builder().preview((Consumer<MessageContext>) context -> send(context, "Введите тему встречи:"))
                .action(context -> {
                    var msg = context.getText();
                    if (msg.isBlank() || msg.length() < 20) {
                        send(context, "❗ Введите тему встречи от 20-ти символов!");
                        return REPEAT;
                    }
                    context.put("meetName", msg);
                    return NEXT;
                });
        var inputMeetDate = Reply.builder().preview((Consumer<MessageContext>) context -> send(context, "Введите дату встречи в формате YYYY-MM-DD:"))
                .action(context -> {
                    var msg = context.getText();
                    try {
                        var date = LocalDate.parse(msg, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        var now = LocalDate.now();
                        if (date.isBefore(now) || date.isAfter(now.plusMonths(1))) {
                            send(context, "❗ Введите дату в пределах от сегодняшнего дня до 1-го месяца вперёд.!");
                            return REPEAT;
                        }
                        context.put("meetDate", date.toString());
                        return NEXT;

                    } catch (DateTimeParseException e) {
                        send(context, "❗ Неверный формат даты! Убедитесь, что используете формат YYYY-MM-DD.");
                        return REPEAT;
                    }
                });
        var inputMeetTime = Reply.builder().preview((Consumer<MessageContext>) context -> send(context, "Введите время встречи в формате HH:mm (24-часовой формат):"))
                .action(context -> {
                    var msg = context.getText();
                    try {
                        // Проверка формата времени
                        var time = LocalTime.parse(msg, DateTimeFormatter.ofPattern("HH:mm"));

                        // Проверка диапазона времени (например, от 08:00 до 22:00)
                        var start = LocalTime.of(8, 0); // 08:00
                        var end = LocalTime.of(22, 0);  // 22:00
                        if (time.isBefore(start) || time.isAfter(end)) {
                            send(context, "❗ Введите время в диапазоне от 08:00 до 22:00!");
                            return REPEAT;
                        }

                        // Сохранение времени в контекст как строку
                        context.put("meetTime", time.toString());
                        return NEXT;

                    } catch (DateTimeParseException e) {
                        send(context, "❗ Неверный формат времени! Убедитесь, что используете формат HH:mm.");
                        return REPEAT;
                    }
                });

        var organizerClubMenu = Menu.builder()
                .items(new Item("Создать встречу", context1 -> {
                    send(context1, "Создаю встречу");
                    return NEXT;
                }))
                .build();

        var createMeeting = Command.create("/create_meeting").access(Role.ALL)
                .help("Позволяет создавать встречи для клубов.")
                .input(inputSelectClub)
                .input(Reply.builder()
                        .preview(context -> {
                            var clubName = context.getValue("clubName");
                            send(context, "Вы выбрали клуб: " + clubName, organizerClubMenu.showMenu());
                            context.setMenu(organizerClubMenu);
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
                    var clubOpt = clubService.getClubById(Long.parseLong(context.getValue("club")));
                    if (clubOpt.isPresent()) {
                        meet.setClub(clubOpt.get());
                        meet = meetService.save(meet);
                        context.put("meetId", String.valueOf(meet.getId()));
                    } else {
                        send(context, "Упс, этот клуб уже не доступен!");
                        return TERMINATE;
                    }
                    send(context, "Встреча создана!");
                    var finalMeet = meet;
                    var dialogMenu = Menu.builder().items(
                            new Item("Да", _ -> {
                                meetService.publicMeetByIdWithNotification(finalMeet.getId());
                                send(context, "Встреча опубликована!");
                                return TERMINATE;
                            }),
                            new Item("Нет", _ -> TERMINATE)
                    ).build();
                    context.setMenu(dialogMenu);
                    send(context, "Хотите опубликовать встречу?", dialogMenu.showMenu());
                    return NEXT;
                }).action(context -> {
                    context.getMenu().onClick(context);
                    return TERMINATE;
                })).build();
        addCommands(commands, createMeeting, controlMeeting);
        addCommands(commands, createClub, clubsCommand, feedback, profile, editProfile);
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

        addCommands(commands, help);
        return commands;
    }
}
