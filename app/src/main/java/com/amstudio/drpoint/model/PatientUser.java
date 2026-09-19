package com.amstudio.drpoint.model;

import java.io.Serializable;

public class PatientUser implements Serializable {

    private String id;
    private String name;
    private String email;
    private String phone;
    private String gender;
    private int age;
    private String bloodGroup;
    private String status;

    public PatientUser() {
    }

    public PatientUser(String id, String name, String email, String phone, String gender, int age, String bloodGroup, String status) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.gender = gender;
        this.age = age;
        this.bloodGroup = bloodGroup;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
