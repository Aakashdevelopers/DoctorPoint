package com.amstudio.drpoint.network.model;

import com.google.gson.annotations.SerializedName;

public class BookAppointmentRpcRequest {

    @SerializedName("p_patient_id")
    private String patientId;

    @SerializedName("p_slot_id")
    private String slotId;

    @SerializedName("p_appointment_type")
    private String appointmentType;

    @SerializedName("p_patient_reason")
    private String patientReason;

    @SerializedName("p_booking_source")
    private String bookingSource;

    public BookAppointmentRpcRequest() {
    }

    public BookAppointmentRpcRequest(String patientId, String slotId, String appointmentType, String patientReason, String bookingSource) {
        this.patientId = patientId;
        this.slotId = slotId;
        this.appointmentType = appointmentType != null ? appointmentType : "clinic";
        this.patientReason = patientReason;
        this.bookingSource = bookingSource != null ? bookingSource : "patient";
    }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getSlotId() { return slotId; }
    public void setSlotId(String slotId) { this.slotId = slotId; }

    public String getAppointmentType() { return appointmentType; }
    public void setAppointmentType(String appointmentType) { this.appointmentType = appointmentType; }

    public String getPatientReason() { return patientReason; }
    public void setPatientReason(String patientReason) { this.patientReason = patientReason; }

    public String getBookingSource() { return bookingSource; }
    public void setBookingSource(String bookingSource) { this.bookingSource = bookingSource; }
}
