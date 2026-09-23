package com.amstudio.drpoint.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class CommissionSettings implements Serializable {

    @SerializedName("id")
    private String id;

    @SerializedName("setting_key")
    private String settingKey;

    @SerializedName("new_patient_commission")
    private double newPatientCommission = 10;

    @SerializedName("new_patient_type")
    private String newPatientType = "percentage"; // "percentage" or "flat"

    @SerializedName("followup_commission")
    private double followupCommission = 5;

    @SerializedName("followup_type")
    private String followupType = "percentage"; // "percentage" or "flat"

    @SerializedName("notes")
    private String notes;

    public CommissionSettings() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSettingKey() { return settingKey; }
    public void setSettingKey(String settingKey) { this.settingKey = settingKey; }

    public double getNewPatientCommission() { return newPatientCommission; }
    public void setNewPatientCommission(double newPatientCommission) { this.newPatientCommission = newPatientCommission; }

    public String getNewPatientType() { return newPatientType; }
    public void setNewPatientType(String newPatientType) { this.newPatientType = newPatientType; }

    public double getFollowupCommission() { return followupCommission; }
    public void setFollowupCommission(double followupCommission) { this.followupCommission = followupCommission; }

    public String getFollowupType() { return followupType; }
    public void setFollowupType(String followupType) { this.followupType = followupType; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public double calculateCommission(double totalFee, boolean isFollowUp) {
        double commissionVal = isFollowUp ? followupCommission : newPatientCommission;
        String type = isFollowUp ? followupType : newPatientType;

        if (type == null) type = "percentage";
        type = type.toLowerCase().trim();

        double commAmount;
        if (type.contains("percent") || type.contains("%")) {
            commAmount = (totalFee * commissionVal) / 100.0;
        } else {
            commAmount = commissionVal;
        }

        if (commAmount < 0) commAmount = 0;
        if (commAmount > totalFee) commAmount = totalFee;
        return commAmount;
    }

    public double calculateDoctorEarning(double totalFee, boolean isFollowUp) {
        double commAmount = calculateCommission(totalFee, isFollowUp);
        double netEarning = totalFee - commAmount;
        return Math.max(0, netEarning);
    }
}
