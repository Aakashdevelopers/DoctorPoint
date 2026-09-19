package com.amstudio.drpoint.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Doctor implements Serializable {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("qualification")
    private String qualification;

    @SerializedName("specialization")
    private String specialization;

    @SerializedName("experience")
    private String experience;

    @SerializedName("rating")
    private double rating;

    @SerializedName(value = "review_count", alternate = {"total_reviews"})
    private int reviewCount;

    @SerializedName("clinic_name")
    private String clinicName;

    @SerializedName("location")
    private String location;

    @SerializedName("fee")
    private int fee;

    @SerializedName(value = "followup_fee", alternate = {"followUpFee", "returning_patient_fee"})
    private int followUpFee = 300;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName(value = "clinic_photos", alternate = {"clinic_photo_urls", "clinicPhotos"})
    private String clinicPhotos;

    @SerializedName("about")
    private String about;

    private int imageRes;

    @SerializedName(value = "is_verified", alternate = {"is_popular"})
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

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getSpecializationString() {
        if (specialization != null && !specialization.trim().isEmpty()) {
            return specialization;
        }
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

    public int getFee() { return fee > 0 ? fee : 500; }
    public void setFee(int fee) { this.fee = fee; }

    public int getFollowUpFee() { return followUpFee > 0 ? followUpFee : (fee > 0 ? fee : 300); }
    public void setFollowUpFee(int followUpFee) { this.followUpFee = followUpFee; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getAbout() { return about; }
    public void setAbout(String about) { this.about = about; }

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

    public String getClinicPhotos() { return clinicPhotos; }
    public void setClinicPhotos(String clinicPhotos) { this.clinicPhotos = clinicPhotos; }

    public List<Object> getClinicPhotosList() {
        List<Object> photos = new ArrayList<>();
        if (clinicPhotos != null && !clinicPhotos.trim().isEmpty()) {
            if (clinicPhotos.startsWith("[")) {
                try {
                    String clean = clinicPhotos.replace("[", "").replace("]", "").replace("\"", "").replace("'", "");
                    String[] urls = clean.split(",");
                    for (String u : urls) {
                        if (!u.trim().isEmpty()) {
                            photos.add(u.trim());
                        }
                    }
                } catch (Exception ignored) {}
            } else {
                String[] urls = clinicPhotos.split(",");
                for (String u : urls) {
                    if (!u.trim().isEmpty()) {
                        photos.add(u.trim());
                    }
                }
            }
        }
        if (photos.isEmpty()) {
            photos.add("https://images.unsplash.com/photo-1586773860418-d37222d8fce3?auto=format&fit=crop&q=80&w=600");
            photos.add("https://images.unsplash.com/photo-1629909613654-28e377c37b09?auto=format&fit=crop&q=80&w=600");
            photos.add("https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?auto=format&fit=crop&q=80&w=600");
        }
        return photos;
    }

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
