package com.amstudio.drpoint.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Objects;

public class DoctorSlot implements Serializable {

    @SerializedName("id")
    private String id;

    @SerializedName("doctor_id")
    private String doctorId;

    @SerializedName("clinic_id")
    private String clinicId;

    @SerializedName("slot_date")
    private String slotDate;

    @SerializedName("start_time")
    private String startTime;

    @SerializedName("end_time")
    private String endTime;

    @SerializedName("status")
    private String status;

    @SerializedName("appointment_id")
    private String appointmentId;

    @SerializedName("morning_start")
    private String morningStart;

    @SerializedName("morning_end")
    private String morningEnd;

    @SerializedName("evening_start")
    private String eveningStart;

    @SerializedName("evening_end")
    private String eveningEnd;

    @SerializedName("slot_duration")
    private int slotDuration = 20;

    @SerializedName(value = "consult_duration", alternate = {"consultDuration"})
    private int consultDuration = 15;

    @SerializedName(value = "buffer_gap", alternate = {"bufferGap"})
    private int bufferGap = 5;

    @SerializedName("fee")
    private int fee = 500;

    @SerializedName(value = "followup_fee", alternate = {"followUpFee", "returning_patient_fee"})
    private int followUpFee = 300;

    public DoctorSlot() {
    }

    public DoctorSlot(String id, String doctorId, String clinicId, String slotDate, String startTime, String endTime, String status) {
        this.id = id;
        this.doctorId = doctorId;
        this.clinicId = clinicId;
        this.slotDate = slotDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getClinicId() { return clinicId; }
    public void setClinicId(String clinicId) { this.clinicId = clinicId; }

    public String getSlotDate() {
        if (slotDate != null) {
            String clean = slotDate.trim();
            if (clean.contains("T")) clean = clean.substring(0, clean.indexOf("T"));
            if (clean.contains(" ")) clean = clean.substring(0, clean.indexOf(" "));
            return clean;
        }
        return slotDate;
    }
    public void setSlotDate(String slotDate) { this.slotDate = slotDate; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getMorningStart() { return morningStart; }
    public void setMorningStart(String morningStart) { this.morningStart = morningStart; }

    public String getMorningEnd() { return morningEnd; }
    public void setMorningEnd(String morningEnd) { this.morningEnd = morningEnd; }

    public String getEveningStart() { return eveningStart; }
    public void setEveningStart(String eveningStart) { this.eveningStart = eveningStart; }

    public String getEveningEnd() { return eveningEnd; }
    public void setEveningEnd(String eveningEnd) { this.eveningEnd = eveningEnd; }

    public int getSlotDuration() { return slotDuration > 0 ? slotDuration : 20; }
    public void setSlotDuration(int slotDuration) { this.slotDuration = slotDuration; }

    public int getConsultDuration() { return consultDuration > 0 ? consultDuration : 15; }
    public void setConsultDuration(int consultDuration) { this.consultDuration = consultDuration; }

    public int getBufferGap() { return bufferGap >= 0 ? bufferGap : 5; }
    public void setBufferGap(int bufferGap) { this.bufferGap = bufferGap; }

    public int getFee() { return fee > 0 ? fee : 500; }
    public void setFee(int fee) { this.fee = fee; }

    public int getFollowUpFee() { return followUpFee > 0 ? followUpFee : (fee > 0 ? fee : 300); }
    public void setFollowUpFee(int followUpFee) { this.followUpFee = followUpFee; }

    public String getFormattedTime() {
        if (startTime == null) return "";
        try {
            String[] parts = startTime.split(":");
            int hour = Integer.parseInt(parts[0]);
            int min = Integer.parseInt(parts[1]);
            String ampm = hour >= 12 ? "PM" : "AM";
            int hour12 = hour % 12;
            if (hour12 == 0) hour12 = 12;
            return String.format("%02d:%02d %s", hour12, min, ampm);
        } catch (Exception e) {
            return startTime;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DoctorSlot slot = (DoctorSlot) o;
        return Objects.equals(id, slot.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
