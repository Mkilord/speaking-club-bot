package ru.mkilord.node.fsm;

import java.util.List;

/** A group of commands. Every Spring bean of this type is registered in {@link DialogEngine}. */
public interface CommandCatalog {
    List<Command> commands();
}
