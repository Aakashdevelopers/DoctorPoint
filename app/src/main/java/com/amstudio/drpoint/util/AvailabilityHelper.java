package com.amstudio.drpoint.util;

import com.amstudio.drpoint.adapter.DateChipAdapter;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.model.DoctorSlot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AvailabilityHelper {

    // Configurable Availability Window (Default: 2 days in advance)
    public static final int DEFAULT_AVAILABILITY_WINDOW_DAYS = 2;

    public static boolean isDateInPast(String slotDateStr) {
        if (slotDateStr == null || slotDateStr.trim().isEmpty()) return true;
        try {
            SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String clean = slotDateStr.trim();
            if (clean.contains("T")) clean = clean.substring(0, clean.indexOf("T"));
            if (clean.contains(" ")) clean = clean.substring(0, clean.indexOf(" "));
            Date slotDate = sdfDate.parse(clean);
            if (slotDate == null) return true;

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            return slotDate.before(today.getTime());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Reusable check to ensure slot is in 'available' status and is not in the past.
     */
    public static boolean isSlotInPast(DoctorSlot slot) {
        if (slot == null || slot.getSlotDate() == null || slot.getSlotDate().trim().isEmpty()) {
            return true;
        }
        return isDateInPast(slot.getSlotDate());
    }

    public static boolean isSlotValidAndBookable(DoctorSlot slot) {
        if (slot == null) return false;
        if (!"available".equalsIgnoreCase(slot.getStatus())) return false;
        return !isSlotInPast(slot);
    }

    /**
     * Check if slot falls within a configurable day visibility window.
     */
    public static boolean isSlotInVisibilityWindow(DoctorSlot slot, int windowDays) {
        if (!isSlotValidAndBookable(slot)) {
            return false;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date slotDate = sdf.parse(slot.getSlotDate());
            if (slotDate == null) return false;

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            Calendar windowEnd = Calendar.getInstance();
            windowEnd.setTime(today.getTime());
            windowEnd.add(Calendar.DAY_OF_YEAR, windowDays);

            return !slotDate.before(today.getTime()) && !slotDate.after(windowEnd.getTime());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Filter doctors who have at least 1 bookable slot in the specified visibility window.
     */
    public static List<Doctor> filterAvailableDoctorsInWindow(List<Doctor> doctors, List<DoctorSlot> slots, int windowDays) {
        List<Doctor> available = new ArrayList<>();
        Map<String, Boolean> doctorAvailabilityMap = new HashMap<>();

        if (slots != null) {
            for (DoctorSlot slot : slots) {
                if (isSlotInVisibilityWindow(slot, windowDays)) {
                    doctorAvailabilityMap.put(slot.getDoctorId(), true);
                }
            }
        }

        for (Doctor doctor : doctors) {
            if (doctor != null && doctor.isVerified()) {
                if (slots == null || slots.isEmpty()) {
                    if (doctor.isAvailableToday()) {
                        available.add(doctor);
                    }
                } else if (Boolean.TRUE.equals(doctorAvailabilityMap.get(doctor.getId()))) {
                    available.add(doctor);
                }
            }
        }

        return available;
    }

    /**
     * Group slots by slot_date for a specific clinic and return formatted DateChip DateItems.
     * Only dates with at least 1 bookable slot are included.
     */
    public static List<DateChipAdapter.DateItem> extractAvailableDateChips(List<DoctorSlot> slots, String selectedClinicId) {
        if (slots == null || slots.isEmpty()) return new ArrayList<>();

        List<String> rawDates = new ArrayList<>();
        SimpleDateFormat inputSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat daySdf = new SimpleDateFormat("EEE", Locale.getDefault());
        SimpleDateFormat dateSdf = new SimpleDateFormat("dd MMM", Locale.getDefault());
        SimpleDateFormat todaySdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        String todayStr = todaySdf.format(new Date());

        for (DoctorSlot slot : slots) {
            if (slot == null || slot.getSlotDate() == null) continue;
            if (isDateInPast(slot.getSlotDate())) continue;

            if (!rawDates.contains(slot.getSlotDate())) {
                rawDates.add(slot.getSlotDate());
            }
        }

        Collections.sort(rawDates);

        List<DateChipAdapter.DateItem> dateItems = new ArrayList<>();
        for (String rawDate : rawDates) {
            try {
                Date d = inputSdf.parse(rawDate);
                if (d == null) continue;

                String dayName;
                if (rawDate.equals(todayStr)) {
                    dayName = "Today";
                } else {
                    dayName = daySdf.format(d);
                }
                String dateVal = dateSdf.format(d);

                dateItems.add(new DateChipAdapter.DateItem(dayName, dateVal, rawDate));
            } catch (Exception e) {
                dateItems.add(new DateChipAdapter.DateItem("Day", rawDate, rawDate));
            }
        }

        return dateItems;
    }

    /**
     * Filter available slots by date, sorted by start_time ascending.
     */
    public static List<DoctorSlot> filterAndSortSlots(List<DoctorSlot> slots, String selectedClinicId, String selectedDateRaw) {
        if (slots == null || slots.isEmpty()) return new ArrayList<>();

        List<DoctorSlot> filtered = new ArrayList<>();
        String targetDate = selectedDateRaw != null ? selectedDateRaw.trim() : null;
        if (targetDate != null && targetDate.contains("T")) targetDate = targetDate.substring(0, targetDate.indexOf("T"));
        if (targetDate != null && targetDate.contains(" ")) targetDate = targetDate.substring(0, targetDate.indexOf(" "));

        for (DoctorSlot slot : slots) {
            if (isDateInPast(slot.getSlotDate())) continue;

            // Date filtering
            if (targetDate != null && !targetDate.isEmpty()) {
                String slotDate = slot.getSlotDate();
                if (slotDate != null && !targetDate.equalsIgnoreCase(slotDate)) {
                    continue;
                }
            }

            if (isSlotInPast(slot)) continue;

            filtered.add(slot);
        }

        // Sort by start_time ascending
        Collections.sort(filtered, (s1, s2) -> {
            if (s1.getStartTime() == null) return -1;
            if (s2.getStartTime() == null) return 1;
            return s1.getStartTime().compareTo(s2.getStartTime());
        });

        return filtered;
    }

    public static List<String> getUniqueAvailableDates(List<DoctorSlot> slots) {
        if (slots == null || slots.isEmpty()) return new ArrayList<>();

        List<String> dates = new ArrayList<>();
        for (DoctorSlot slot : slots) {
            if (!isSlotInPast(slot) && slot.getSlotDate() != null) {
                if (!dates.contains(slot.getSlotDate())) {
                    dates.add(slot.getSlotDate());
                }
            }
        }
        Collections.sort(dates);
        return dates;
    }

    public static String formatSlotTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return "";
        try {
            SimpleDateFormat sdfIn = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat sdfOut = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date date = sdfIn.parse(timeStr);
            return date != null ? sdfOut.format(date) : timeStr;
        } catch (Exception e) {
            return timeStr;
        }
    }
}
