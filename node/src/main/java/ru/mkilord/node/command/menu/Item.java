package ru.mkilord.node.command.menu;

import lombok.Getter;
import lombok.experimental.FieldDefaults;
import ru.mkilord.node.command.MessageContext;
import ru.mkilord.node.command.Step;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static lombok.AccessLevel.PRIVATE;

@Getter
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class Item {

    String id;
    String text;

    Function<MessageContext, Step> onClick;

    public Optional<Function<MessageContext, Step>> getOnClick() {
        return Optional.ofNullable(onClick);
    }

    public static List<Item> of(Item... items) {
        return new ArrayList<>(List.of(items));
    }

    public Item(String text, Function<MessageContext, Step> onClick) {
        this.id = UUID.randomUUID().toString();
        this.text = text;
        this.onClick = onClick;
    }

    public Item(String id, String text) {
        this.id = id;
        this.text = text;
        this.onClick = null;
    }
}