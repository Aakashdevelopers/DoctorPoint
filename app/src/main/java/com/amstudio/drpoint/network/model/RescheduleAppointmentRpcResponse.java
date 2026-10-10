package com.amstudio.drpoint.network.model;

import com.google.gson.annotations.SerializedName;

public class RescheduleAppointmentRpcResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("appointment_id")
    private String appointmentId;

    @SerializedName("slot_id")
    private String slotId;

    @SerializedName("slot_date")
    private String slotDate;

    @SerializedName("start_time")
    private String startTime;

    @SerializedName("token_number")
    private int tokenNumber;

    public RescheduleAppointmentRpcResponse() {
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getSlotId() { return slotId; }
    public void setSlotId(String slotId) { this.slotId = slotId; }

    public String getSlotDate() { return slotDate; }
    public void setSlotDate(String slotDate) { this.slotDate = slotDate; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public int getTokenNumber() { return tokenNumber; }
    public void setTokenNumber(int tokenNumber) { this.tokenNumber = tokenNumber; }
}
