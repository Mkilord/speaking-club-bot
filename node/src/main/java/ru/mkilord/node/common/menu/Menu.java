package ru.mkilord.node.common.menu;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@FieldDefaults(makeFinal = true, level = PRIVATE)
@AllArgsConstructor(access = PRIVATE)
@Getter
public class Menu {
    InlineKeyboardMarkup keyboardMarkup;

    @FieldDefaults(level = PRIVATE, makeFinal = true)
    public static class Builder {
        List<Item> items = new ArrayList<>();

        public Builder addButton(Item item) {
            items.add(item);
            return this;
        }

        public Builder addButtons(List<Item> buttons) {
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
            return new Menu(keyboard);
        }
    }
}
