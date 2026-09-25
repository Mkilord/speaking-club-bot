package ru.mkilord.node.fsm;

/** What the engine does after a handler. */
public enum Step {
    /** Go to the next reply of the command, or finish the command after the last one. */
    NEXT,
    /** Stay on the current reply and wait for another input. The handler already explained what is wrong. */
    REPEAT,
    /** Stop the command right now. The post action is not called. */
    TERMINATE,
    /** Input does not fit the current reply. The engine sends a generic hint and waits. */
    INVALID
}
