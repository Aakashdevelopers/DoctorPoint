package com.amstudio.drpoint.model;

import com.amstudio.drpoint.R;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class Speciality {
    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String name;

    private int iconRes = R.drawable.ic_stethoscope;

    @SerializedName("icon_url")
    private String iconUrl;

    public Speciality() {}

    public Speciality(String name, int iconRes) {
        this.name = name;
        this.iconRes = iconRes;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getIconRes() { return iconRes; }
    public void setIconRes(int iconRes) { this.iconRes = iconRes; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Speciality speciality = (Speciality) o;
        return Objects.equals(name, speciality.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
