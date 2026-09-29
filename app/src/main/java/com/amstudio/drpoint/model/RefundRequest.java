package com.amstudio.drpoint.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class RefundRequest implements Serializable {

    @SerializedName("id")
    private String id;

    @SerializedName("appointment_id")
    private String appointmentId;

    @SerializedName("patient_id")
    private String patientId;

    @SerializedName("patient_name")
    private String patientName;

    @SerializedName("patient_phone")
    private String patientPhone;

    @SerializedName("doctor_id")
    private String doctorId;

    @SerializedName("doctor_name")
    private String doctorName;

    @SerializedName("doctor_specialization")
    private String doctorSpecialization;

    @SerializedName("clinic_name")
    private String clinicName;

    @SerializedName("appointment_date")
    private String appointmentDate;

    @SerializedName("amount")
    private int amount;

    @SerializedName("reason")
    private String reason;

    @SerializedName("status")
    private String status = "pending";

    @SerializedName("created_at")
    private String createdAt;

    public RefundRequest() {
    }

    public RefundRequest(String appointmentId, String patientId, String patientName, String patientPhone,
                         String doctorId, String doctorName, String doctorSpecialization, String clinicName,
                         String appointmentDate, int amount, String reason) {
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.patientPhone = patientPhone;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.doctorSpecialization = doctorSpecialization;
        this.clinicName = clinicName;
        this.appointmentDate = appointmentDate;
        this.amount = amount;
        this.reason = reason;
        this.status = "pending";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDoctorSpecialization() { return doctorSpecialization; }
    public void setDoctorSpecialization(String doctorSpecialization) { this.doctorSpecialization = doctorSpecialization; }

    public String getClinicName() { return clinicName; }
    public void setClinicName(String clinicName) { this.clinicName = clinicName; }

    public String getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(String appointmentDate) { this.appointmentDate = appointmentDate; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
