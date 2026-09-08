package com.amstudio.drpoint.util;

import com.amstudio.drpoint.R;
import com.amstudio.drpoint.model.Appointment;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.model.MenuItem;
import com.amstudio.drpoint.model.QuickAccessItem;
import com.amstudio.drpoint.model.Speciality;
import com.amstudio.drpoint.network.SupabaseClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DummyDataProvider {

    private static List<Doctor> doctors;
    private static List<Appointment> appointments;

    public static synchronized List<QuickAccessItem> getQuickAccessItems() {
        List<QuickAccessItem> items = new ArrayList<>();
        items.add(new QuickAccessItem("Doctors", "By Specialty", R.drawable.ic_stethoscope, R.color.bg_blue_light));
        items.add(new QuickAccessItem("Test", "Lab & Imaging", R.drawable.ic_flask, R.color.bg_green_light));
        items.add(new QuickAccessItem("Medicine", "Home Delivery", R.drawable.ic_pill, R.color.bg_orange_light));
        items.add(new QuickAccessItem("Hospital", "Near You", R.drawable.ic_medical_cross, R.color.bg_purple_light));
        return items;
    }

    public static synchronized List<Speciality> getSpecialities() {
        List<Speciality> items = new ArrayList<>();
        items.add(new Speciality("General Physician", R.drawable.ic_stethoscope));
        items.add(new Speciality("Skin & Hair", R.drawable.ic_user));
        items.add(new Speciality("Women's Health", R.drawable.ic_heart));
        items.add(new Speciality("Dental Care", R.drawable.ic_medical_cross));
        items.add(new Speciality("Child Care", R.drawable.ic_user));
        items.add(new Speciality("ENT", R.drawable.ic_stethoscope));
        items.add(new Speciality("Mental Health", R.drawable.ic_user));
        items.add(new Speciality("Heart Care", R.drawable.ic_heart_filled));
        return items;
    }

    public interface SpecialitiesCallback {
        void onSpecialitiesLoaded(List<Speciality> specialities);
    }

    public static synchronized void fetchSpecialitiesFromSupabase(SpecialitiesCallback callback) {
        SupabaseClient.getDoctorService().getSpecialities().enqueue(new Callback<List<Speciality>>() {
            @Override
            public void onResponse(Call<List<Speciality>> call, Response<List<Speciality>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    if (callback != null) {
                        callback.onSpecialitiesLoaded(response.body());
                    }
                    return;
                }
                if (callback != null) {
                    callback.onSpecialitiesLoaded(getSpecialities());
                }
            }

            @Override
            public void onFailure(Call<List<Speciality>> call, Throwable t) {
                if (callback != null) {
                    callback.onSpecialitiesLoaded(getSpecialities());
                }
            }
        });
    }

    public static synchronized List<Doctor> getDoctors() {
        if (doctors == null) {
            doctors = new ArrayList<>();
            doctors.add(new Doctor(
                    "doc_1",
                    "Dr. Priya Sharma",
                    "MBBS, MD - Dermatology",
                    "12 Yrs Exp",
                    4.8,
                    120,
                    "Skin Care Clinic",
                    "Indiranagar, Bangalore",
                    900,
                    R.drawable.ic_user,
                    true,
                    "Female",
                    true,
                    true
            ));
            doctors.add(new Doctor(
                    "doc_2",
                    "Dr. Rajesh Kumar",
                    "MBBS, MS - Cardiology",
                    "15 Yrs Exp",
                    4.9,
                    210,
                    "Heart Care Centre",
                    "Koramangala, Bangalore",
                    1200,
                    R.drawable.ic_user,
                    true,
                    "Male",
                    true,
                    false
            ));
            doctors.add(new Doctor(
                    "doc_3",
                    "Dr. Ananya Rao",
                    "MBBS, DGO - Gynaecology",
                    "10 Yrs Exp",
                    4.7,
                    95,
                    "Motherhood Hospital",
                    "HSR Layout, Bangalore",
                    800,
                    R.drawable.ic_user,
                    true,
                    "Female",
                    false,
                    true
            ));
            doctors.add(new Doctor(
                    "doc_4",
                    "Dr. Vikram Malhotra",
                    "BDS, MDS - Orthodontics",
                    "8 Yrs Exp",
                    4.6,
                    85,
                    "Smile Dental Care",
                    "Whitefield, Bangalore",
                    700,
                    R.drawable.ic_user,
                    false,
                    "Male",
                    true,
                    true
            ));
            doctors.add(new Doctor(
                    "doc_5",
                    "Dr. Sunita Patel",
                    "MD - Pediatrics",
                    "14 Yrs Exp",
                    4.9,
                    180,
                    "Kids Health Clinic",
                    "Jayanagar, Bangalore",
                    1000,
                    R.drawable.ic_user,
                    true,
                    "Female",
                    true,
                    false
            ));
        }
        return new ArrayList<>(doctors);
    }

    public interface DoctorsCallback {
        void onDoctorsLoaded(List<Doctor> doctors);
    }

    public static synchronized void fetchDoctorsFromSupabase(DoctorsCallback callback) {
        SupabaseClient.getDoctorService().getDoctors().enqueue(new Callback<List<Doctor>>() {
            @Override
            public void onResponse(Call<List<Doctor>> call, Response<List<Doctor>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    synchronized (DummyDataProvider.class) {
                        doctors = new ArrayList<>(response.body());
                    }
                } else if (doctors == null) {
                    getDoctors();
                }
                if (callback != null) {
                    callback.onDoctorsLoaded(getDoctors());
                }
            }

            @Override
            public void onFailure(Call<List<Doctor>> call, Throwable t) {
                if (doctors == null) {
                    getDoctors();
                }
                if (callback != null) {
                    callback.onDoctorsLoaded(getDoctors());
                }
            }
        });
    }

    public static synchronized List<Doctor> getFilteredDoctors(String query, String category, String filterChip) {
        List<Doctor> all = getDoctors();
        List<Doctor> result = new ArrayList<>();

        for (Doctor d : all) {
            // Category check
            if (category != null && !category.isEmpty() && !"All Doctors".equalsIgnoreCase(category) && !"AI Recommended Doctors".equalsIgnoreCase(category)) {
                boolean matchesCategory = d.getQualification().toLowerCase().contains(category.toLowerCase()) ||
                        d.getSpecializationString().toLowerCase().contains(category.toLowerCase());
                if (!matchesCategory) {
                    continue;
                }
            }

            // Chip filter check
            if ("FEMALE".equalsIgnoreCase(filterChip) && !"Female".equalsIgnoreCase(d.getGender())) {
                continue;
            }
            if ("AVAILABLE".equalsIgnoreCase(filterChip) && !d.isAvailableToday()) {
                continue;
            }
            if ("NEARBY".equalsIgnoreCase(filterChip) && !d.isNearby()) {
                continue;
            }

            // Search Query check
            if (query != null && !query.trim().isEmpty()) {
                String q = query.trim().toLowerCase();
                boolean matchesQuery = d.getName().toLowerCase().contains(q) ||
                        d.getQualification().toLowerCase().contains(q) ||
                        d.getClinicName().toLowerCase().contains(q) ||
                        d.getLocation().toLowerCase().contains(q);
                if (!matchesQuery) {
                    continue;
                }
            }

            result.add(d);
        }
        return result;
    }

    public static synchronized List<Appointment> getAppointments() {
        if (appointments == null) {
            appointments = new ArrayList<>();
            appointments.add(new Appointment(
                    "appt_1",
                    "doc_1",
                    "Dr. Priya Sharma",
                    "Dermatologist",
                    "05 Sep",
                    "10:30 AM",
                    "Skin Care Clinic",
                    "Indiranagar, Bangalore",
                    "✓ Confirmed",
                    900,
                    R.drawable.ic_user
            ));
            appointments.add(new Appointment(
                    "appt_2",
                    "doc_2",
                    "Dr. Rajesh Kumar",
                    "Cardiologist",
                    "12 Sep",
                    "02:15 PM",
                    "Heart Care Centre",
                    "Koramangala, Bangalore",
                    "✓ Confirmed",
                    1200,
                    R.drawable.ic_user
            ));
            appointments.add(new Appointment(
                    "appt_3",
                    "doc_3",
                    "Dr. Ananya Rao",
                    "Gynaecologist",
                    "20 Aug",
                    "11:00 AM",
                    "Motherhood Hospital",
                    "HSR Layout, Bangalore",
                    "Completed",
                    800,
                    R.drawable.ic_user
            ));
        }
        return appointments;
    }

    public static synchronized void addAppointment(Appointment appt) {
        if (appointments == null) {
            getAppointments();
        }
        appointments.add(0, appt);
    }

    public static synchronized boolean cancelAppointment(String appointmentId) {
        if (appointments == null) return false;
        for (Appointment a : appointments) {
            if (a.getId().equals(appointmentId)) {
                a.setStatus("Cancelled");
                return true;
            }
        }
        return false;
    }

    public static synchronized List<MenuItem> getProfileMenuItems() {
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem("My Appointments", R.drawable.ic_appointments, R.color.text_primary));
        items.add(new MenuItem("Video Consultations", R.drawable.ic_chat, R.color.text_primary));
        items.add(new MenuItem("Test Bookings", R.drawable.ic_flask, R.color.text_primary));
        items.add(new MenuItem("Medicine Orders", R.drawable.ic_pill, R.color.text_primary));
        items.add(new MenuItem("Health Records", R.drawable.ic_file, R.color.text_primary));
        items.add(new MenuItem("My Doctors", R.drawable.ic_stethoscope, R.color.text_primary));
        items.add(new MenuItem("Payments & Wallet", R.drawable.ic_wallet, R.color.text_primary));
        items.add(new MenuItem("Help & Support", R.drawable.ic_help, R.color.text_primary));
        items.add(new MenuItem("Settings", R.drawable.ic_settings, R.color.text_primary));
        items.add(new MenuItem("Logout", R.drawable.ic_logout, R.color.error_red));
        return items;
    }

    public static List<Integer> getClinicPhotoResIds() {
        return Arrays.asList(
                R.drawable.ic_stethoscope,
                R.drawable.ic_medical_cross,
                R.drawable.ic_flask,
                R.drawable.ic_file
        );
    }
}
