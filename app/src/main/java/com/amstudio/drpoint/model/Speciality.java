package com.amstudio.drpoint.model;

import com.amstudio.drpoint.R;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class Speciality {

    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("name")
    private String name;

    @SerializedName("icon_url")
    private String iconUrl;

    @SerializedName("image_url")
    private String imageUrl;

    private int iconRes = R.drawable.ic_stethoscope;

    public Speciality() {}

    public Speciality(String name, int iconRes) {
        this.title = name;
        this.name = name;
        this.iconRes = iconRes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        if (title != null && !title.trim().isEmpty()) {
            return title.trim();
        }
        if (name != null && !name.trim().isEmpty()) {
            return name.trim();
        }
        return "Specialist";
    }

    public String getTitle() {
        return getName();
    }

    public void setName(String name) {
        this.name = name;
        this.title = name;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getIconRes() {
        return iconRes;
    }

    public void setIconRes(int iconRes) {
        this.iconRes = iconRes;
    }

    public String getIconUrl() {
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            return imageUrl.trim();
        }
        if (iconUrl != null && !iconUrl.trim().isEmpty()) {
            return iconUrl.trim();
        }
        return null;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getImageUrl() {
        return getIconUrl();
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Speciality speciality = (Speciality) o;
        return Objects.equals(getName(), speciality.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }
}
