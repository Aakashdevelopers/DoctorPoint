package com.amstudio.drpoint.network.model;

import com.google.gson.annotations.SerializedName;

public class ErrorResponse {
    @SerializedName("msg")
    private String msg;

    @SerializedName("message")
    private String message;

    @SerializedName("error_description")
    private String errorDescription;

    @SerializedName("error")
    private String error;

    public String getErrorMessage() {
        if (msg != null && !msg.isEmpty()) return msg;
        if (message != null && !message.isEmpty()) return message;
        if (errorDescription != null && !errorDescription.isEmpty()) return errorDescription;
        if (error != null && !error.isEmpty()) return error;
        return "An unknown error occurred.";
    }
}
