package com.amstudio.drpoint.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Objects;

public class MedicalRecord implements Serializable {

    @SerializedName("id")
    private String id;

    @SerializedName(value = "patient_id", alternate = {"user_id"})
    private String patientId;

    @SerializedName("record_type")
    private String recordType = "prescription";

    @SerializedName("title")
    private String title;

    @SerializedName("doctor_name")
    private String doctorName;

    @SerializedName("clinic_name")
    private String clinicName;

    @SerializedName("record_date")
    private String recordDate;

    @SerializedName("file_path")
    private String filePath;

    @SerializedName("file_url")
    private String fileUrl;

    @SerializedName("notes")
    private String notes;

    @SerializedName("created_at")
    private String createdAt;

    // Doctor Prescription Specific Fields
    @SerializedName("diagnosis")
    private String diagnosis;

    @SerializedName("symptoms")
    private String symptoms;

    @SerializedName("medicines")
    private String medicines;

    @SerializedName("dosage")
    private String dosage;

    @SerializedName("duration")
    private String duration;

    @SerializedName("advice")
    private String advice;

    @SerializedName("follow_up_date")
    private String followUpDate;

    @SerializedName("is_doctor_generated")
    private boolean isDoctorGenerated = false;

    public MedicalRecord() {
    }

    public MedicalRecord(String id, String patientId, String recordType, String title, String doctorName, String clinicName, String recordDate, String filePath, String notes) {
        this.id = id;
        this.patientId = patientId;
        this.recordType = recordType;
        this.title = title;
        this.doctorName = doctorName;
        this.clinicName = clinicName;
        this.recordDate = recordDate;
        this.filePath = filePath;
        this.notes = notes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getRecordType() {
        return recordType != null ? recordType : "prescription";
    }
    public void setRecordType(String recordType) { this.recordType = recordType; }

    public String getTitle() {
        return title != null ? title : "Medical Record";
    }
    public void setTitle(String title) { this.title = title; }

    public String getDoctorName() {
        return doctorName != null ? doctorName : "Dr. Medical Specialist";
    }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getClinicName() {
        return clinicName != null ? clinicName : "Care Hospital";
    }
    public void setClinicName(String clinicName) { this.clinicName = clinicName; }

    public String getRecordDate() { return recordDate; }
    public void setRecordDate(String recordDate) { this.recordDate = recordDate; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }

    public String getMedicines() { return medicines; }
    public void setMedicines(String medicines) { this.medicines = medicines; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getAdvice() { return advice; }
    public void setAdvice(String advice) { this.advice = advice; }

    public String getFollowUpDate() { return followUpDate; }
    public void setFollowUpDate(String followUpDate) { this.followUpDate = followUpDate; }

    public boolean isDoctorGenerated() {
        return isDoctorGenerated || "doctor_prescription".equalsIgnoreCase(recordType);
    }
    public void setDoctorGenerated(boolean doctorGenerated) { isDoctorGenerated = doctorGenerated; }

    public String getUserFriendlyType() {
        if (isDoctorGenerated()) return "Doctor Prescription";
        String rt = getRecordType().toLowerCase();
        switch (rt) {
            case "prescription": return "Patient Prescription";
            case "lab_report": return "Lab Report";
            case "medical_report": return "Medical Report";
            case "scan": return "Scan & Imaging";
            default: return "Health Record";
        }
    }

    public boolean isPdf() {
        if (filePath != null && filePath.toLowerCase().endsWith(".pdf")) return true;
        if (fileUrl != null && fileUrl.toLowerCase().endsWith(".pdf")) return true;
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MedicalRecord record = (MedicalRecord) o;
        return Objects.equals(id, record.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
