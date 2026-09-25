package ru.mkilord.node.fsm;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Reusable description of one dialog step. {@link Command.Builder} turns it into a {@link Reply}.
 * There are three kinds:
 * <ul>
 *     <li>text: shows a prompt and validates the typed answer;</li>
 *     <li>menu: shows buttons and handles the pressed one;</li>
 *     <li>message: shows something and moves on without waiting for input.</li>
 * </ul>
 */
public final class Input {

    final Function<MessageContext, Step> preview;
    final Function<MessageContext, Step> action;
    final Function<MessageContext, Menu> menu;

    private Input(Function<MessageContext, Step> preview,
                  Function<MessageContext, Step> action,
                  Function<MessageContext, Menu> menu) {
        this.preview = preview;
        this.action = action;
        this.menu = menu;
    }

    /** Sends the prompt, then passes the answer to the action. */
    public static Input text(String prompt, Function<MessageContext, Step> action) {
        return new Input(context -> {
            context.send(prompt);
            return Step.NEXT;
        }, action, null);
    }

    /**
     * Builds a menu from the dialog state. The function is called twice: to show the menu
     * and to handle the click. Return null when there is nothing to show; the command stops.
     */
    public static Input menu(Function<MessageContext, Menu> menu) {
        return new Input(null, null, menu);
    }

    /** Does something and goes on to the next step without waiting for the user. */
    public static Input message(Consumer<MessageContext> preview) {
        return new Input(context -> {
            preview.accept(context);
            return Step.NEXT;
        }, null, null);
    }

    /** Like {@link #message(Consumer)}, but can stop the command. */
    public static Input step(Function<MessageContext, Step> preview) {
        return new Input(preview, null, null);
    }

    boolean waitsForInput() {
        return action != null || menu != null;
    }
}
