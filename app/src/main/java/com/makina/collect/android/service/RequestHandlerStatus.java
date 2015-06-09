package com.makina.collect.android.service;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Indicates the current status of {@link AbstractRequestHandler}.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class RequestHandlerStatus
        implements Parcelable {

    private Status mStatus;
    private String mMessage;

    public RequestHandlerStatus(@NonNull final Status pStatus) {
        this(pStatus,
             null);
    }

    public RequestHandlerStatus(
            @NonNull final Status pStatus,
            @Nullable final String pMessage) {
        this.mStatus = pStatus;
        this.mMessage = pMessage;
    }

    public RequestHandlerStatus(Parcel source) {
        mStatus = (Status) source.readSerializable();
        mMessage = source.readString();
    }

    @NonNull
    public Status getStatus() {
        return mStatus;
    }

    @Nullable
    public String getMessage() {
        return mMessage;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(mStatus);
        dest.writeString(mMessage);
    }

    public static final Creator<RequestHandlerStatus> CREATOR = new Creator<RequestHandlerStatus>() {
        @Override
        public RequestHandlerStatus createFromParcel(Parcel source) {
            return new RequestHandlerStatus(source);
        }

        @Override
        public RequestHandlerStatus[] newArray(int size) {
            return new RequestHandlerStatus[size];
        }
    };

    public enum Status {

        /**
         * Indicates that the {@link AbstractRequestHandler} has not been executed yet.
         */
        PENDING,

        /**
         * Indicates that the {@link AbstractRequestHandler} is still running.
         */
        RUNNING,

        /**
         * Indicates that the {@link AbstractRequestHandler} has been canceled.
         */
        ABORTED,

        /**
         * Indicates that the {@link AbstractRequestHandler} has finished successfully.
         */
        FINISHED,

        /**
         * Indicates that the {@link AbstractRequestHandler} has finished with warnings.
         */
        FINISHED_WITH_WARNINGS,

        /**
         * Indicates that the {@link AbstractRequestHandler} has finished with errors.
         */
        FINISHED_WITH_ERRORS
    }
}
