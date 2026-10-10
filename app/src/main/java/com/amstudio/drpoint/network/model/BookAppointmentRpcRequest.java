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

    @SerializedName(value = "p_patient_name", alternate = {"patient_name", "patient_full_name"})
    private String patientName;

    @SerializedName(value = "p_patient_phone", alternate = {"patient_phone", "phone"})
    private String patientPhone;

    @SerializedName(value = "p_patient_age", alternate = {"patient_age", "age"})
    private Integer patientAge;

    @SerializedName(value = "p_patient_gender", alternate = {"patient_gender", "gender"})
    private String patientGender;

    @SerializedName(value = "p_slot_number", alternate = {"slot_number"})
    private Integer slotNumber;

    public BookAppointmentRpcRequest() {
    }

    public BookAppointmentRpcRequest(String patientId, String slotId, String appointmentType, String patientReason, String bookingSource) {
        this.patientId = patientId;
        this.slotId = slotId;
        this.appointmentType = appointmentType != null ? appointmentType : "clinic";
        this.patientReason = patientReason;
        this.bookingSource = bookingSource != null ? bookingSource : "patient";
    }

    public BookAppointmentRpcRequest(String patientId, String slotId, String appointmentType, String patientReason, String bookingSource, String patientName, String patientPhone) {
        this(patientId, slotId, appointmentType, patientReason, bookingSource);
        this.patientName = patientName;
        this.patientPhone = patientPhone;
    }

    public BookAppointmentRpcRequest(String patientId, String slotId, String appointmentType, String patientReason, String bookingSource, String patientName, String patientPhone, Integer patientAge, String patientGender) {
        this(patientId, slotId, appointmentType, patientReason, bookingSource, patientName, patientPhone);
        this.patientAge = patientAge;
        this.patientGender = patientGender;
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

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }

    public Integer getPatientAge() { return patientAge; }
    public void setPatientAge(Integer patientAge) { this.patientAge = patientAge; }

    public String getPatientGender() { return patientGender; }
    public void setPatientGender(String patientGender) { this.patientGender = patientGender; }

    public Integer getSlotNumber() { return slotNumber; }
    public void setSlotNumber(Integer slotNumber) { this.slotNumber = slotNumber; }
}
