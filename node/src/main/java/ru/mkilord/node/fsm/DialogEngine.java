package ru.mkilord.node.fsm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.mkilord.node.model.Role;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Finite state machine that drives dialogs.
 * <p>
 * State lives in {@link MessageContext} (backed by the database), the engine itself is stateless
 * and is built once at startup from all {@link CommandCatalog} beans.
 */
@Slf4j
@Component
public class DialogEngine {

    static final String HELP = "/help";
    static final String UNKNOWN_COMMAND = "Не знаю такой команды. Список команд: /help";
    static final String NO_ACCESS = "Эта команда вам недоступна. Список команд: /help";
    static final String OUTDATED_BUTTON = "Эта кнопка уже неактуальна. Список команд: /help";
    static final String CHOOSE_BUTTON = "Выберите вариант с помощью кнопок под сообщением.";
    static final String INVALID_INPUT = "Не понял ответ. Попробуйте ещё раз или начните заново: /help";

    private final Map<String, Command> commands = new LinkedHashMap<>();
    private final Map<String, Reply> replies = new HashMap<>();

    public DialogEngine(List<CommandCatalog> catalogs) {
        catalogs.stream().flatMap(catalog -> catalog.commands().stream()).forEach(this::register);
        register(helpCommand());
        log.info("Registered {} commands: {}", commands.size(), commands.keySet());
    }

    public void handle(MessageContext context) {
        var command = commands.get(commandName(context.getText()));
        if (command != null) {
            if (!command.isAvailableFor(context.getUser().getRole())) {
                context.send(NO_ACCESS);
                return;
            }
            start(command, context);
            return;
        }
        var reply = context.getReplyId().map(replies::get);
        if (reply.isPresent()) {
            answer(reply.get(), context);
            return;
        }
        // No active dialog, or the stored step does not exist anymore (for example after a release).
        context.clear();
        context.send(context.isCallback() ? OUTDATED_BUTTON : UNKNOWN_COMMAND);
    }

    public List<Command> commandsFor(Role role) {
        return commands.values().stream()
                .filter(command -> command.isAvailableFor(role) && command.getHelp() != null)
                .toList();
    }

    private void start(Command command, MessageContext context) {
        context.clear();
        var step = command.getAction() == null ? Step.NEXT : command.getAction().apply(context);
        if (step == Step.NEXT && command.getFirstReply() != null) {
            enter(command.getFirstReply(), context);
        } else {
            context.clear();
        }
    }

    private void enter(Reply reply, MessageContext context) {
        var step = reply.show(context);
        if (step != Step.NEXT) {
            context.clear();
            return;
        }
        if (reply.waitsForInput()) {
            context.setReplyId(reply.getId());
        } else {
            advance(reply, context);
        }
    }

    private void answer(Reply reply, MessageContext context) {
        switch (reply.handle(context)) {
            case NEXT -> advance(reply, context);
            case REPEAT -> {
                // stay on this reply
            }
            case TERMINATE -> context.clear();
            case INVALID -> context.send(reply.isMenu() ? CHOOSE_BUTTON : INVALID_INPUT);
        }
    }

    private void advance(Reply reply, MessageContext context) {
        if (reply.getNext() != null) {
            enter(reply.getNext(), context);
            return;
        }
        if (reply.getPost() != null) {
            reply.getPost().accept(context);
        }
        context.clear();
    }

    private void register(Command command) {
        if (commands.putIfAbsent(command.getName(), command) != null) {
            throw new IllegalStateException("Duplicate command " + command.getName());
        }
        command.replies().forEach(reply -> replies.put(reply.getId(), reply));
    }

    /** "/help@my_bot" and "/help" are the same command. */
    static String commandName(String text) {
        if (!text.startsWith("/")) {
            return text;
        }
        var end = text.indexOf('@');
        return end > 0 ? text.substring(0, end) : text;
    }

    private Command helpCommand() {
        return Command.create(HELP)
                .access(Role.ALL)
                .step(context -> {
                    var available = commandsFor(context.getUser().getRole());
                    var list = available.stream()
                            .map(command -> command.getName() + " - " + command.getHelp())
                            .collect(Collectors.joining("\n"));
                    context.send(list.isEmpty() ? "Для вас пока нет доступных команд." : "Команды:\n\n" + list);
                    return Step.TERMINATE;
                })
                .build();
    }
}
