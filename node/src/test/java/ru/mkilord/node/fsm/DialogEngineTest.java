package ru.mkilord.node.fsm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.mkilord.node.dialog.DialogState;
import ru.mkilord.node.model.Role;
import ru.mkilord.node.model.User;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DialogEngineTest {

    private static final long CHAT = 42L;

    private final List<String> saved = new ArrayList<>();
    private DialogEngine engine;
    private DialogState state;
    private User user;

    @BeforeEach
    void setUp() {
        CommandCatalog catalog = () -> List.of(
                Command.create("/name")
                        .access(Role.ALL)
                        .help("ask name and color")
                        .input(
                                Input.text("Name?", context -> {
                                    if (context.getText().length() < 2) {
                                        context.send("Too short");
                                        return Step.REPEAT;
                                    }
                                    context.put("name", context.getText());
                                    return Step.NEXT;
                                }),
                                Input.menu(context -> Menu.builder("Color?")
                                        .item(Item.of("red", "Red"))
                                        .item(Item.of("blue", "Blue"))
                                        .onSelect((ctx, key) -> {
                                            ctx.put("color", key);
                                            return Step.NEXT;
                                        })
                                        .build()),
                                Input.message(context -> context.send("Almost done")))
                        .post(context -> saved.add(context.get("name") + ":" + context.get("color")))
                        .build(),
                Command.create("/secret")
                        .access(Role.MODERATOR)
                        .help("moderators only")
                        .action(context -> context.send("secret"))
                        .build(),
                Command.create("/empty")
                        .access(Role.ALL)
                        .input(Input.menu(context -> {
                            context.send("Nothing here");
                            return null;
                        }))
                        .build());
        engine = new DialogEngine(List.of(catalog));
        state = new DialogState(CHAT);
        user = new User(CHAT, CHAT, "tester");
        user.setRole(Role.MEMBER);
    }

    @Test
    void walksThroughTextMenuAndMessageSteps() {
        assertThat(texts(send(TestUpdates.text(CHAT, "/name")))).containsExactly("Name?");

        assertThat(texts(send(TestUpdates.text(CHAT, "A")))).containsExactly("Too short");
        assertThat(state.getReplyId()).isEqualTo("/name0");

        var menuAnswer = send(TestUpdates.text(CHAT, "Alice"));
        assertThat(texts(menuAnswer)).containsExactly("Color?");
        var buttonData = callbackData(menuAnswer.getFirst(), 1);
        assertThat(buttonData).isEqualTo("m:/name1:blue");

        assertThat(texts(send(TestUpdates.callback(CHAT, buttonData)))).containsExactly("Almost done");
        assertThat(saved).containsExactly("Alice:blue");
        assertThat(state.getReplyId()).isNull();
        assertThat(state.getValues()).isEmpty();
    }

    @Test
    void menuStepAsksForButtonWhenUserTypesText() {
        send(TestUpdates.text(CHAT, "/name"));
        send(TestUpdates.text(CHAT, "Alice"));

        assertThat(texts(send(TestUpdates.text(CHAT, "blue")))).containsExactly(DialogEngine.CHOOSE_BUTTON);
        assertThat(state.getReplyId()).isEqualTo("/name1");
    }

    @Test
    void rejectsButtonFromAnotherStep() {
        send(TestUpdates.text(CHAT, "/name"));
        send(TestUpdates.text(CHAT, "Alice"));

        assertThat(texts(send(TestUpdates.callback(CHAT, "m:/name0:blue")))).containsExactly(DialogEngine.CHOOSE_BUTTON);
        assertThat(texts(send(TestUpdates.callback(CHAT, "m:/name1:green")))).containsExactly(DialogEngine.CHOOSE_BUTTON);
        assertThat(saved).isEmpty();
    }

    @Test
    void newCommandDropsCurrentDialog() {
        send(TestUpdates.text(CHAT, "/name"));
        send(TestUpdates.text(CHAT, "Alice"));

        var answer = send(TestUpdates.text(CHAT, "/help"));

        assertThat(texts(answer).getFirst()).contains("/name - ask name and color").doesNotContain("/secret");
        assertThat(state.getReplyId()).isNull();
        assertThat(state.getValues()).isEmpty();
    }

    @Test
    void checksRole() {
        assertThat(texts(send(TestUpdates.text(CHAT, "/secret")))).containsExactly(DialogEngine.NO_ACCESS);

        user.setRole(Role.MODERATOR);
        assertThat(texts(send(TestUpdates.text(CHAT, "/secret")))).containsExactly("secret");
    }

    @Test
    void emptyMenuStopsCommand() {
        assertThat(texts(send(TestUpdates.text(CHAT, "/empty")))).containsExactly("Nothing here");
        assertThat(state.getReplyId()).isNull();
    }

    @Test
    void unknownTextAndOldButtonsAreExplained() {
        assertThat(texts(send(TestUpdates.text(CHAT, "hello")))).containsExactly(DialogEngine.UNKNOWN_COMMAND);
        assertThat(texts(send(TestUpdates.callback(CHAT, "m:/name1:blue")))).containsExactly(DialogEngine.OUTDATED_BUTTON);
    }

    @Test
    void stepRemovedInNewReleaseIsDropped() {
        state.setReplyId("/removed3");

        assertThat(texts(send(TestUpdates.text(CHAT, "hi")))).containsExactly(DialogEngine.UNKNOWN_COMMAND);
        assertThat(state.getReplyId()).isNull();
    }

    @Test
    void commandNameMayContainBotUsername() {
        assertThat(DialogEngine.commandName("/help@club_bot")).isEqualTo("/help");
        assertThat(DialogEngine.commandName("hello@mail.ru")).isEqualTo("hello@mail.ru");
    }

    private List<SendMessage> send(Update update) {
        var context = new MessageContext(update, user, state);
        engine.handle(context);
        return context.getOutbox();
    }

    private static List<String> texts(List<SendMessage> messages) {
        return messages.stream().map(SendMessage::getText).toList();
    }

    private static String callbackData(SendMessage message, int button) {
        var keyboard = (InlineKeyboardMarkup) message.getReplyMarkup();
        return keyboard.getKeyboard().get(button).getFirst().getCallbackData();
    }
}
