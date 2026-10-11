package com.amstudio.drpoint.util;

import com.amstudio.drpoint.adapter.DateChipAdapter;
import com.amstudio.drpoint.model.Appointment;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.model.DoctorSlot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class AvailabilityHelper {

    // Configurable Availability Window (Default: 2 days in advance)
    public static final int DEFAULT_AVAILABILITY_WINDOW_DAYS = 2;
    public static final String TIME_ZONE_ID = "Asia/Kolkata";

    private static long serverTimeOffsetMillis = 0L;

    public static TimeZone getAppTimeZone() {
        return TimeZone.getTimeZone(TIME_ZONE_ID);
    }

    public static Calendar getCalendarInstance() {
        return Calendar.getInstance(getAppTimeZone(), Locale.US);
    }

    public static synchronized void updateServerTimeOffset(long serverTimestampMillis) {
        if (serverTimestampMillis > 0) {
            serverTimeOffsetMillis = serverTimestampMillis - System.currentTimeMillis();
        }
    }

    public static long getCalibratedCurrentTimeMillis() {
        return System.currentTimeMillis() + serverTimeOffsetMillis;
    }

    public static String normalizeTimeFormat(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return "00:00:00";
        String clean = timeStr.trim().toUpperCase(Locale.US);

        if (clean.contains("+")) clean = clean.substring(0, clean.indexOf("+")).trim();
        if (clean.contains(".")) clean = clean.substring(0, clean.indexOf(".")).trim();

        boolean isPm = clean.contains("PM");
        boolean isAm = clean.contains("AM");

        clean = clean.replace("AM", "").replace("PM", "").trim();

        String[] parts = clean.split(":");
        if (parts.length >= 1) {
            try {
                int h = Integer.parseInt(parts[0].trim());
                int m = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
                int s = parts.length > 2 ? Integer.parseInt(parts[2].trim()) : 0;

                if (isPm && h < 12) {
                    h += 12;
                } else if (isAm && h == 12) {
                    h = 0;
                }

                return String.format(Locale.US, "%02d:%02d:%02d", h, m, s);
            } catch (Exception ignored) {}
        }
        return timeStr.trim();
    }

    public static String cleanDateString(String rawDateStr) {
        if (rawDateStr == null || rawDateStr.trim().isEmpty()) return "";
        String clean = rawDateStr.trim();
        if (clean.contains("T")) clean = clean.substring(0, clean.indexOf("T")).trim();
        if (clean.contains(" ")) clean = clean.substring(0, clean.indexOf(" ")).trim();
        return clean;
    }

    public static boolean isDateInPast(String slotDateStr) {
        if (slotDateStr == null || slotDateStr.trim().isEmpty()) return true;
        try {
            String clean = cleanDateString(slotDateStr);
            SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            sdfDate.setTimeZone(getAppTimeZone());
            Date slotDate = sdfDate.parse(clean);
            if (slotDate == null) return true;

            Calendar today = getCalendarInstance();
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
     * Checks if a slot's exact start time (Date + Start Time in Asia/Kolkata timezone)
     * has been reached or passed.
     * Example: 18 September 09:00 AM slot.
     * At 08:59 AM -> false (AVAILABLE)
     * At 09:00 AM -> true (LOCKED)
     */
    public static boolean isSlotTimeReachedOrPassed(String slotDateStr, String startTimeStr) {
        if (slotDateStr == null || slotDateStr.trim().isEmpty()) return true;
        if (startTimeStr == null || startTimeStr.trim().isEmpty()) return isDateInPast(slotDateStr);

        try {
            String cleanDate = cleanDateString(slotDateStr);
            String normTime = normalizeTimeFormat(startTimeStr);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            sdf.setTimeZone(getAppTimeZone());

            Date slotStartDateTime = sdf.parse(cleanDate + " " + normTime);
            if (slotStartDateTime == null) return isDateInPast(cleanDate);

            long currentTime = getCalibratedCurrentTimeMillis();
            return currentTime >= slotStartDateTime.getTime();
        } catch (Exception e) {
            return isDateInPast(slotDateStr);
        }
    }

    public static boolean isSlotTimeReachedOrPassed(DoctorSlot slot) {
        if (slot == null || slot.getSlotDate() == null || slot.getSlotDate().trim().isEmpty()) {
            return true;
        }
        return isSlotTimeReachedOrPassed(slot.getSlotDate(), slot.getStartTime());
    }

    /**
     * Checks if a slot's duration has completed (Date + Start Time + Slot Duration).
     * If duration is 20 minutes, 09:00 AM slot ends at 09:20 AM.
     */
    public static boolean isSlotEndPassed(String slotDateStr, String startTimeStr, String endTimeStr, int durationMinutes) {
        if (slotDateStr == null || slotDateStr.trim().isEmpty()) return true;
        try {
            String cleanDate = cleanDateString(slotDateStr);

            if (endTimeStr != null && !endTimeStr.trim().isEmpty()) {
                String normEndTime = normalizeTimeFormat(endTimeStr);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                sdf.setTimeZone(getAppTimeZone());
                Date slotEndDateTime = sdf.parse(cleanDate + " " + normEndTime);
                if (slotEndDateTime != null) {
                    return getCalibratedCurrentTimeMillis() >= slotEndDateTime.getTime();
                }
            }

            String normStartTime = normalizeTimeFormat(startTimeStr);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            sdf.setTimeZone(getAppTimeZone());
            Date slotStartDateTime = sdf.parse(cleanDate + " " + normStartTime);
            if (slotStartDateTime == null) return isDateInPast(cleanDate);

            int minutesToAdd = durationMinutes > 0 ? durationMinutes : 20;
            long endMillis = slotStartDateTime.getTime() + (minutesToAdd * 60L * 1000L);

            return getCalibratedCurrentTimeMillis() >= endMillis;
        } catch (Exception e) {
            return isDateInPast(slotDateStr);
        }
    }

    public static boolean isSlotEndPassed(DoctorSlot slot) {
        if (slot == null || slot.getSlotDate() == null) return true;
        int duration = slot.getSlotDuration() > 0 ? slot.getSlotDuration() : (slot.getConsultDuration() > 0 ? slot.getConsultDuration() : 20);
        return isSlotEndPassed(slot.getSlotDate(), slot.getStartTime(), slot.getEndTime(), duration);
    }

    /**
     * Reusable check to ensure slot is not in the past (both Date and Time).
     * Returns true if currentTime >= slotStartDateTime or date is in past.
     */
    public static boolean isSlotInPast(DoctorSlot slot) {
        return isSlotTimeReachedOrPassed(slot);
    }

    public static boolean isSlotValidAndBookable(DoctorSlot slot) {
        if (slot == null) return false;
        if (!"available".equalsIgnoreCase(slot.getStatus())) return false;
        return !isSlotTimeReachedOrPassed(slot);
    }

    /**
     * Checks if an uncompleted appointment's scheduled slot end time has passed.
     * Applies to uncompleted statuses (pending, confirmed, checked_in, waiting, in_consultation, etc.).
     * Returns false if status is completed, cancelled, rejected, or no_show.
     */
    public static boolean isAppointmentSchedulePassed(Appointment appointment, int defaultSlotDurationMins) {
        if (appointment == null) return false;
        String st = appointment.getStatus();
        if (st == null) return false;
        String lowerSt = st.trim().toLowerCase(Locale.US);

        if ("completed".equals(lowerSt) || "cancelled".equals(lowerSt) ||
            "rejected".equals(lowerSt) || "no_show".equals(lowerSt)) {
            return false;
        }

        String apptDate = appointment.getAppointmentDate();
        String startTime = appointment.getStartTime();
        String endTime = appointment.getEndTime();

        if (apptDate == null || apptDate.trim().isEmpty()) return false;

        return isSlotEndPassed(apptDate, startTime, endTime, defaultSlotDurationMins > 0 ? defaultSlotDurationMins : 20);
    }

    /**
     * Checks if a pending/unconfirmed appointment has passed its appointment schedule window.
     */
    public static boolean isPendingAppointmentExpired(Appointment appointment, int defaultSlotDurationMins) {
        return isAppointmentSchedulePassed(appointment, defaultSlotDurationMins);
    }

    /**
     * Check if slot falls within a configurable day visibility window.
     */
    public static boolean isSlotInVisibilityWindow(DoctorSlot slot, int windowDays) {
        if (!isSlotValidAndBookable(slot)) {
            return false;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            sdf.setTimeZone(getAppTimeZone());
            Date slotDate = sdf.parse(cleanDateString(slot.getSlotDate()));
            if (slotDate == null) return false;

            Calendar today = getCalendarInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            Calendar windowEnd = (Calendar) today.clone();
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
     */
    public static List<DateChipAdapter.DateItem> extractAvailableDateChips(List<DoctorSlot> slots, String selectedClinicId) {
        if (slots == null || slots.isEmpty()) return new ArrayList<>();

        List<String> rawDates = new ArrayList<>();
        SimpleDateFormat inputSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        inputSdf.setTimeZone(getAppTimeZone());

        SimpleDateFormat daySdf = new SimpleDateFormat("EEE", Locale.US);
        daySdf.setTimeZone(getAppTimeZone());

        SimpleDateFormat dateSdf = new SimpleDateFormat("dd MMM", Locale.US);
        dateSdf.setTimeZone(getAppTimeZone());

        SimpleDateFormat todaySdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        todaySdf.setTimeZone(getAppTimeZone());

        String todayStr = todaySdf.format(new Date(getCalibratedCurrentTimeMillis()));

        for (DoctorSlot slot : slots) {
            if (slot == null || slot.getSlotDate() == null) continue;
            String cleanDate = cleanDateString(slot.getSlotDate());
            if (isDateInPast(cleanDate)) continue;

            if (selectedClinicId != null && !selectedClinicId.trim().isEmpty()) {
                String slotClinicId = slot.getClinicId();
                if (slotClinicId != null && !slotClinicId.trim().isEmpty() && !slotClinicId.equalsIgnoreCase(selectedClinicId)) {
                    continue;
                }
            }

            if (!rawDates.contains(cleanDate)) {
                rawDates.add(cleanDate);
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
     * Filter slots by date, sorted by start_time ascending.
     * Deduplicates slots with identical normalized start_time.
     * Marks slots as 'locked' if currentTime >= slotStartTime.
     */
    public static List<DoctorSlot> filterAndSortSlots(List<DoctorSlot> slots, String selectedClinicId, String selectedDateRaw) {
        if (slots == null || slots.isEmpty()) return new ArrayList<>();

        Map<String, DoctorSlot> uniqueSlotsMap = new LinkedHashMap<>();
        String targetDate = selectedDateRaw != null ? cleanDateString(selectedDateRaw) : null;

        for (DoctorSlot slot : slots) {
            if (slot == null || slot.getSlotDate() == null) continue;
            String slotDate = cleanDateString(slot.getSlotDate());

            if (isDateInPast(slotDate)) continue;

            if (selectedClinicId != null && !selectedClinicId.trim().isEmpty()) {
                String slotClinicId = slot.getClinicId();
                if (slotClinicId != null && !slotClinicId.trim().isEmpty() && !slotClinicId.equalsIgnoreCase(selectedClinicId)) {
                    continue;
                }
            }

            // Date filtering
            if (targetDate != null && !targetDate.isEmpty()) {
                if (!targetDate.equalsIgnoreCase(slotDate)) {
                    continue;
                }
            }

            String normTime = normalizeTimeFormat(slot.getStartTime());

            // Check if slot start time has passed
            boolean isTimePassed = isSlotTimeReachedOrPassed(slotDate, normTime);

            DoctorSlot displaySlot = new DoctorSlot(
                    slot.getId(),
                    slot.getDoctorId(),
                    slot.getClinicId(),
                    slot.getSlotDate(),
                    slot.getStartTime(),
                    slot.getEndTime(),
                    slot.getStatus()
            );
            displaySlot.setAppointmentId(slot.getAppointmentId());
            displaySlot.setMorningStart(slot.getMorningStart());
            displaySlot.setMorningEnd(slot.getMorningEnd());
            displaySlot.setEveningStart(slot.getEveningStart());
            displaySlot.setEveningEnd(slot.getEveningEnd());
            displaySlot.setSlotDuration(slot.getSlotDuration());
            displaySlot.setConsultDuration(slot.getConsultDuration());
            displaySlot.setBufferGap(slot.getBufferGap());
            displaySlot.setFee(slot.getFee());
            displaySlot.setFollowUpFee(slot.getFollowUpFee());

            DoctorSlot existing = uniqueSlotsMap.get(normTime);
            if (existing == null) {
                if (isTimePassed && "available".equalsIgnoreCase(displaySlot.getStatus())) {
                    displaySlot.setStatus("locked");
                }
                uniqueSlotsMap.put(normTime, displaySlot);
            } else {
                if ("booked".equalsIgnoreCase(displaySlot.getStatus()) || !"available".equalsIgnoreCase(displaySlot.getStatus())) {
                    existing.setStatus("booked");
                } else if (isTimePassed) {
                    existing.setStatus("locked");
                }
            }
        }

        List<DoctorSlot> filtered = new ArrayList<>(uniqueSlotsMap.values());

        // Sort by start_time ascending
        Collections.sort(filtered, (s1, s2) -> {
            String t1 = normalizeTimeFormat(s1.getStartTime());
            String t2 = normalizeTimeFormat(s2.getStartTime());
            return t1.compareTo(t2);
        });

        return filtered;
    }

    public static List<String> getUniqueAvailableDates(List<DoctorSlot> slots) {
        if (slots == null || slots.isEmpty()) return new ArrayList<>();

        List<String> dates = new ArrayList<>();
        for (DoctorSlot slot : slots) {
            if (slot != null && slot.getSlotDate() != null) {
                String cleanDate = cleanDateString(slot.getSlotDate());
                if (!isDateInPast(cleanDate) && !dates.contains(cleanDate)) {
                    dates.add(cleanDate);
                }
            }
        }
        Collections.sort(dates);
        return dates;
    }

    public static String formatSlotTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return "";
        try {
            SimpleDateFormat sdfIn = new SimpleDateFormat("HH:mm", Locale.US);
            sdfIn.setTimeZone(getAppTimeZone());
            SimpleDateFormat sdfOut = new SimpleDateFormat("hh:mm a", Locale.US);
            sdfOut.setTimeZone(getAppTimeZone());
            Date date = sdfIn.parse(timeStr);
            return date != null ? sdfOut.format(date) : timeStr;
        } catch (Exception e) {
            return timeStr;
        }
    }
}
