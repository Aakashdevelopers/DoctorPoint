package com.amstudio.drpoint.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class PreferenceManager {

    private static final String PREF_NAME = "carewell_prefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_PHONE = "user_phone";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_FAVORITE_DOCTORS = "favorite_doctors";
    private static final String KEY_REMINDERS_ENABLED = "reminders_enabled";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_SELECTED_DOCTOR_ID = "selected_doctor_id";
    private static final String KEY_USER_AVATAR = "user_avatar";
    private static final String KEY_USER_GENDER = "user_gender";
    private static final String KEY_USER_AGE = "user_age";
    private static final String KEY_USER_BLOOD_GROUP = "user_blood_group";
    private static final String KEY_USER_ADDRESS = "user_address";
    private static final String KEY_USER_DOB = "user_dob";
    private static final String KEY_USER_EMERGENCY_CONTACT = "user_emergency_contact";
    private static final String KEY_SELECTED_STATE = "selected_state";

    private static PreferenceManager instance;
    private final SharedPreferences prefs;

    private PreferenceManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized PreferenceManager getInstance(Context context) {
        if (instance == null) {
            instance = new PreferenceManager(context);
        }
        return instance;
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void setLoggedIn(boolean loggedIn) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn).apply();
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "Aakash Mishra");
    }

    public void setUserName(String name) {
        prefs.edit().putString(KEY_USER_NAME, name).apply();
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "aakash.mishra@example.com");
    }

    public void setUserEmail(String email) {
        prefs.edit().putString(KEY_USER_EMAIL, email).apply();
    }

    public String getUserPhone() {
        return prefs.getString(KEY_USER_PHONE, "");
    }

    public void setUserPhone(String phone) {
        prefs.edit().putString(KEY_USER_PHONE, phone).apply();
    }

    public String getUserAvatar() {
        return prefs.getString(KEY_USER_AVATAR, "");
    }

    public void setUserAvatar(String avatarUrl) {
        prefs.edit().putString(KEY_USER_AVATAR, avatarUrl).apply();
    }

    public String getUserGender() {
        return prefs.getString(KEY_USER_GENDER, "");
    }

    public void setUserGender(String gender) {
        prefs.edit().putString(KEY_USER_GENDER, gender).apply();
    }

    public int getUserAge() {
        return prefs.getInt(KEY_USER_AGE, 0);
    }

    public void setUserAge(int age) {
        prefs.edit().putInt(KEY_USER_AGE, age).apply();
    }

    public String getUserBloodGroup() {
        return prefs.getString(KEY_USER_BLOOD_GROUP, "");
    }

    public void setUserBloodGroup(String bloodGroup) {
        prefs.edit().putString(KEY_USER_BLOOD_GROUP, bloodGroup).apply();
    }

    public String getUserAddress() {
        return prefs.getString(KEY_USER_ADDRESS, "");
    }

    public void setUserAddress(String address) {
        prefs.edit().putString(KEY_USER_ADDRESS, address).apply();
    }

    public String getSelectedState() {
        String selected = prefs.getString(KEY_SELECTED_STATE, "");
        if (selected != null && !selected.trim().isEmpty()) {
            return selected.trim();
        }
        String userAddr = getUserAddress();
        if (userAddr != null && !userAddr.trim().isEmpty()) {
            return userAddr.trim();
        }
        return "Karnataka";
    }

    public boolean isStateManuallySelected() {
        String selected = prefs.getString(KEY_SELECTED_STATE, "");
        return selected != null && !selected.trim().isEmpty();
    }

    public void setSelectedState(String state) {
        prefs.edit().putString(KEY_SELECTED_STATE, state != null ? state.trim() : "").apply();
    }

    public String getUserDob() {
        return prefs.getString(KEY_USER_DOB, "");
    }

    public void setUserDob(String dob) {
        prefs.edit().putString(KEY_USER_DOB, dob).apply();
    }

    public String getUserEmergencyContact() {
        return prefs.getString(KEY_USER_EMERGENCY_CONTACT, "");
    }

    public void setUserEmergencyContact(String emergencyContact) {
        prefs.edit().putString(KEY_USER_EMERGENCY_CONTACT, emergencyContact).apply();
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, "");
    }

    public void setUserId(String userId) {
        prefs.edit().putString(KEY_USER_ID, userId).apply();
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, "");
    }

    public void setAccessToken(String token) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply();
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, "");
    }

    public void setRefreshToken(String token) {
        prefs.edit().putString(KEY_REFRESH_TOKEN, token).apply();
    }

    public Set<String> getFavoriteDoctorIds() {
        return new HashSet<>(prefs.getStringSet(KEY_FAVORITE_DOCTORS, new HashSet<>()));
    }

    public boolean isFavoriteDoctor(String doctorId) {
        if (doctorId == null) return false;
        Set<String> favorites = prefs.getStringSet(KEY_FAVORITE_DOCTORS, new HashSet<>());
        return favorites.contains(doctorId);
    }

    public boolean toggleFavoriteDoctor(String doctorId) {
        if (doctorId == null) return false;
        Set<String> favorites = new HashSet<>(prefs.getStringSet(KEY_FAVORITE_DOCTORS, new HashSet<>()));
        boolean isNowFav;
        if (favorites.contains(doctorId)) {
            favorites.remove(doctorId);
            isNowFav = false;
        } else {
            favorites.add(doctorId);
            isNowFav = true;
        }
        prefs.edit().putStringSet(KEY_FAVORITE_DOCTORS, favorites).apply();
        return isNowFav;
    }

    public boolean getRemindersEnabled() {
        return prefs.getBoolean(KEY_REMINDERS_ENABLED, true);
    }

    public void setRemindersEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_REMINDERS_ENABLED, enabled).apply();
    }

    public String getUserRole() {
        return prefs.getString(KEY_USER_ROLE, "USER");
    }

    public void setUserRole(String role) {
        prefs.edit().putString(KEY_USER_ROLE, role).apply();
    }

    public String getSelectedDoctorId() {
        return prefs.getString(KEY_SELECTED_DOCTOR_ID, "doc_1");
    }

    public void setSelectedDoctorId(String doctorId) {
        prefs.edit().putString(KEY_SELECTED_DOCTOR_ID, doctorId).apply();
    }

    public void recordDoctorBooking(String doctorId) {
        if (doctorId == null || doctorId.trim().isEmpty()) return;
        prefs.edit().putBoolean("booked_doc_" + doctorId, true).apply();
    }

    public boolean hasBookedWithDoctor(String doctorId) {
        if (doctorId == null || doctorId.trim().isEmpty()) return false;
        return prefs.getBoolean("booked_doc_" + doctorId, false);
    }

    public void clearSession() {
        prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_USER_ID)
                .remove(KEY_USER_NAME)
                .remove(KEY_USER_EMAIL)
                .remove(KEY_USER_PHONE)
                .remove(KEY_USER_AVATAR)
                .remove(KEY_USER_GENDER)
                .remove(KEY_USER_AGE)
                .remove(KEY_USER_BLOOD_GROUP)
                .remove(KEY_USER_ADDRESS)
                .remove(KEY_USER_DOB)
                .remove(KEY_USER_EMERGENCY_CONTACT)
                .apply();
    }
}
