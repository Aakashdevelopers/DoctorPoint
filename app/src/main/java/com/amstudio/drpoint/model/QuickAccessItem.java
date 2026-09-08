package com.amstudio.drpoint.model;

import java.util.Objects;

public class QuickAccessItem {
    private String title;
    private String subtitle;
    private int iconRes;
    private int bgColorRes;

    public QuickAccessItem(String title, int iconRes, int bgColorRes) {
        this.title = title;
        this.subtitle = "";
        this.iconRes = iconRes;
        this.bgColorRes = bgColorRes;
    }

    public QuickAccessItem(String title, String subtitle, int iconRes, int bgColorRes) {
        this.title = title;
        this.subtitle = subtitle;
        this.iconRes = iconRes;
        this.bgColorRes = bgColorRes;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public int getIconRes() { return iconRes; }
    public void setIconRes(int iconRes) { this.iconRes = iconRes; }

    public int getBgColorRes() { return bgColorRes; }
    public void setBgColorRes(int bgColorRes) { this.bgColorRes = bgColorRes; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuickAccessItem item = (QuickAccessItem) o;
        return Objects.equals(title, item.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title);
    }
}
