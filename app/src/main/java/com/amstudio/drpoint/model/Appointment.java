package com.amstudio.drpoint.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

public class Appointment implements Serializable {

    @SerializedName("id")
    private String id;

    @SerializedName(value = "patient_id", alternate = {"user_id"})
    private String patientId;

    @SerializedName("doctor_id")
    private String doctorId;

    @SerializedName(value = "doctor_name", alternate = {"doctorName"})
    private String doctorName;

    @SerializedName(value = "doctor_specialization", alternate = {"specialization", "speciality"})
    private String specialization;

    @SerializedName("doctor")
    private Doctor doctor;

    @SerializedName("clinic_id")
    private String clinicId;

    @SerializedName(value = "clinic_name", alternate = {"clinicName"})
    private String clinicName;

    @SerializedName(value = "clinic_location", alternate = {"location", "address"})
    private String location;

    @SerializedName("slot_id")
    private String slotId;

    @SerializedName(value = "appointment_date", alternate = {"date", "slot_date"})
    private String appointmentDate;

    @SerializedName(value = "start_time", alternate = {"appointment_time", "time"})
    private String startTime;

    @SerializedName("end_time")
    private String endTime;

    @SerializedName("appointment_type")
    private String appointmentType = "clinic";

    @SerializedName("status")
    private String status;

    @SerializedName("payment_status")
    private String paymentStatus = "Pending";

    @SerializedName(value = "amount", alternate = {"fee"})
    private int amount;

    @SerializedName("token_number")
    private int tokenNumber;

    @SerializedName(value = "patient_reason", alternate = {"notes"})
    private String patientReason;

    @SerializedName("prescription")
    private String prescription;

    private int imageRes;

    public Appointment() {
    }

    public Appointment(String id, String doctorName, String specialization, String date, String time, String clinicName, String location, String status) {
        this(id, "user_default", "doc_1", doctorName, specialization, date, time, clinicName, location, status, 900, "", 0);
    }

    public Appointment(String id, String doctorId, String doctorName, String specialization, String date, String time, String clinicName, String location, String status, int fee, int imageRes) {
        this(id, "user_default", doctorId, doctorName, specialization, date, time, clinicName, location, status, fee, "", imageRes);
    }

    public Appointment(String id, String patientId, String doctorId, String doctorName, String specialization, String appointmentDate, String startTime, String clinicName, String location, String status, int amount, String patientReason, int imageRes) {
        this.id = id;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.specialization = specialization;
        this.appointmentDate = appointmentDate;
        this.startTime = startTime;
        this.clinicName = clinicName;
        this.location = location;
        this.status = status;
        this.amount = amount;
        this.patientReason = patientReason;
        this.imageRes = imageRes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getUserId() { return patientId; }
    public void setUserId(String userId) { this.patientId = userId; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }

    public String getDoctorName() {
        if (doctorName != null && !doctorName.trim().isEmpty() && !doctorName.equalsIgnoreCase("Dr. Medical Specialist")) {
            return doctorName;
        }
        if (doctor != null && doctor.getName() != null && !doctor.getName().trim().isEmpty()) {
            return doctor.getName();
        }
        return "Dr. Medical Specialist";
    }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getSpecialization() {
        if (specialization != null && !specialization.trim().isEmpty() && !specialization.equalsIgnoreCase("Healthcare Specialist")) {
            return specialization;
        }
        if (doctor != null && doctor.getSpecialization() != null && !doctor.getSpecialization().trim().isEmpty()) {
            return doctor.getSpecialization();
        }
        return "General Physician";
    }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getClinicId() { return clinicId; }
    public void setClinicId(String clinicId) { this.clinicId = clinicId; }

    public String getClinicName() {
        if (clinicName != null && !clinicName.trim().isEmpty() && !clinicName.equalsIgnoreCase("Doctor Point Care Centre")) {
            return clinicName;
        }
        if (doctor != null && doctor.getClinicName() != null && !doctor.getClinicName().trim().isEmpty()) {
            return doctor.getClinicName();
        }
        return "Care Clinic";
    }
    public void setClinicName(String clinicName) { this.clinicName = clinicName; }

    public String getLocation() {
        if (location != null && !location.trim().isEmpty() && !location.equalsIgnoreCase("Main Clinic Centre")) {
            return location;
        }
        if (doctor != null && doctor.getLocation() != null && !doctor.getLocation().trim().isEmpty()) {
            return doctor.getLocation();
        }
        return "Main City Branch";
    }
    public void setLocation(String location) { this.location = location; }

    public String getFormattedTime() {
        if (startTime == null || startTime.trim().isEmpty()) return "10:00 AM";
        try {
            String clean = startTime.trim();
            if (clean.contains("AM") || clean.contains("PM") || clean.contains("am") || clean.contains("pm")) {
                return clean;
            }
            String[] parts = clean.split(":");
            int hour = Integer.parseInt(parts[0]);
            int min = Integer.parseInt(parts[1]);
            String ampm = hour >= 12 ? "PM" : "AM";
            int hour12 = hour % 12;
            if (hour12 == 0) hour12 = 12;
            return String.format(Locale.US, "%02d:%02d %s", hour12, min, ampm);
        } catch (Exception e) {
            return startTime;
        }
    }

    public String getSlotId() { return slotId; }
    public void setSlotId(String slotId) { this.slotId = slotId; }

    public String getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(String appointmentDate) { this.appointmentDate = appointmentDate; }

    public String getDate() { return appointmentDate; }
    public void setDate(String date) { this.appointmentDate = date; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getTime() { return startTime; }
    public void setTime(String time) { this.startTime = time; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getAppointmentType() {
        return appointmentType != null ? appointmentType : "clinic";
    }
    public void setAppointmentType(String appointmentType) { this.appointmentType = appointmentType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentStatus() {
        return paymentStatus != null ? paymentStatus : "Pending";
    }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    public int getFee() { return amount; }
    public void setFee(int fee) { this.amount = fee; }

    public int getTokenNumber() { return tokenNumber; }
    public void setTokenNumber(int tokenNumber) { this.tokenNumber = tokenNumber; }

    public String getPatientReason() { return patientReason; }
    public void setPatientReason(String patientReason) { this.patientReason = patientReason; }

    public String getNotes() { return patientReason; }
    public void setNotes(String notes) { this.patientReason = notes; }

    public String getPrescription() { return prescription; }
    public void setPrescription(String prescription) { this.prescription = prescription; }

    public int getImageRes() { return imageRes; }
    public void setImageRes(int imageRes) { this.imageRes = imageRes; }

    public String getUserFriendlyStatus() {
        if (status == null) return "Pending";
        String s = status.trim().toLowerCase();
        switch (s) {
            case "confirmed": return "✓ Confirmed";
            case "checked_in": return "Checked In";
            case "waiting": return "In Waiting Room";
            case "in_consultation": return "In Consultation";
            case "completed": return "Completed";
            case "cancelled": return "Cancelled";
            case "rejected": return "Rejected";
            case "no_show": return "No Show";
            default: return "Pending";
        }
    }

    public boolean isCancellable() {
        if (status == null) return true;
        String s = status.trim().toLowerCase();
        return "pending".equals(s) || "confirmed".equalsIgnoreCase(s) || "checked_in".equalsIgnoreCase(s) || "waiting".equalsIgnoreCase(s);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Appointment that = (Appointment) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
