package com.amstudio.drpoint.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Objects;

public class NotificationItem implements Serializable {

    @SerializedName("id")
    private String id;

    @SerializedName(value = "patient_id", alternate = {"user_id"})
    private String patientId;

    @SerializedName("title")
    private String title;

    @SerializedName(value = "message", alternate = {"body"})
    private String message;

    @SerializedName("type")
    private String type = "appointment_confirmed";

    @SerializedName("is_read")
    private boolean isRead = false;

    @SerializedName("created_at")
    private String createdAt;

    public NotificationItem() {
    }

    public NotificationItem(String id, String patientId, String title, String message, String type, boolean isRead, String createdAt) {
        this.id = id;
        this.patientId = patientId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getTitle() {
        return title != null ? title : "Notification";
    }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() {
        return message != null ? message : "";
    }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotificationItem that = (NotificationItem) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
