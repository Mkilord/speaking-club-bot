package ru.mkilord.node.dialog;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface DialogStateRepository extends JpaRepository<DialogState, Long> {

    /** Creates an empty state if the chat has none. Safe to call from several replicas at once. */
    @Modifying
    @Query(value = """
            insert into dialog_state (chat_id, dialog_values, updated_at)
            values (:chatId, '{}'::jsonb, now())
            on conflict (chat_id) do nothing
            """, nativeQuery = true)
    void createIfAbsent(@Param("chatId") long chatId);

    /** SELECT ... FOR UPDATE: updates of one chat are processed one at a time across all replicas. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from DialogState s where s.chatId = :chatId")
    Optional<DialogState> lockByChatId(@Param("chatId") long chatId);

    @Modifying
    @Query(value = "update dialog_state set reply_id = null, dialog_values = '{}'::jsonb where chat_id = :chatId",
            nativeQuery = true)
    void resetDialog(@Param("chatId") long chatId);

    @Modifying
    @Query("delete from DialogState s where s.updatedAt < :before")
    int deleteIdleBefore(@Param("before") Instant before);

    /**
     * Remembers that the update was processed. Returns 0 if it was already there.
     * A concurrent duplicate waits on the primary key until the first transaction ends.
     */
    @Modifying
    @Query(value = "insert into processed_update (update_id) values (:updateId) on conflict do nothing",
            nativeQuery = true)
    int markProcessed(@Param("updateId") long updateId);

    @Modifying
    @Query(value = "delete from processed_update where processed_at < :before", nativeQuery = true)
    int deleteProcessedBefore(@Param("before") Instant before);
}
