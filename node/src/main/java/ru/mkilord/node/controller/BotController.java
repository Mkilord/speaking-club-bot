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
import ru.mkilord.node.service.UserService;
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
    UserService userService;

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
     *
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
                    var msg = context.getText();
                    if (!TextUtils.isRange(msg, 0, 10)) {
                        send(context, "❗ Введите число от 0 до 10!");
                        return REPEAT;
                    }
                    context.put("rate", msg);
                    return NEXT;
                });
        var inputClubName = Reply.builder().preview(context -> send(context, "Введите название клуба."))
                .action(context -> {
                    var msg = context.getText();
                    if (msg.isBlank() || msg.length() < 2) {
                        send(context, "❗ Введите имя клуба длиннее 2х символов!");
                        return REPEAT;
                    }
                    context.put("clubName", msg);
                    return NEXT;
                });
        var inputClubDescription = Reply.builder().preview(context -> send(context, "Введите описание клуба."))
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
                    var clubs = new ArrayList<>(List.of("Немецкий", "Русский", "Английский"));
                    // Запросить информацию о клубах
                    var items = clubs.stream().map(string -> new Item(string, string)).toList();
                    var menu = Menu.builder()
                            .items(items)
                            .build();
                    context.setMenu(menu);
                    send(context, "Выберите клуб", menu.showMenu());
                }).action(context -> {
                    if (Menu.invalidItem(context.getMenu(), context.getText())) return INVALID;
                    var club = context.getText();
                    context.put("club", club);
//                    send(context, "Вы выбрали клуб: " + club);
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
                    //Запрашиваем доступные встречи.
                    var meets = new ArrayList<>(List.of("20.11.2024 18:00", "24.11.2024 12:20"));
                    var items = meets.stream().map(string -> new Item(string, string)).toList();
                    var menu = Menu.builder()
                            .items(items)
                            .build();
                    context.setMenu(menu);
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
                    var club = context.getValue("club");
                    send(context, club + " клуб:", clubOptionsMenu.showMenu());
                }).action(clubOptionsMenu::onClick))
                .input(context -> {
                    if (Menu.invalidItem(context.getMenu(), context.getText())) return INVALID;
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

        // TODO 2024-12-24 13:53: убрать при релизе
        var debugMenu = Menu.builder().items(
                new Item("Получить Модератора", context -> {
                    userService.grantRole(context.getUser().getTelegramId(), Role.MODERATOR);
                    return TERMINATE;
                }),
                new Item("Получить Участника", context -> {
                    userService.grantRole(context.getUser().getTelegramId(), Role.MEMBER);
                    return TERMINATE;
                }),
                new Item("Получить Организатор", context -> {
                    userService.grantRole(context.getUser().getTelegramId(), Role.ORGANIZER);
                    return TERMINATE;
                })
        ).build();

        // TODO 2024-12-24 13:53: убрать при релизе
        var debug = Command.create("/debug").access(Role.ALL).action(context -> {
            send(context, "Выберите функцию:", debugMenu.showMenu());
        }).input(debugMenu::onClick).build();

        var createClub = Command.create("/create_club").access(Role.MEMBER)
                .input(inputClubName, inputClubDescription)
                .input(Reply.builder().preview(context -> {
                            var clubName = context.getValue("clubName");
                            var clubDescription = context.getValue("clubDescription");
                            send(context, """
                                    Клуб:%s
                                    О клубе:
                                    %s
                                    """.formatted(clubName, clubDescription));
                            //Создать клуб
                            send(context, "Клуб создан!");
                        })
                ).build();


        /*     *ORGANIZER - Это тот кто создаёт встречи. Закреплён за клубом.
         * /control_clubs - отображает список клубов которые он контролирует
         *      К: Создать встречу. -> Тема встречи -> Дата -> Время. ?> Запустить рассылку
         * /control_meeting - отображает все предстоящие встречи.
         *      В: Запустить рассылку. (Уведомить подписчиков клуба)
         *      В: Получить список с участниками.
         *      В: Изменить.
         *      В: Отметить проведённой. -> Запустить опрос.
         *      В: Отменить.
         *      В: Удалить.*/
//        var clubsControlMenu = Menu.builder().items(new Item(""))
        var organizerClubMenu = Menu.builder()
                .items(new Item("Создать встречу", context1 -> {
                    send(context1, "Создаю встречу");
                    return NEXT;
                }))
                .build();

        var controlClubs = Command.create("/control_clubs").access(Role.ALL)
                .help("Позволяет создавать встречи для клубов.")
                .input(inputSelectClub)
                .input(Reply.builder()
                        .preview(context -> {
                            var club = context.getValue("club");
                            send(context, "Вы выбрали клуб: " + club, organizerClubMenu.showMenu());
                            context.setMenu(organizerClubMenu);
                        })
                        .action(organizerClubMenu::onClick))
                .build();

        addCommands(commands, controlClubs);
        addCommands(commands, createClub, debug, clubsCommand, feedback, profile, editProfile);
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
