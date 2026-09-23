package com.amstudio.drpoint.util;

import com.amstudio.drpoint.DoctorPointApp;
import com.amstudio.drpoint.R;
import com.amstudio.drpoint.model.Appointment;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.model.DoctorSlot;
import com.amstudio.drpoint.model.MenuItem;
import com.amstudio.drpoint.model.PatientUser;
import com.amstudio.drpoint.model.QuickAccessItem;
import com.amstudio.drpoint.model.Speciality;
import com.amstudio.drpoint.network.SupabaseClient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DummyDataProvider {

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

    public static boolean isDoctorMatchingCategory(Doctor d, String category) {
        if (category == null || category.trim().isEmpty() || "All Doctors".equalsIgnoreCase(category) || "AI Recommended Doctors".equalsIgnoreCase(category)) {
            return true;
        }
        String cat = category.toLowerCase().trim();
        String spec = (d.getSpecialization() != null ? d.getSpecialization() : "").toLowerCase();
        String qual = (d.getQualification() != null ? d.getQualification() : "").toLowerCase();
        String specStr = d.getSpecializationString().toLowerCase();

        String allDocText = spec + " " + qual + " " + specStr;

        if (allDocText.contains(cat)) {
            return true;
        }

        if (cat.contains("skin") || cat.contains("derma")) {
            return allDocText.contains("derma") || allDocText.contains("skin");
        }
        if (cat.contains("women") || cat.contains("gynaec") || cat.contains("maternity")) {
            return allDocText.contains("gynaec") || allDocText.contains("women") || allDocText.contains("obstetric");
        }
        if (cat.contains("child") || cat.contains("pediatr") || cat.contains("baby")) {
            return allDocText.contains("pediatr") || allDocText.contains("child") || allDocText.contains("baby");
        }
        if (cat.contains("heart") || cat.contains("cardio")) {
            return allDocText.contains("cardio") || allDocText.contains("heart");
        }
        if (cat.contains("eye") || cat.contains("optom") || cat.contains("ophthalm")) {
            return allDocText.contains("eye") || allDocText.contains("optom") || allDocText.contains("ophthalm");
        }
        if (cat.contains("ent") || cat.contains("ear") || cat.contains("nose") || cat.contains("throat")) {
            return allDocText.contains("ent") || allDocText.contains("ear") || allDocText.contains("nose") || allDocText.contains("throat") || allDocText.contains("otolaryng");
        }
        if (cat.contains("dent") || cat.contains("teeth")) {
            return allDocText.contains("dent") || allDocText.contains("teeth") || allDocText.contains("orthodont") || allDocText.contains("bds") || allDocText.contains("mds");
        }
        if (cat.contains("general") || cat.contains("physician") || cat.contains("fever")) {
            return allDocText.contains("general") || allDocText.contains("physician") || allDocText.contains("mbbs") || allDocText.contains("md");
        }

        return false;
    }

    public static synchronized List<Doctor> getFilteredDoctors(String query, String category, String filterChip) {
        List<Doctor> all = getDoctors();
        List<Doctor> result = new ArrayList<>();

        for (Doctor d : all) {
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

            // Search Query check (Doctor name, specialty, qualification, clinic, location)
            if (query != null && !query.trim().isEmpty()) {
                String q = query.trim().toLowerCase();
                boolean matchesQuery = d.getName().toLowerCase().contains(q) ||
                        d.getQualification().toLowerCase().contains(q) ||
                        d.getSpecializationString().toLowerCase().contains(q) ||
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
                    R.drawable.ic_user
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
                    R.drawable.ic_user
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
                    R.drawable.ic_user
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
                    R.drawable.ic_user
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

    public static synchronized List<MenuItem> getProfileMenuItems() {
        List<MenuItem> items = new ArrayList<>();
        items.add(new MenuItem("My Appointments", R.drawable.ic_appointments, R.color.text_primary));
        items.add(new MenuItem("Saved Doctors", R.drawable.ic_heart_filled, R.color.text_primary));
        items.add(new MenuItem("Apply for Doctor", R.drawable.ic_stethoscope, R.color.text_primary));
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

