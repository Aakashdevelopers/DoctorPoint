package com.amstudio.drpoint.network.model;

import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;

public class SignUpRequest {
    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    @SerializedName("data")
    private Map<String, Object> data;

    public SignUpRequest(String email, String password, String fullName, String phone) {
        this.email = email;
        this.password = password;
        this.data = new HashMap<>();
        if (fullName != null && !fullName.isEmpty()) {
            this.data.put("full_name", fullName);
        }
        if (phone != null && !phone.isEmpty()) {
            this.data.put("phone", phone);
        }
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public Map<String, Object> getData() {
        return data;
    }
}
