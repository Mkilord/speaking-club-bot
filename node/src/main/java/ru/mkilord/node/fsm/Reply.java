package ru.mkilord.node.fsm;

import lombok.Getter;

import java.util.function.Consumer;

/** A step of a concrete command: an {@link Input} with an id and a link to the next step. */
final class Reply {

    @Getter
    private final String id;
    private final Input input;
    @Getter
    private Reply next;
    @Getter
    private Consumer<MessageContext> post;

    Reply(String id, Input input) {
        this.id = id;
        this.input = input;
    }

    void setNext(Reply next) {
        this.next = next;
    }

    void setPost(Consumer<MessageContext> post) {
        this.post = post;
    }

    boolean waitsForInput() {
        return input.waitsForInput();
    }

    boolean isMenu() {
        return input.menu != null;
    }

    /** Shows the step to the user. */
    Step show(MessageContext context) {
        if (input.menu != null) {
            var menu = input.menu.apply(context);
            if (menu == null) {
                return Step.TERMINATE;
            }
            context.send(menu.getTitle(), menu.keyboard(id));
            return Step.NEXT;
        }
        return input.preview == null ? Step.NEXT : input.preview.apply(context);
    }

    /** Handles the user's answer to this step. */
    Step handle(MessageContext context) {
        if (input.menu != null) {
            var key = Menu.selectedKey(context, id);
            if (key.isEmpty()) {
                return Step.INVALID;
            }
            var menu = input.menu.apply(context);
            return menu == null ? Step.TERMINATE : menu.click(context, key.get());
        }
        if (input.action != null) {
            // A button from an old message is not an answer to a text question.
            return context.isCallback() ? Step.INVALID : input.action.apply(context);
        }
        return Step.NEXT;
    }
}
