package com.amstudio.drpoint.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Objects;

public class Doctor implements Serializable {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("qualification")
    private String qualification;

    @SerializedName("experience")
    private String experience;

    @SerializedName("rating")
    private double rating;

    @SerializedName("review_count")
    private int reviewCount;

    @SerializedName("clinic_name")
    private String clinicName;

    @SerializedName("location")
    private String location;

    @SerializedName("fee")
    private int fee;

    @SerializedName("image_url")
    private String imageUrl;

    private int imageRes;

    @SerializedName("is_verified")
    private boolean isVerified = true;

    @SerializedName("gender")
    private String gender = "Female";

    @SerializedName("is_available_today")
    private boolean isAvailableToday = true;

    @SerializedName("is_nearby")
    private boolean isNearby = true;

    public Doctor() {
    }

    public Doctor(String id, String name, String qualification, String experience, double rating,
                  int reviewCount, String clinicName, String location, int fee, int imageRes,
                  boolean isVerified) {
        this(id, name, qualification, experience, rating, reviewCount, clinicName, location, fee, imageRes, isVerified, "Female", true, true);
    }

    public Doctor(String id, String name, String qualification, String experience, double rating,
                  int reviewCount, String clinicName, String location, int fee, int imageRes,
                  boolean isVerified, String gender, boolean isAvailableToday, boolean isNearby) {
        this.id = id;
        this.name = name;
        this.qualification = qualification;
        this.experience = experience;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.clinicName = clinicName;
        this.location = location;
        this.fee = fee;
        this.imageRes = imageRes;
        this.isVerified = isVerified;
        this.gender = gender;
        this.isAvailableToday = isAvailableToday;
        this.isNearby = isNearby;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }

    public String getSpecializationString() {
        if (qualification == null) return "";
        if (qualification.contains("-")) {
            String[] parts = qualification.split("-");
            return parts[parts.length - 1].trim();
        }
        return qualification;
    }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public String getClinicName() { return clinicName; }
    public void setClinicName(String clinicName) { this.clinicName = clinicName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getFee() { return fee; }
    public void setFee(int fee) { this.fee = fee; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public int getImageRes() { return imageRes; }
    public void setImageRes(int imageRes) { this.imageRes = imageRes; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public boolean isAvailableToday() { return isAvailableToday; }
    public void setAvailableToday(boolean availableToday) { isAvailableToday = availableToday; }

    public boolean isNearby() { return isNearby; }
    public void setNearby(boolean nearby) { isNearby = nearby; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Doctor doctor = (Doctor) o;
        return Objects.equals(id, doctor.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
