package ru.mkilord.node.fsm;

import java.util.function.Function;

/**
 * Button of a {@link Menu}.
 *
 * @param key     stable identifier, part of the callback data
 * @param text    button label
 * @param onClick handler, or null to use the menu's {@code onSelect}
 * @param command true if the button just sends a command like "/help"
 */
public record Item(String key, String text, Function<MessageContext, Step> onClick, boolean command) {

    public static Item of(String key, String text) {
        return new Item(key, text, null, false);
    }

    public static Item of(String key, String text, Function<MessageContext, Step> onClick) {
        return new Item(key, text, onClick, false);
    }

    /** Button that works like typing the command. It keeps working in old messages. */
    public static Item command(String command, String text) {
        return new Item(command, text, null, true);
    }
}
