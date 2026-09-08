package com.amstudio.drpoint.model;

import java.io.Serializable;
import java.util.Objects;

public class Appointment implements Serializable {
    private String id;
    private String doctorId;
    private String doctorName;
    private String specialization;
    private String date;
    private String time;
    private String clinicName;
    private String location;
    private String status;
    private int fee;
    private int imageRes;

    public Appointment(String id, String doctorName, String specialization, String date, String time, String clinicName, String location, String status) {
        this(id, "doc_1", doctorName, specialization, date, time, clinicName, location, status, 900, 0);
    }

    public Appointment(String id, String doctorId, String doctorName, String specialization, String date, String time, String clinicName, String location, String status, int fee, int imageRes) {
        this.id = id;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.specialization = specialization;
        this.date = date;
        this.time = time;
        this.clinicName = clinicName;
        this.location = location;
        this.status = status;
        this.fee = fee;
        this.imageRes = imageRes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getClinicName() { return clinicName; }
    public void setClinicName(String clinicName) { this.clinicName = clinicName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getFee() { return fee; }
    public void setFee(int fee) { this.fee = fee; }

    public int getImageRes() { return imageRes; }
    public void setImageRes(int imageRes) { this.imageRes = imageRes; }

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
