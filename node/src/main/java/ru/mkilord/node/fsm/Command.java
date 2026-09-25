package ru.mkilord.node.fsm;

import lombok.Getter;
import ru.mkilord.node.model.Role;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Bot command: an optional action, a chain of replies and an optional post action.
 * <pre>
 * /register -> action -> [full name] -> [phone] -> [email] -> post
 * </pre>
 * Reply ids are built from the command name and the position, for example {@code /register1}.
 * They are the same on every replica and after a restart, so they can be stored in the database.
 */
@Getter
public final class Command {

    private final String name;
    private final String help;
    private final Set<Role> roles;
    private final Function<MessageContext, Step> action;
    private final Reply firstReply;

    private Command(String name, String help, Set<Role> roles,
                    Function<MessageContext, Step> action, Reply firstReply) {
        this.name = name;
        this.help = help;
        this.roles = roles;
        this.action = action;
        this.firstReply = firstReply;
    }

    public static Builder create(String name) {
        return new Builder(name);
    }

    public boolean isAvailableFor(Role role) {
        return roles.contains(role);
    }

    List<Reply> replies() {
        var result = new ArrayList<Reply>();
        for (var reply = firstReply; reply != null; reply = reply.getNext()) {
            result.add(reply);
        }
        return result;
    }

    public static final class Builder {
        private final String name;
        private String help;
        private Set<Role> roles = EnumSet.of(Role.USER);
        private Function<MessageContext, Step> action;
        private final List<Input> inputs = new ArrayList<>();
        private Consumer<MessageContext> post;

        private Builder(String name) {
            if (!name.startsWith("/")) {
                throw new IllegalArgumentException("Command must start with '/': " + name);
            }
            this.name = name;
        }

        public Builder access(Set<Role> roles) {
            this.roles = EnumSet.copyOf(roles);
            return this;
        }

        public Builder access(Role role) {
            this.roles = EnumSet.of(role);
            return this;
        }

        /** Description for /help. Commands without it are hidden from the list. */
        public Builder help(String help) {
            this.help = help;
            return this;
        }

        /** Action that decides whether the command goes on to its inputs. */
        public Builder step(Function<MessageContext, Step> action) {
            this.action = action;
            return this;
        }

        /** Action that always goes on to the inputs. */
        public Builder action(Consumer<MessageContext> action) {
            this.action = context -> {
                action.accept(context);
                return Step.NEXT;
            };
            return this;
        }

        public Builder input(Input... inputs) {
            this.inputs.addAll(List.of(inputs));
            return this;
        }

        /** Called when the last reply finishes with {@link Step#NEXT}. */
        public Builder post(Consumer<MessageContext> post) {
            this.post = post;
            return this;
        }

        public Command build() {
            if (post != null && inputs.isEmpty()) {
                throw new IllegalStateException("Command " + name + " has a post action but no inputs");
            }
            Reply first = null;
            Reply previous = null;
            for (int i = 0; i < inputs.size(); i++) {
                var reply = new Reply(name + i, inputs.get(i));
                if (previous == null) {
                    first = reply;
                } else {
                    previous.setNext(reply);
                }
                previous = reply;
            }
            if (previous != null) {
                previous.setPost(post);
            }
            return new Command(name, help, roles, action, first);
        }
    }
}
