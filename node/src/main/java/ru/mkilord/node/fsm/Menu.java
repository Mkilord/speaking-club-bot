package ru.mkilord.node.fsm;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Inline keyboard with handlers.
 * <p>
 * A menu is not stored between messages. The reply builds it again from the dialog state
 * when the button is pressed, so handlers are plain lambdas and nothing has to be serialized.
 * Callback data has the form {@code m:<replyId>:<itemKey>}: a button from an older message
 * does not match the current reply and is rejected.
 */
public final class Menu {

    private static final String PREFIX = "m:";
    private static final String SEPARATOR = ":";
    private static final int MAX_CALLBACK_BYTES = 64;

    private final String title;
    private final List<Item> items;
    private final BiFunction<MessageContext, String, Step> onSelect;

    private Menu(String title, List<Item> items, BiFunction<MessageContext, String, Step> onSelect) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Menu '" + title + "' has no items");
        }
        this.title = title;
        this.items = List.copyOf(items);
        this.onSelect = onSelect;
    }

    public static Builder builder(String title) {
        return new Builder(title);
    }

    /** Keyboard with command buttons only, for example "Help" after registration. */
    public static InlineKeyboardMarkup commands(Item... items) {
        return keyboard(null, List.of(items));
    }

    public String getTitle() {
        return title;
    }

    InlineKeyboardMarkup keyboard(String replyId) {
        return keyboard(replyId, items);
    }

    Step click(MessageContext context, String key) {
        var item = items.stream().filter(i -> i.key().equals(key)).findFirst();
        if (item.isEmpty()) {
            return Step.INVALID;
        }
        if (item.get().onClick() != null) {
            return item.get().onClick().apply(context);
        }
        if (onSelect != null) {
            return onSelect.apply(context, key);
        }
        return Step.TERMINATE;
    }

    static Optional<String> selectedKey(MessageContext context, String replyId) {
        if (!context.isCallback()) {
            return Optional.empty();
        }
        var expectedPrefix = PREFIX + replyId + SEPARATOR;
        var data = context.getText();
        return data.startsWith(expectedPrefix)
                ? Optional.of(data.substring(expectedPrefix.length()))
                : Optional.empty();
    }

    private static InlineKeyboardMarkup keyboard(String replyId, List<Item> items) {
        var rows = items.stream()
                .map(item -> {
                    var data = item.command() ? item.key() : PREFIX + replyId + SEPARATOR + item.key();
                    if (data.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_CALLBACK_BYTES) {
                        throw new IllegalStateException("Callback data is longer than 64 bytes: " + data);
                    }
                    var button = InlineKeyboardButton.builder().text(item.text()).callbackData(data).build();
                    return List.of(button);
                })
                .toList();
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public static final class Builder {
        private final String title;
        private final List<Item> items = new ArrayList<>();
        private BiFunction<MessageContext, String, Step> onSelect;

        private Builder(String title) {
            this.title = Objects.requireNonNull(title);
        }

        public Builder item(Item item) {
            items.add(item);
            return this;
        }

        public Builder item(String key, String text, java.util.function.Function<MessageContext, Step> onClick) {
            return item(Item.of(key, text, onClick));
        }

        public Builder items(List<Item> items) {
            this.items.addAll(items);
            return this;
        }

        /** Handler for items without their own onClick. Receives the key of the pressed item. */
        public Builder onSelect(BiFunction<MessageContext, String, Step> onSelect) {
            this.onSelect = onSelect;
            return this;
        }

        public Menu build() {
            return new Menu(title, items, onSelect);
        }
    }
}
