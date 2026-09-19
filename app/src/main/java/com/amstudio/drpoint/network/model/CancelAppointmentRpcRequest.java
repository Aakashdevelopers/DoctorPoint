package com.amstudio.drpoint.network.model;

import com.google.gson.annotations.SerializedName;

public class CancelAppointmentRpcRequest {

    @SerializedName("p_appointment_id")
    private String appointmentId;

    @SerializedName("p_canceller_id")
    private String cancellerId;

    @SerializedName("p_cancellation_reason")
    private String cancellationReason;

    public CancelAppointmentRpcRequest() {
    }

    public CancelAppointmentRpcRequest(String appointmentId, String cancellerId, String cancellationReason) {
        this.appointmentId = appointmentId;
        this.cancellerId = cancellerId;
        this.cancellationReason = cancellationReason != null ? cancellationReason : "Cancelled by patient";
    }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getCancellerId() { return cancellerId; }
    public void setCancellerId(String cancellerId) { this.cancellerId = cancellerId; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
}
