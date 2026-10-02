package com.amstudio.drpoint.util;

import android.content.Context;

import com.amstudio.drpoint.DoctorPointApp;
import com.amstudio.drpoint.R;
import com.amstudio.drpoint.model.Appointment;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.model.DoctorReview;
import com.amstudio.drpoint.model.DoctorSlot;
import com.amstudio.drpoint.model.MenuItem;
import com.amstudio.drpoint.model.PatientUser;
import com.amstudio.drpoint.model.QuickAccessItem;
import com.amstudio.drpoint.model.Speciality;
import com.amstudio.drpoint.network.SupabaseClient;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DummyDataProvider {

    public static final String[] INDIAN_STATES = new String[]{
            "All States",
            "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh",
            "Goa", "Gujarat", "Haryana", "Himachal Pradesh", "Jharkhand",
            "Karnataka", "Kerala", "Madhya Pradesh", "Maharashtra", "Manipur",
            "Meghalaya", "Mizoram", "Nagaland", "Odisha", "Punjab",
            "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana", "Tripura",
            "Uttar Pradesh", "Uttarakhand", "West Bengal", "Delhi", "Chandigarh"
    };

    private static List<Doctor> doctors;
    private static List<Appointment> appointments;
    private static List<PatientUser> patientUsers;
    private static List<Speciality> specialitiesList;

    public static synchronized List<QuickAccessItem> getQuickAccessItems() {
        List<QuickAccessItem> items = new ArrayList<>();
        items.add(new QuickAccessItem("Doctors", "By Specialty", R.drawable.ic_stethoscope, R.color.bg_blue_light));
        items.add(new QuickAccessItem("Test", "Lab & Imaging", R.drawable.ic_flask, R.color.bg_green_light));
        items.add(new QuickAccessItem("Medicine", "Home Delivery", R.drawable.ic_pill, R.color.bg_orange_light));
        items.add(new QuickAccessItem("Hospital", "Near You", R.drawable.ic_medical_cross, R.color.bg_purple_light));
        return items;
    }

    public static synchronized List<Speciality> getSpecialities() {
        if (specialitiesList == null) {
            specialitiesList = new ArrayList<>();
            specialitiesList.add(new Speciality("General Physician", R.drawable.ic_stethoscope));
            specialitiesList.add(new Speciality("Women's Health", R.drawable.ic_heart));
            specialitiesList.add(new Speciality("Skin Specialist", R.drawable.ic_user));
            specialitiesList.add(new Speciality("Dentist", R.drawable.ic_medical_cross));
            specialitiesList.add(new Speciality("Eye Specialist", R.drawable.ic_stethoscope));
            specialitiesList.add(new Speciality("Ear, Nose & Throat", R.drawable.ic_stethoscope));
            specialitiesList.add(new Speciality("Child Care", R.drawable.ic_user));
            specialitiesList.add(new Speciality("Heart Care", R.drawable.ic_heart_filled));
        }
        return new ArrayList<>(specialitiesList);
    }

    public static synchronized void addSpeciality(Speciality speciality) {
        if (specialitiesList == null) {
            getSpecialities();
        }
        specialitiesList.add(speciality);
    }

    public static synchronized boolean deleteSpeciality(String title) {
        if (specialitiesList == null) getSpecialities();
        for (int i = 0; i < specialitiesList.size(); i++) {
            if (specialitiesList.get(i).getTitle().equalsIgnoreCase(title)) {
                specialitiesList.remove(i);
                return true;
            }
        }
        return false;
    }

    // Patient Users Management
    public static synchronized List<PatientUser> getPatientUsers() {
        if (patientUsers == null) {
            patientUsers = new ArrayList<>();
            patientUsers.add(new PatientUser("usr_1", "Aakash Mishra", "aakash.mishra@example.com", "+91 9876543210", "Male", 26, "O+", "Active"));
            patientUsers.add(new PatientUser("usr_2", "Rohan Sharma", "rohan.s@gmail.com", "+91 9123456789", "Male", 29, "B+", "Active"));
            patientUsers.add(new PatientUser("usr_3", "Priya Verma", "priya.verma@yahoo.com", "+91 9988776655", "Female", 24, "A+", "Active"));
            patientUsers.add(new PatientUser("usr_4", "Suresh Gupta", "suresh.g@gmail.com", "+91 9811223344", "Male", 45, "AB+", "Active"));
            patientUsers.add(new PatientUser("usr_5", "Kavita Rao", "kavita.rao@gmail.com", "+91 9766554433", "Female", 32, "O-", "Blocked"));
        }
        return new ArrayList<>(patientUsers);
    }

    public static synchronized void addPatientUser(PatientUser user) {
        if (patientUsers == null) getPatientUsers();
        if (user.getId() == null || user.getId().isEmpty()) {
            user.setId("usr_" + (patientUsers.size() + 1));
        }
        patientUsers.add(0, user);
    }

    public static synchronized boolean updatePatientUser(PatientUser user) {
        if (patientUsers == null) getPatientUsers();
        for (int i = 0; i < patientUsers.size(); i++) {
            if (patientUsers.get(i).getId().equals(user.getId())) {
                patientUsers.set(i, user);
                return true;
            }
        }
        return false;
    }

    public static synchronized boolean deletePatientUser(String userId) {
        if (patientUsers == null) getPatientUsers();
        for (int i = 0; i < patientUsers.size(); i++) {
            if (patientUsers.get(i).getId().equals(userId)) {
                patientUsers.remove(i);
                return true;
            }
        }
        return false;
    }

    public interface SpecialitiesCallback {
        void onSpecialitiesLoaded(List<Speciality> specialities);
    }

    public static synchronized void fetchSpecialitiesFromSupabase(SpecialitiesCallback callback) {
        SupabaseClient.getDoctorService().getSpecialities().enqueue(new Callback<List<Speciality>>() {
            @Override
            public void onResponse(Call<List<Speciality>> call, Response<List<Speciality>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<Speciality> validList = new ArrayList<>();
                    for (Speciality s : response.body()) {
                        if (s != null && s.getName() != null && !s.getName().trim().isEmpty() && !"Specialist".equalsIgnoreCase(s.getName().trim())) {
                            validList.add(s);
                        }
                    }
                    if (!validList.isEmpty()) {
                        if (callback != null) {
                            callback.onSpecialitiesLoaded(validList);
                        }
                        return;
                    }
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
            Doctor d1 = new Doctor(
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
            );
            d1.setDoctorPhone("+91 9876543210");
            d1.setReceptionPhone("+91 9876543211");
            doctors.add(d1);

            Doctor d2 = new Doctor(
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
            );
            d2.setDoctorPhone("+91 9123456789");
            d2.setReceptionPhone("+91 9123456790");
            doctors.add(d2);

            Doctor d3 = new Doctor(
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
            );
            d3.setDoctorPhone("+91 9988776655");
            d3.setReceptionPhone("+91 9988776656");
            doctors.add(d3);

            Doctor d4 = new Doctor(
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
            );
            d4.setDoctorPhone("+91 9811223344");
            d4.setReceptionPhone("+91 9811223345");
            doctors.add(d4);

            Doctor d5 = new Doctor(
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
            );
            d5.setDoctorPhone("+91 9766554433");
            d5.setReceptionPhone("+91 9766554434");
            doctors.add(d5);

            Doctor d6 = new Doctor(
                    "doc_6",
                    "Dr. Ramesh Verma",
                    "MBBS, MD - General Medicine",
                    "16 Yrs Exp",
                    4.9,
                    150,
                    "Patna Care Clinic",
                    "Boring Road, Patna, Bihar",
                    600,
                    R.drawable.ic_user,
                    true,
                    "Male",
                    true,
                    true
            );
            d6.setState("Bihar");
            d6.setDoctorPhone("+91 9835012345");
            doctors.add(d6);

            Doctor d7 = new Doctor(
                    "doc_7",
                    "Dr. Amit Shah",
                    "MBBS, MS - ENT",
                    "11 Yrs Exp",
                    4.7,
                    110,
                    "Delhi Healthcare Centre",
                    "Connaught Place, New Delhi",
                    850,
                    R.drawable.ic_user,
                    true,
                    "Male",
                    true,
                    true
            );
            d7.setState("Delhi");
            d7.setDoctorPhone("+91 9810012345");
            doctors.add(d7);

            Doctor d8 = new Doctor(
                    "doc_8",
                    "Dr. Neha Kapoor",
                    "MBBS, MD - Pediatrics",
                    "9 Yrs Exp",
                    4.8,
                    130,
                    "Mumbai Kids Clinic",
                    "Andheri West, Mumbai, Maharashtra",
                    1000,
                    R.drawable.ic_user,
                    true,
                    "Female",
                    true,
                    true
            );
            d8.setState("Maharashtra");
            d8.setDoctorPhone("+91 9820012345");
            doctors.add(d8);
        }
        return new ArrayList<>(doctors);
    }

    public static synchronized void addDoctor(Doctor doctor) {
        if (doctors == null) getDoctors();
        if (doctor.getId() == null || doctor.getId().isEmpty()) {
            doctor.setId("doc_" + (doctors.size() + 1));
        }
        doctors.add(0, doctor);
    }

    public static synchronized boolean updateDoctor(Doctor doctor) {
        if (doctors == null) getDoctors();
        for (int i = 0; i < doctors.size(); i++) {
            if (doctors.get(i).getId().equals(doctor.getId())) {
                doctors.set(i, doctor);
                return true;
            }
        }
        return false;
    }

    public static synchronized boolean deleteDoctor(String doctorId) {
        if (doctors == null) getDoctors();
        for (int i = 0; i < doctors.size(); i++) {
            if (doctors.get(i).getId().equals(doctorId)) {
                doctors.remove(i);
                return true;
            }
        }
        return false;
    }

    public static synchronized Doctor getDoctorById(String doctorId) {
        if (doctors == null) getDoctors();
        for (Doctor d : doctors) {
            if (d.getId().equals(doctorId)) {
                return d;
            }
        }
        return doctors.isEmpty() ? null : doctors.get(0);
    }

    public interface DoctorsCallback {
        void onDoctorsLoaded(List<Doctor> doctors);
    }

    public static synchronized void fetchDoctorsFromSupabase(DoctorsCallback callback) {
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        SupabaseClient.getDoctorService().getDoctors().enqueue(new Callback<List<Doctor>>() {
            @Override
            public void onResponse(Call<List<Doctor>> call, Response<List<Doctor>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<Doctor> fetchedDocs = response.body();

                    // Calculate average ratings and total review counts from doctor_reviews table
                    SupabaseClient.getDoctorService().getDoctorReviews(null).enqueue(new Callback<List<DoctorReview>>() {
                        @Override
                        public void onResponse(Call<List<DoctorReview>> revCall, Response<List<DoctorReview>> revResponse) {
                            if (revResponse.isSuccessful() && revResponse.body() != null) {
                                Map<String, List<DoctorReview>> reviewMap = new HashMap<>();
                                for (DoctorReview r : revResponse.body()) {
                                    if (r != null && r.getDoctorId() != null) {
                                        if (!reviewMap.containsKey(r.getDoctorId())) {
                                            reviewMap.put(r.getDoctorId(), new ArrayList<>());
                                        }
                                        List<DoctorReview> list = reviewMap.get(r.getDoctorId());
                                        if (list != null) {
                                            list.add(r);
                                        }
                                    }
                                }
                                for (Doctor d : fetchedDocs) {
                                    if (d != null && d.getId() != null) {
                                        List<DoctorReview> docReviews = reviewMap.get(d.getId());
                                        if (docReviews != null && !docReviews.isEmpty()) {
                                            double sum = 0;
                                            for (DoctorReview r : docReviews) {
                                                sum += r.getRating();
                                            }
                                            double rawAvg = sum / docReviews.size();
                                            double finalAvg = Math.round(rawAvg * 10.0) / 10.0;
                                            d.setRating(finalAvg);
                                            d.setReviewCount(docReviews.size());
                                        } else {
                                            d.setRating(0.0);
                                            d.setReviewCount(0);
                                        }
                                    }
                                }
                            }
                            fetchSlotsAndUpdateDoctors(fetchedDocs, todayDate, callback);
                        }

                        @Override
                        public void onFailure(Call<List<DoctorReview>> revCall, Throwable t) {
                            fetchSlotsAndUpdateDoctors(fetchedDocs, todayDate, callback);
                        }
                    });
                } else {
                    if (doctors == null) getDoctors();
                    if (callback != null) callback.onDoctorsLoaded(getDoctors());
                }
            }

            @Override
            public void onFailure(Call<List<Doctor>> call, Throwable t) {
                if (doctors == null) getDoctors();
                if (callback != null) callback.onDoctorsLoaded(getDoctors());
            }
        });
    }

    private static void fetchSlotsAndUpdateDoctors(List<Doctor> fetchedDocs, String todayDate, DoctorsCallback callback) {
        SupabaseClient.getSlotService().getAllFutureAvailableSlots("gte." + todayDate)
                .enqueue(new Callback<List<DoctorSlot>>() {
                    @Override
                    public void onResponse(Call<List<DoctorSlot>> c, Response<List<DoctorSlot>> r) {
                        Set<String> activeSlotDoctorIds = new HashSet<>();
                        if (r.isSuccessful() && r.body() != null) {
                            for (DoctorSlot slot : r.body()) {
                                if (slot.getDoctorId() != null) {
                                    activeSlotDoctorIds.add(slot.getDoctorId());
                                }
                            }
                        }
                        for (Doctor d : fetchedDocs) {
                            d.setAvailableToday(activeSlotDoctorIds.contains(d.getId()));
                        }
                        synchronized (DummyDataProvider.class) {
                            doctors = new ArrayList<>(fetchedDocs);
                        }
                        if (callback != null) callback.onDoctorsLoaded(getDoctors());
                    }

                    @Override
                    public void onFailure(Call<List<DoctorSlot>> c, Throwable t) {
                        synchronized (DummyDataProvider.class) {
                            doctors = new ArrayList<>(fetchedDocs);
                        }
                        if (callback != null) callback.onDoctorsLoaded(getDoctors());
                    }
                });
    }

    public static void recalculateAndUpdateDoctorRating(String doctorId) {
        if (doctorId == null || doctorId.trim().isEmpty()) return;

        SupabaseClient.getDoctorService().getDoctorReviews("eq." + doctorId)
                .enqueue(new Callback<List<DoctorReview>>() {
                    @Override
                    public void onResponse(Call<List<DoctorReview>> call, Response<List<DoctorReview>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            List<DoctorReview> reviews = response.body();
                            double sum = 0;
                            for (DoctorReview r : reviews) {
                                sum += r.getRating();
                            }
                            double rawAvg = sum / reviews.size();
                            final double finalAvgRating = Math.round(rawAvg * 10.0) / 10.0;
                            final int finalTotalCount = reviews.size();

                            synchronized (DummyDataProvider.class) {
                                if (doctors != null) {
                                    for (Doctor d : doctors) {
                                        if (d.getId().equals(doctorId)) {
                                            d.setRating(finalAvgRating);
                                            d.setReviewCount(finalTotalCount);
                                            break;
                                        }
                                    }
                                }
                            }

                            Map<String, Object> updateMap = new HashMap<>();
                            updateMap.put("rating", finalAvgRating);
                            updateMap.put("review_count", finalTotalCount);

                            SupabaseClient.getDoctorService().updateDoctorRating("eq." + doctorId, "return=minimal", updateMap)
                                    .enqueue(new Callback<Void>() {
                                        @Override
                                        public void onResponse(Call<Void> c, Response<Void> r) {
                                            if (!r.isSuccessful()) {
                                                // Fallback if review_count fails: try total_reviews or rating only
                                                Map<String, Object> fallbackMap = new HashMap<>();
                                                fallbackMap.put("rating", finalAvgRating);
                                                fallbackMap.put("total_reviews", finalTotalCount);
                                                SupabaseClient.getDoctorService().updateDoctorRating("eq." + doctorId, "return=minimal", fallbackMap)
                                                        .enqueue(new Callback<Void>() {
                                                            @Override
                                                            public void onResponse(Call<Void> c2, Response<Void> r2) {
                                                                if (!r2.isSuccessful()) {
                                                                    Map<String, Object> ratingOnlyMap = new HashMap<>();
                                                                    ratingOnlyMap.put("rating", finalAvgRating);
                                                                    SupabaseClient.getDoctorService().updateDoctorRating("eq." + doctorId, "return=minimal", ratingOnlyMap).enqueue(new Callback<Void>() {
                                                                        @Override
                                                                        public void onResponse(Call<Void> c3, Response<Void> r3) {}
                                                                        @Override
                                                                        public void onFailure(Call<Void> c3, Throwable t3) {}
                                                                    });
                                                                }
                                                            }
                                                            @Override
                                                            public void onFailure(Call<Void> c2, Throwable t2) {}
                                                        });
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<Void> c, Throwable t) {}
                                    });
                        }
                    }

                    @Override
                    public void onFailure(Call<List<DoctorReview>> call, Throwable t) {}
                });
    }

    public static boolean isDoctorMatchingCategory(Doctor d, String category) {
        if (d == null) return false;
        if (category == null || category.trim().isEmpty() || "All Doctors".equalsIgnoreCase(category.trim()) || "All".equalsIgnoreCase(category.trim()) || "AI Recommended Doctors".equalsIgnoreCase(category.trim())) {
            return true;
        }

        String cat = category.toLowerCase().trim();

        // 1. Available Today filter
        if (cat.contains("available today") || cat.equalsIgnoreCase("available")) {
            return d.isAvailableToday();
        }

        // 2. Top Rated / Top Doctors filter
        if (cat.contains("top rated") || cat.contains("top doctor") || cat.contains("popular")) {
            return d.getRating() > 2.0;
        }

        // 3. Saved Doctors filter
        if (cat.contains("saved") || cat.contains("favorite")) {
            return true;
        }

        // 4. Generic Specialist / Specialities page title filter (only when category name is literally "Specialist", "Specialities", or "Explore Specialities")
        if (cat.equalsIgnoreCase("specialist") || cat.equalsIgnoreCase("specialists") || cat.equalsIgnoreCase("specialities") || cat.equalsIgnoreCase("speciality") || cat.equalsIgnoreCase("explore specialities")) {
            String spec = (d.getSpecialization() != null ? d.getSpecialization() : "").trim();
            String qual = (d.getQualification() != null ? d.getQualification() : "").trim();
            String specStr = d.getSpecializationString().trim();
            return !spec.isEmpty() || !qual.isEmpty() || !specStr.isEmpty();
        }

        if (cat.equalsIgnoreCase("all doctors") || cat.equalsIgnoreCase("all") || cat.equalsIgnoreCase("see all")) {
            return true;
        }

        String spec = (d.getSpecialization() != null ? d.getSpecialization() : "").toLowerCase().trim();
        String qual = (d.getQualification() != null ? d.getQualification() : "").toLowerCase().trim();
        String specStr = d.getSpecializationString().toLowerCase().trim();

        String allDocText = (spec + " " + qual + " " + specStr).trim();

        if (allDocText.isEmpty()) {
            return false;
        }

        // Clean both category and doctor's specialization strings by removing common noise words
        String cleanCat = cat.replace("specialist", "").replace("specialities", "").replace("speciality", "").replace("doctor", "").replace("doctors", "").replace("care", "").trim();
        String cleanSpec = spec.replace("specialist", "").replace("specialities", "").replace("speciality", "").replace("doctor", "").replace("doctors", "").replace("care", "").trim();

        // Direct match on clean specialization
        if (!cleanSpec.isEmpty() && !cleanCat.isEmpty()) {
            if (cleanSpec.equalsIgnoreCase(cleanCat) ||
               (cleanCat.length() >= 3 && cleanSpec.contains(cleanCat)) ||
               (cleanSpec.length() >= 3 && cleanCat.contains(cleanSpec))) {
                return true;
            }
        }

        // Domain & Keyword specific exact matching
        if (cat.contains("skin") || cat.contains("derma") || cat.contains("cosmeto") || cat.contains("hair")) {
            return allDocText.contains("derma") || allDocText.contains("skin") || allDocText.contains("cosmeto") || allDocText.contains("hair");
        }
        if (cat.contains("women") || cat.contains("gynaec") || cat.contains("gynec") || cat.contains("maternity") || cat.contains("obstetric")) {
            return allDocText.contains("gynaec") || allDocText.contains("gynec") || allDocText.contains("women") || allDocText.contains("obstetric") || allDocText.contains("maternity");
        }
        if (cat.contains("child") || cat.contains("pediatr") || cat.contains("baby")) {
            return allDocText.contains("pediatr") || allDocText.contains("child") || allDocText.contains("baby");
        }
        if (cat.contains("heart") || cat.contains("cardio")) {
            return allDocText.contains("cardio") || allDocText.contains("heart");
        }
        if (cat.contains("eye") || cat.contains("optom") || cat.contains("ophthalm") || cat.contains("vision")) {
            return allDocText.contains("eye") || allDocText.contains("optom") || allDocText.contains("ophthalm") || allDocText.contains("vision");
        }
        if (cat.contains("ent") || cat.contains("ear") || cat.contains("nose") || cat.contains("throat") || cat.contains("otolaryng")) {
            return allDocText.contains("ent") || allDocText.contains("ear") || allDocText.contains("nose") || allDocText.contains("throat") || allDocText.contains("otolaryng");
        }
        if (cat.contains("dent") || cat.contains("teeth") || cat.contains("orthodont")) {
            return allDocText.contains("dent") || allDocText.contains("teeth") || allDocText.contains("orthodont") || allDocText.contains("bds") || allDocText.contains("mds");
        }
        if (cat.contains("ortho") || cat.contains("bone") || cat.contains("joint")) {
            return allDocText.contains("ortho") || allDocText.contains("bone") || allDocText.contains("joint");
        }
        if (cat.contains("neuro") || cat.contains("brain")) {
            return allDocText.contains("neuro") || allDocText.contains("brain");
        }
        if (cat.contains("psych") || cat.contains("mental") || cat.contains("mind")) {
            return allDocText.contains("psych") || allDocText.contains("mental") || allDocText.contains("mind");
        }
        if (cat.contains("gastro") || cat.contains("stomach") || cat.contains("digest")) {
            return allDocText.contains("gastro") || allDocText.contains("stomach") || allDocText.contains("digest");
        }
        if (cat.contains("pulmo") || cat.contains("chest") || cat.contains("lung")) {
            return allDocText.contains("pulmo") || allDocText.contains("chest") || allDocText.contains("lung");
        }
        if (cat.contains("nephro") || cat.contains("kidney")) {
            return allDocText.contains("nephro") || allDocText.contains("kidney");
        }
        if (cat.contains("uro")) {
            return allDocText.contains("uro");
        }
        if (cat.contains("onco") || cat.contains("cancer")) {
            return allDocText.contains("onco") || allDocText.contains("cancer");
        }
        if (cat.contains("general") || cat.contains("physician") || cat.contains("fever") || cat.contains("internal")) {
            boolean isOtherSpecialist = allDocText.contains("derma") || allDocText.contains("cardio") || allDocText.contains("gynaec")
                    || allDocText.contains("gynec") || allDocText.contains("pediatr") || allDocText.contains("dent")
                    || allDocText.contains("ortho") || allDocText.contains("neuro") || allDocText.contains("eye")
                    || allDocText.contains("ophthalm") || allDocText.contains("ent") || allDocText.contains("psych")
                    || allDocText.contains("gastro") || allDocText.contains("pulmo") || allDocText.contains("nephro")
                    || allDocText.contains("uro") || allDocText.contains("onco");
            return !isOtherSpecialist && (allDocText.contains("general") || allDocText.contains("physician") || allDocText.contains("internal") || allDocText.contains("fever") || allDocText.contains("mbbs"));
        }

        // Substring check on allDocText
        if (allDocText.contains(cat)) {
            return true;
        }

        return false;
    }

    public interface OnStateSelectedListener {
        void onStateSelected(String selectedState);
    }

    public static void showStatePickerDialog(Context context, OnStateSelectedListener listener) {
        String currentState = PreferenceManager.getInstance(context).getSelectedState();
        int defaultItem = 0;
        for (int i = 0; i < INDIAN_STATES.length; i++) {
            if (INDIAN_STATES[i].equalsIgnoreCase(currentState)) {
                defaultItem = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(context)
                .setTitle("Select State / Location")
                .setSingleChoiceItems(INDIAN_STATES, defaultItem, (dialog, which) -> {
                    String selected = INDIAN_STATES[which];
                    PreferenceManager.getInstance(context).setSelectedState(selected);
                    dialog.dismiss();
                    if (listener != null) {
                        listener.onStateSelected(selected);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public static boolean isDoctorInState(Doctor doctor, String targetState) {
        if (doctor == null) return false;
        if (targetState == null || targetState.trim().isEmpty() ||
                "All States".equalsIgnoreCase(targetState.trim()) ||
                "All Cities".equalsIgnoreCase(targetState.trim()) ||
                "All".equalsIgnoreCase(targetState.trim())) {
            return true;
        }

        String selStateClean = targetState.trim().toLowerCase();
        String docState = doctor.getState();
        String docLocation = doctor.getLocation();

        // 1. Direct match on doctor's state property
        if (docState != null && !docState.trim().isEmpty()) {
            String docStateClean = docState.trim().toLowerCase();
            if (docStateClean.equalsIgnoreCase(selStateClean) ||
                    docStateClean.contains(selStateClean) ||
                    selStateClean.contains(docStateClean)) {
                return true;
            }
        }

        // 2. Match on doctor's location string
        if (docLocation != null && !docLocation.trim().isEmpty()) {
            String docLocClean = docLocation.trim().toLowerCase();
            if (docLocClean.contains(selStateClean)) {
                return true;
            }

            // City to state mapping check
            if (selStateClean.contains("karnataka") && (docLocClean.contains("bangalore") || docLocClean.contains("bengaluru") || docLocClean.contains("mysore") || docLocClean.contains("mangalore"))) {
                return true;
            }
            if (selStateClean.contains("bihar") && (docLocClean.contains("patna") || docLocClean.contains("gaya") || docLocClean.contains("muzaffarpur") || docLocClean.contains("bhagalpur"))) {
                return true;
            }
            if (selStateClean.contains("maharashtra") && (docLocClean.contains("mumbai") || docLocClean.contains("pune") || docLocClean.contains("nagpur") || docLocClean.contains("thane"))) {
                return true;
            }
            if (selStateClean.contains("delhi") && (docLocClean.contains("delhi") || docLocClean.contains("new delhi") || docLocClean.contains("noida") || docLocClean.contains("gurugram") || docLocClean.contains("gurgaon"))) {
                return true;
            }
            if (selStateClean.contains("west bengal") && (docLocClean.contains("kolkata") || docLocClean.contains("howrah") || docLocClean.contains("siliguri"))) {
                return true;
            }
            if (selStateClean.contains("tamil nadu") && (docLocClean.contains("chennai") || docLocClean.contains("coimbatore") || docLocClean.contains("madurai"))) {
                return true;
            }
            if (selStateClean.contains("telangana") && (docLocClean.contains("hyderabad") || docLocClean.contains("warangal"))) {
                return true;
            }
            if (selStateClean.contains("rajasthan") && (docLocClean.contains("jaipur") || docLocClean.contains("jodhpur") || docLocClean.contains("udaipur"))) {
                return true;
            }
            if (selStateClean.contains("uttar pradesh") && (docLocClean.contains("lucknow") || docLocClean.contains("kanpur") || docLocClean.contains("varanasi") || docLocClean.contains("noida") || docLocClean.contains("agra"))) {
                return true;
            }
            if (selStateClean.contains("gujarat") && (docLocClean.contains("ahmedabad") || docLocClean.contains("surat") || docLocClean.contains("vadodara"))) {
                return true;
            }
            if (selStateClean.contains("punjab") && (docLocClean.contains("ludhiana") || docLocClean.contains("amritsar") || docLocClean.contains("jalandhar"))) {
                return true;
            }
            if (selStateClean.contains("kerala") && (docLocClean.contains("kochi") || docLocClean.contains("thiruvananthapuram") || docLocClean.contains("kozhikode"))) {
                return true;
            }
            if (selStateClean.contains("madhya pradesh") && (docLocClean.contains("indore") || docLocClean.contains("bhopal") || docLocClean.contains("gwalior"))) {
                return true;
            }
        }

        return false;
    }

    public static List<Doctor> filterDoctorsByState(List<Doctor> doctorList, String targetState) {
        if (doctorList == null) return new ArrayList<>();
        if (targetState == null || targetState.trim().isEmpty() ||
                "All States".equalsIgnoreCase(targetState.trim()) ||
                "All Cities".equalsIgnoreCase(targetState.trim()) ||
                "All".equalsIgnoreCase(targetState.trim())) {
            return new ArrayList<>(doctorList);
        }
        List<Doctor> filtered = new ArrayList<>();
        for (Doctor d : doctorList) {
            if (isDoctorInState(d, targetState)) {
                filtered.add(d);
            }
        }
        return filtered;
    }

    public static synchronized List<Doctor> getFilteredDoctors(String query, String category, String filterChip, String targetState) {
        List<Doctor> all = getDoctors();
        List<Doctor> result = new ArrayList<>();

        for (Doctor d : all) {
            // State check
            if (!isDoctorInState(d, targetState)) {
                continue;
            }

            // Category / Specialty / Saved check
            if ("Saved Doctors".equalsIgnoreCase(category) || "Saved".equalsIgnoreCase(category)) {
                DoctorPointApp app = DoctorPointApp.getInstance();
                if (app != null && !PreferenceManager.getInstance(app).isFavoriteDoctor(d.getId())) {
                    continue;
                }
            } else if (!isDoctorMatchingCategory(d, category)) {
                continue;
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

            // Search Query check (Doctor name, specialty, qualification, clinic, location, state)
            if (query != null && !query.trim().isEmpty()) {
                String q = query.trim().toLowerCase();
                boolean matchesQuery = d.getName().toLowerCase().contains(q) ||
                        d.getQualification().toLowerCase().contains(q) ||
                        d.getSpecializationString().toLowerCase().contains(q) ||
                        d.getClinicName().toLowerCase().contains(q) ||
                        d.getLocation().toLowerCase().contains(q) ||
                        d.getState().toLowerCase().contains(q);
                if (!matchesQuery) {
                    continue;
                }
            }

            result.add(d);
        }

        if (category != null) {
            String lowerCat = category.toLowerCase().trim();
            if (lowerCat.contains("top rated") || lowerCat.contains("top doctor") || lowerCat.contains("popular")) {
                Collections.sort(result, (a, b) -> {
                    int reviewCompare = Integer.compare(b.getReviewCount(), a.getReviewCount());
                    if (reviewCompare != 0) {
                        return reviewCompare;
                    }
                    return Double.compare(b.getRating(), a.getRating());
                });
            }
        }

        return result;
    }

    public static synchronized List<Doctor> getFilteredDoctors(String query, String category, String filterChip) {
        String targetState = "All States";
        DoctorPointApp app = DoctorPointApp.getInstance();
        if (app != null) {
            targetState = PreferenceManager.getInstance(app).getSelectedState();
        }
        return getFilteredDoctors(query, category, filterChip, targetState);
    }

    public static synchronized List<Appointment> getAppointments() {
        if (appointments == null) {
            appointments = new ArrayList<>();
            appointments.add(new Appointment(
                    "appt_1",
                    "usr_1",
                    "doc_1",
                    "Dr. Priya Sharma",
                    "Dermatologist",
                    "05 Sep",
                    "10:30 AM",
                    "Skin Care Clinic",
                    "Indiranagar, Bangalore",
                    "Confirmed",
                    900,
                    "Patient reported skin rashes.",
                    R.drawable.ic_user,
                    1
            ));
            appointments.add(new Appointment(
                    "appt_2",
                    "usr_2",
                    "doc_2",
                    "Dr. Rajesh Kumar",
                    "Cardiologist",
                    "12 Sep",
                    "02:15 PM",
                    "Heart Care Centre",
                    "Koramangala, Bangalore",
                    "Confirmed",
                    1200,
                    "Routine checkup.",
                    R.drawable.ic_user,
                    2
            ));
            appointments.add(new Appointment(
                    "appt_3",
                    "usr_3",
                    "doc_3",
                    "Dr. Ananya Rao",
                    "Gynaecologist",
                    "20 Aug",
                    "11:00 AM",
                    "Motherhood Hospital",
                    "HSR Layout, Bangalore",
                    "Completed",
                    800,
                    "Prescribed vitamins.",
                    R.drawable.ic_user,
                    1
            ));
            appointments.add(new Appointment(
                    "appt_4",
                    "usr_4",
                    "doc_1",
                    "Dr. Priya Sharma",
                    "Dermatologist",
                    "08 Sep",
                    "04:00 PM",
                    "Skin Care Clinic",
                    "Indiranagar, Bangalore",
                    "Pending",
                    900,
                    "Follow up consultation.",
                    R.drawable.ic_user,
                    2
            ));
        }
        return new ArrayList<>(appointments);
    }

    public static synchronized List<Appointment> getDoctorAppointments(String doctorId) {
        List<Appointment> all = getAppointments();
        List<Appointment> docAppts = new ArrayList<>();
        for (Appointment a : all) {
            if (a.getDoctorId() != null && a.getDoctorId().equalsIgnoreCase(doctorId)) {
                docAppts.add(a);
            }
        }
        if (docAppts.isEmpty()) {
            return all; // Fallback to show sample list
        }
        return docAppts;
    }

    public static synchronized void addAppointment(Appointment appt) {
        if (appointments == null) {
            getAppointments();
        }
        appointments.add(0, appt);
    }

    public static synchronized boolean updateAppointmentStatus(String appointmentId, String newStatus) {
        if (appointments == null) getAppointments();
        for (Appointment a : appointments) {
            if (a.getId().equals(appointmentId)) {
                a.setStatus(newStatus);
                return true;
            }
        }
        return false;
    }

    public static synchronized boolean updateAppointmentNotes(String appointmentId, String notes) {
        if (appointments == null) getAppointments();
        for (Appointment a : appointments) {
            if (a.getId().equals(appointmentId)) {
                a.setNotes(notes);
                return true;
            }
        }
        return false;
    }

    public static synchronized boolean cancelAppointment(String appointmentId) {
        return updateAppointmentStatus(appointmentId, "Cancelled");
    }

    public static synchronized boolean deleteAppointment(String appointmentId) {
        if (appointments == null) getAppointments();
        for (int i = 0; i < appointments.size(); i++) {
            if (appointments.get(i).getId().equals(appointmentId)) {
                appointments.remove(i);
                return true;
            }
        }
        return false;
    }

    public static synchronized List<MenuItem> getAccountMenuItems() {
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem("My Appointments", "View & manage booked appointments", R.drawable.ic_calendar, R.color.secondary, R.color.bg_blue_light, R.color.text_primary));
        items.add(new MenuItem("Saved Doctors", "Your favorite doctors & specialists", R.drawable.ic_heart_filled, R.color.error_red, R.color.bg_pink_light, R.color.text_primary));
        items.add(new MenuItem("Find Doctors & Specialists", "Explore top doctors near you", R.drawable.ic_search, R.color.accent, R.color.bg_teal_light, R.color.text_primary));
        items.add(new MenuItem("Notifications", "Reminders & health updates", R.drawable.ic_bell, R.color.warning_yellow, R.color.bg_yellow_light, R.color.text_primary));
        return items;
    }

    public static synchronized List<MenuItem> getHistoryMenuItems() {
        return getAccountMenuItems();
    }

    public static synchronized List<MenuItem> getHelpSupportMenuItems() {
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem("Join as a Doctor", "Register as a verified doctor on DoctorPoint", R.drawable.ic_stethoscope, R.color.primary, R.color.bg_teal_light, R.color.text_primary, "JOIN"));
        items.add(new MenuItem("Help & Support", "24/7 patient support center", R.drawable.ic_help, R.color.success_green, R.color.bg_green_light, R.color.text_primary));
        return items;
    }

    public static synchronized List<MenuItem> getMoreMenuItems() {
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem("Privacy Policy", "Data privacy & security guidelines", R.drawable.ic_privacy, R.color.secondary, R.color.bg_indigo_light, R.color.text_primary));
        items.add(new MenuItem("Terms & Conditions", "Terms of service & user agreement", R.drawable.ic_terms, R.color.text_secondary, R.color.bg_purple_light, R.color.text_primary));
        items.add(new MenuItem("Like us? Give us 5 stars", "Rate your experience on Play Store", R.drawable.ic_star, R.color.warning_yellow, R.color.bg_orange_light, R.color.text_primary));
        items.add(new MenuItem("Logout", "Sign out of your account", R.drawable.ic_logout, R.color.error_red, R.color.error_light, R.color.error_red));
        return items;
    }

    public static synchronized List<MenuItem> getProfileMenuItems() {
        List<MenuItem> items = new ArrayList<>();
        items.addAll(getAccountMenuItems());
        items.addAll(getHelpSupportMenuItems());
        items.addAll(getMoreMenuItems());
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

