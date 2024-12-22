package ru.mkilord.node.common.menu;

import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static lombok.AccessLevel.PRIVATE;

@Getter
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class Item {

    String id;
    String text;

    public static List<Item> of(Item... items) {
        return new ArrayList<>(List.of(items));
    }

    public Item(String id, String text) {
        this.id = id;
        this.text = text;
    }

    public Item(String text) {
        this(UUID.randomUUID().toString(), text);
    }
}