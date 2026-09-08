package com.amstudio.drpoint.model;

import java.util.Objects;

public class MenuItem {
    private String title;
    private int iconRes;
    private int textColorRes;

    public MenuItem(String title, int iconRes, int textColorRes) {
        this.title = title;
        this.iconRes = iconRes;
        this.textColorRes = textColorRes;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getIconRes() { return iconRes; }
    public void setIconRes(int iconRes) { this.iconRes = iconRes; }

    public int getTextColorRes() { return textColorRes; }
    public void setTextColorRes(int textColorRes) { this.textColorRes = textColorRes; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MenuItem menuItem = (MenuItem) o;
        return Objects.equals(title, menuItem.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title);
    }
}
