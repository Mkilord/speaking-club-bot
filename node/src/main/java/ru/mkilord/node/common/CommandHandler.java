package ru.mkilord.node.common;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.mkilord.node.common.context.ContextFlow;
import ru.mkilord.node.common.context.MessageContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@FieldDefaults(level = PRIVATE)
@RequiredArgsConstructor
@Component
public class CommandHandler {
    Map<String, Reply> replyMap;

    List<Command> commandsList;

    final ContextFlow contextFlow;

    Consumer<MessageContext> unknownCommandCallback;

    public void registerCommands(CommandCatalog commandCatalog, Consumer<MessageContext> unknownCommandCallback) {
        this.unknownCommandCallback = unknownCommandCallback;
        commandsList = commandCatalog.setCommands();
        replyMap = commandsList.stream()
                .map(Command::extractReplies)
                .flatMap(repliesMap -> repliesMap.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (reply1, _) -> reply1));
    }

    private boolean processReply(MessageContext context) {
        if (!context.hasReply()) return false;
        return Optional.ofNullable(replyMap.get(context.getReplyId()))
                .map(reply -> reply.processAction(context))
                .orElse(false);
    }

    private boolean processCommand(MessageContext context) {
        return commandsList.stream()
                .filter(command -> command.matchConditions(context))
                .findFirst()
                .map(command -> command.processAction(context)).orElse(false);
    }

    public void process(Update update) {
        var context = contextFlow.lookupOrCreateContext(update);
        if (processCommand(context) || processReply(context)) return;
        unknownCommandCallback.accept(context);
    }
}
