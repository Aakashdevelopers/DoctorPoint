package com.amstudio.drpoint.network.model;

import com.google.gson.annotations.SerializedName;

public class RescheduleAppointmentRpcRequest {

    @SerializedName("p_appointment_id")
    private String appointmentId;

    @SerializedName("p_patient_id")
    private String patientId;

    @SerializedName("p_new_slot_id")
    private String newSlotId;

    public RescheduleAppointmentRpcRequest() {
    }

    public RescheduleAppointmentRpcRequest(String appointmentId, String patientId, String newSlotId) {
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.newSlotId = newSlotId;
    }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getNewSlotId() { return newSlotId; }
    public void setNewSlotId(String newSlotId) { this.newSlotId = newSlotId; }
}
