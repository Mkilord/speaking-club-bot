package ru.mkilord.dispatcher.bot;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.mkilord.dispatcher.messaging.UpdatePublisher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class UpdateRouterTest {

    private final UpdatePublisher publisher = mock(UpdatePublisher.class);
    private final AbsSender sender = mock(AbsSender.class);
    private final UpdateRouter router = new UpdateRouter(publisher);

    @Test
    void publishesTextMessages() throws Exception {
        var update = messageUpdate("/start");

        router.route(update, sender);

        verify(publisher).publish(update);
        verify(sender, never()).execute(any(SendMessage.class));
    }

    @Test
    void answersCallbackAndPublishesIt() throws Exception {
        var callback = new CallbackQuery();
        callback.setId("cb-1");
        callback.setData("/help");
        callback.setMessage(message(null));
        var update = new Update();
        update.setCallbackQuery(callback);

        router.route(update, sender);

        verify(sender).execute(argThat((AnswerCallbackQuery answer) -> "cb-1".equals(answer.getCallbackQueryId())));
        verify(publisher).publish(update);
    }

    @Test
    void repliesToUnsupportedMessagesWithoutPublishing() throws Exception {
        var update = messageUpdate(null);

        router.route(update, sender);

        verify(publisher, never()).publish(any());
        verify(sender).execute(argThat((SendMessage m) -> UpdateRouter.UNSUPPORTED_TEXT.equals(m.getText())));
    }

    @Test
    void tellsUserWhenBrokerIsDown() throws Exception {
        var update = messageUpdate("/start");
        doThrow(new AmqpConnectException(new RuntimeException("down"))).when(publisher).publish(update);

        router.route(update, sender);

        verify(sender).execute(argThat((SendMessage m) -> UpdateRouter.UNAVAILABLE_TEXT.equals(m.getText())));
    }

    private static Update messageUpdate(String text) {
        var update = new Update();
        update.setMessage(message(text));
        return update;
    }

    private static Message message(String text) {
        var message = new Message();
        message.setChat(new Chat(7L, "private"));
        message.setText(text);
        return message;
    }
}
