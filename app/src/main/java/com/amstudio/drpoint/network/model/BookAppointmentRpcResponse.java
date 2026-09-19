package com.amstudio.drpoint.network.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class BookAppointmentRpcResponse implements Serializable {

    @SerializedName("success")
    private boolean success;

    @SerializedName("appointment_id")
    private String appointmentId;

    @SerializedName("slot_id")
    private String slotId;

    @SerializedName("doctor_id")
    private String doctorId;

    @SerializedName("patient_id")
    private String patientId;

    @SerializedName("slot_date")
    private String slotDate;

    @SerializedName("start_time")
    private String startTime;

    @SerializedName("amount")
    private double amount;

    @SerializedName("token_number")
    private int tokenNumber;

    @SerializedName("message")
    private String message;

    public BookAppointmentRpcResponse() {
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getSlotId() { return slotId; }
    public void setSlotId(String slotId) { this.slotId = slotId; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getSlotDate() { return slotDate; }
    public void setSlotDate(String slotDate) { this.slotDate = slotDate; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public int getTokenNumber() { return tokenNumber; }
    public void setTokenNumber(int tokenNumber) { this.tokenNumber = tokenNumber; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
