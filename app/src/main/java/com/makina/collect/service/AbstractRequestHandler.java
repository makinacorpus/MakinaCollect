package com.makina.collect.service;

import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.NonNull;
import android.util.Log;

/**
 * {@code Service} handler used by {@link RequestHandlerService}.
 * <p/>
 * Called when {@link RequestHandlerService} receive a {@code Message}
 * corresponding to the concrete implementation of {@link AbstractRequestHandler}.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public abstract class AbstractRequestHandler {

    private static final String TAG = ConnectClientRequestHandler.class.getSimpleName();

    public static final String KEY_HANDLER = "KEY_HANDLER";
    public static final String KEY_CLIENT_TOKEN = "KEY_CLIENT_TOKEN";

    private final Context mContext;
    RequestHandlerServiceListener mRequestHandlerServiceListener;

    public AbstractRequestHandler(final Context pContext) {

        this.mContext = pContext;
    }

    public Context getContext() {

        return this.mContext;
    }

    public void setRequestHandlerServiceListener(@NonNull RequestHandlerServiceListener mRequestHandlerServiceListener) {

        this.mRequestHandlerServiceListener = mRequestHandlerServiceListener;
    }

    /**
     * Checks if the given {@code Message} is eligible for this instance of {@link AbstractRequestHandler}.
     *
     * @param message the {@code Message} to parse
     *
     * @return {@code true} if the given {@code Message} is eligible for this instance, {@code false} otherwise
     */
    public boolean checkMessage(@NonNull final Message message) {

        final Bundle data = message.peekData();

        return (data != null) && data.containsKey(KEY_HANDLER) && data.containsKey(KEY_CLIENT_TOKEN) && ((Class<?>) data.getSerializable(KEY_HANDLER)).isInstance(this);
    }

    /**
     * Tries to send a message using {@link RequestHandlerServiceListener} instance.
     *
     * @param data the data as {@code Bundle} to be sent to the receiver
     */
    public void sendMessage(@NonNull final Bundle data) {

        if (mRequestHandlerServiceListener == null) {
            Log.w(TAG,
                  "RequestHandlerServiceListener is not defined. The Message will not be sent.");

            return;
        }

        mRequestHandlerServiceListener.sendMessage(data.getString(KEY_CLIENT_TOKEN),
                                                   data);
    }

    /**
     * Handles incoming message from {@link RequestHandlerService}.
     *
     * @param message the message to be processed
     */
    protected abstract void handleMessageFromService(Message message);

    /**
     * Handles message received from {@link RequestHandlerServiceClient}.
     *
     * @param message the message to be processed
     */
    protected void handleMessageFromClient(
            Message message,
            @NonNull final RequestHandlerClientListener requestHandlerClientListener) {

        if (checkMessage(message)) {
            requestHandlerClientListener.onHandleMessage(AbstractRequestHandler.this,
                                                         message.getData());
        }
    }

    /**
     * Provides callbacks that are called when {@link AbstractRequestHandler}
     * needs to send {@code Message} to {@link RequestHandlerService}.
     *
     * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
     */
    public interface RequestHandlerServiceListener {

        /**
         * Register a given client with a valid token (e.g. not {@code null}).
         *
         * @param token     the token as key to retrieve the client's {@code Messenger}
         * @param messenger the client {@code Messenger} used to send {@code Message}
         */
        void addClient(@NonNull final String token,
                       final Messenger messenger);

        /**
         * Unregister a client for a given token.
         *
         * @param token the token to use
         */
        void removeClient(@NonNull final String token);

        /**
         * Sends a message through {@link RequestHandlerService}.
         *
         * @param token the token used to retrieve the corresponding {@code Messenger}
         * @param data  the data as {@code Bundle} to be sent to the receiver
         */
        void sendMessage(final String token,
                         @NonNull final Bundle data);
    }

    /**
     * Provides callbacks that are called when {@link AbstractRequestHandler}
     * receive a {@code Message} from {@link RequestHandlerServiceClient}.
     *
     * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
     */
    public interface RequestHandlerClientListener {

        /**
         * Called when the {@code Message} was received from {@link RequestHandlerServiceClient}.
         *
         * @param requestHandler the {@link AbstractRequestHandler}
         *                       which perform the received {@code Message}
         * @param data           the {@code Message} data
         */
        void onHandleMessage(@NonNull final AbstractRequestHandler requestHandler,
                             @NonNull final Bundle data);
    }
}
