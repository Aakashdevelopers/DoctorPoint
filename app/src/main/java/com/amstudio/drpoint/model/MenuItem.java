package com.amstudio.drpoint.model;

import java.util.Objects;

public class MenuItem {
    private String title;
    private String subtitle;
    private int iconRes;
    private int iconTintRes;
    private int bgTintRes;
    private int textColorRes;
    private String badgeText;

    public MenuItem(String title, int iconRes, int textColorRes) {
        this(title, null, iconRes, 0, 0, textColorRes, null);
    }

    public MenuItem(String title, int iconRes, int textColorRes, String badgeText) {
        this(title, null, iconRes, 0, 0, textColorRes, badgeText);
    }

    public MenuItem(String title, String subtitle, int iconRes, int iconTintRes, int bgTintRes, int textColorRes) {
        this(title, subtitle, iconRes, iconTintRes, bgTintRes, textColorRes, null);
    }

    public MenuItem(String title, String subtitle, int iconRes, int iconTintRes, int bgTintRes, int textColorRes, String badgeText) {
        this.title = title;
        this.subtitle = subtitle;
        this.iconRes = iconRes;
        this.iconTintRes = iconTintRes;
        this.bgTintRes = bgTintRes;
        this.textColorRes = textColorRes;
        this.badgeText = badgeText;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public int getIconRes() { return iconRes; }
    public void setIconRes(int iconRes) { this.iconRes = iconRes; }

    public int getIconTintRes() { return iconTintRes; }
    public void setIconTintRes(int iconTintRes) { this.iconTintRes = iconTintRes; }

    public int getBgTintRes() { return bgTintRes; }
    public void setBgTintRes(int bgTintRes) { this.bgTintRes = bgTintRes; }

    public int getTextColorRes() { return textColorRes; }
    public void setTextColorRes(int textColorRes) { this.textColorRes = textColorRes; }

    public String getBadgeText() { return badgeText; }
    public void setBadgeText(String badgeText) { this.badgeText = badgeText; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MenuItem menuItem = (MenuItem) o;
        return Objects.equals(title, menuItem.title)
                && Objects.equals(subtitle, menuItem.subtitle)
                && iconRes == menuItem.iconRes
                && iconTintRes == menuItem.iconTintRes
                && bgTintRes == menuItem.bgTintRes
                && textColorRes == menuItem.textColorRes
                && Objects.equals(badgeText, menuItem.badgeText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, subtitle, iconRes, iconTintRes, bgTintRes, textColorRes, badgeText);
    }
}
