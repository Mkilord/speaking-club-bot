package ru.mkilord.node.common.menu;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.mkilord.node.common.command.MessageContext;
import ru.mkilord.node.common.command.Step;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static lombok.AccessLevel.PRIVATE;

@FieldDefaults(makeFinal = true, level = PRIVATE)
@AllArgsConstructor(access = PRIVATE)
public class Menu {
    InlineKeyboardMarkup keyboardMarkup;

    public InlineKeyboardMarkup showMenu() {
        return keyboardMarkup;
    }

    @Getter
    List<Item> items;

    public Step onClick(MessageContext context) {
        return items.stream().filter(item -> Objects.equals(item.getId(), context.getText()))
                .findFirst()
                .flatMap(Item::getOnClick)
                .map(onClick -> onClick.apply(context)).orElse(Step.TERMINATE);
    }

    public static Builder builder() {
        return new Builder();
    }

    @FieldDefaults(level = PRIVATE)
    public static class Builder {
        final List<Item> items = new ArrayList<>();

        public Builder items(Item item) {
            items.add(item);
            return this;
        }

        public Builder items(Item... items) {
            this.items.addAll(Arrays.asList(items));
            return this;
        }

        public Builder items(List<Item> buttons) {
            this.items.addAll(buttons);
            return this;
        }

        public Menu build() {
            if (items.isEmpty()) {
                throw new RuntimeException("No items to build!");
            }
            var buttons = items.stream().map(item -> {
                var button = new InlineKeyboardButton(item.getText());
                button.setCallbackData(item.getId());
                button.setText(item.getText());
                return button;
            }).toList();
            var wrappedList = buttons.stream().map(Arrays::asList).toList();
            var keyboard = InlineKeyboardMarkup.builder().keyboard(wrappedList).build();
            return new Menu(keyboard, items);
        }
    }
}
