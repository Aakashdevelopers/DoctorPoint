package com.amstudio.drpoint.network.model;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class User {
    @SerializedName("id")
    private String id;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("user_metadata")
    private Map<String, Object> userMetadata;

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        if (phone != null && !phone.isEmpty()) {
            return phone;
        }
        if (userMetadata != null && userMetadata.get("phone") != null) {
            return userMetadata.get("phone").toString();
        }
        return "";
    }

    public String getFullName() {
        if (userMetadata != null && userMetadata.get("full_name") != null) {
            return userMetadata.get("full_name").toString();
        }
        return "";
    }

    public Map<String, Object> getUserMetadata() {
        return userMetadata;
    }
}
