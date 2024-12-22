package ru.mkilord.node.common.menu;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Getter
@FieldDefaults(level = PRIVATE)
@AllArgsConstructor()
public class Item {
    String id;
    String text;
}