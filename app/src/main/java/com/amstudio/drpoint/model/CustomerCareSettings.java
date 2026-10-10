package com.amstudio.drpoint.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class CustomerCareSettings implements Serializable {

    @SerializedName("id")
    private String id;

    @SerializedName("email")
    private String email;

    @SerializedName("phone_1")
    private String phone1;

    @SerializedName("phone_2")
    private String phone2;

    @SerializedName("updated_at")
    private String updatedAt;

    public CustomerCareSettings() {
    }

    public CustomerCareSettings(String email, String phone1, String phone2) {
        this.email = email;
        this.phone1 = phone1;
        this.phone2 = phone2;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone1() {
        return phone1;
    }

    public void setPhone1(String phone1) {
        this.phone1 = phone1;
    }

    public String getPhone2() {
        return phone2;
    }

    public void setPhone2(String phone2) {
        this.phone2 = phone2;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
