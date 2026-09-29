package com.amstudio.drpoint.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class DoctorReview implements Serializable {

    @SerializedName("id")
    private String id;

    @SerializedName("appointment_id")
    private String appointmentId;

    @SerializedName("doctor_id")
    private String doctorId;

    @SerializedName("patient_id")
    private String patientId;

    @SerializedName("patient_name")
    private String patientName;

    @SerializedName("patient_avatar")
    private String patientAvatar;

    @SerializedName("rating")
    private double rating = 5.0;

    @SerializedName("review_text")
    private String reviewText;

    @SerializedName("created_at")
    private String createdAt;

    public DoctorReview() {
    }

    public DoctorReview(String appointmentId, String doctorId, String patientId, String patientName,
                        String patientAvatar, double rating, String reviewText) {
        this.appointmentId = appointmentId;
        this.doctorId = doctorId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.patientAvatar = patientAvatar;
        this.rating = rating;
        this.reviewText = reviewText;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientAvatar() { return patientAvatar; }
    public void setPatientAvatar(String patientAvatar) { this.patientAvatar = patientAvatar; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
