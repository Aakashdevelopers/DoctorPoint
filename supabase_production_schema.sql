-- ============================================================================
-- DOCTOR POINT - PRODUCTION-READY SUPABASE POSTGRESQL DATABASE SCHEMA
-- ============================================================================
-- Features:
--   1. Comprehensive Doctor Appointment Platform Tables (12 tables)
--   2. Strict Foreign Key Integrity & Constraints
--   3. Double-Booking Prevention via PostgreSQL RPC with Atomic Row Locking (FOR UPDATE)
--   4. Automatic Slot Generator Function
--   5. Row Level Security (RLS) Policies on all user-sensitive tables
--   6. Performance Indexes for Search, Slots, and Appointments
--   7. Automatic User Profile Creation Trigger on Auth Signup
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 0. CLEANUP & EXTENSIONS
-- ----------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ----------------------------------------------------------------------------
-- 1. TABLE: profiles
-- Stores basic user information for all roles (patient, doctor, admin, staff)
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS profiles CASCADE;
CREATE TABLE profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(255) NOT NULL,
    avatar_url TEXT,
    role VARCHAR(20) NOT NULL DEFAULT 'patient'
        CHECK (role IN ('patient', 'doctor', 'admin', 'staff')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- 2. TABLE: patients
-- Stores patient-specific medical and personal details
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS patients CASCADE;
CREATE TABLE patients (
    id UUID PRIMARY KEY REFERENCES profiles(id) ON DELETE CASCADE,
    date_of_birth DATE,
    gender VARCHAR(20) CHECK (gender IN ('Male', 'Female', 'Other')),
    blood_group VARCHAR(10) CHECK (blood_group IN ('A+', 'A-', 'B+', 'B-', 'O+', 'O-', 'AB+', 'AB-')),
    emergency_contact VARCHAR(20),
    address TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- 3. TABLE: doctors
-- Stores professional details, pricing, and verification status for doctors
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS doctors CASCADE;
CREATE TABLE doctors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL UNIQUE REFERENCES profiles(id) ON DELETE CASCADE,
    specialty VARCHAR(150) NOT NULL,
    qualification VARCHAR(200) NOT NULL,
    experience_years INT NOT NULL DEFAULT 0 CHECK (experience_years >= 0),
    registration_number VARCHAR(100),
    consultation_fee NUMERIC(10, 2) NOT NULL DEFAULT 0.00 CHECK (consultation_fee >= 0),
    video_fee NUMERIC(10, 2) NOT NULL DEFAULT 0.00 CHECK (video_fee >= 0),
    audio_fee NUMERIC(10, 2) NOT NULL DEFAULT 0.00 CHECK (audio_fee >= 0),
    chat_fee NUMERIC(10, 2) NOT NULL DEFAULT 0.00 CHECK (chat_fee >= 0),
    bio TEXT,
    profile_image TEXT,
    verification_status VARCHAR(20) NOT NULL DEFAULT 'pending'
        CHECK (verification_status IN ('pending', 'verified', 'rejected', 'suspended')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- 4. TABLE: clinics
-- Stores clinic/hospital physical locations associated with a doctor
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS clinics CASCADE;
CREATE TABLE clinics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id UUID NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    clinic_name VARCHAR(200) NOT NULL,
    address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100),
    pincode VARCHAR(20),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    phone VARCHAR(20),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- 5. TABLE: doctor_availability
-- Defines recurring weekly or specific date availability rules for doctors
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS doctor_availability CASCADE;
CREATE TABLE doctor_availability (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id UUID NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    clinic_id UUID REFERENCES clinics(id) ON DELETE CASCADE,
    day_of_week INT CHECK (day_of_week BETWEEN 0 AND 6), -- 0 = Sunday, 1 = Monday, ..., 6 = Saturday
    specific_date DATE, -- Used for one-off/monthly specific date schedules
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    break_start TIME,
    break_end TIME,
    slot_duration INT NOT NULL DEFAULT 15 CHECK (slot_duration > 0), -- in minutes
    valid_from DATE NOT NULL DEFAULT CURRENT_DATE,
    valid_until DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_end_after_start CHECK (end_time > start_time),
    CONSTRAINT chk_availability_type CHECK (
        (day_of_week IS NOT NULL AND specific_date IS NULL) OR
        (day_of_week IS NULL AND specific_date IS NOT NULL)
    )
);

-- ----------------------------------------------------------------------------
-- 6. TABLE: doctor_slots
-- Holds the actual bookable individual time slots generated from availability
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS doctor_slots CASCADE;
CREATE TABLE doctor_slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id UUID NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    clinic_id UUID REFERENCES clinics(id) ON DELETE CASCADE,
    slot_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'available'
        CHECK (status IN ('available', 'booked', 'blocked')),
    appointment_id UUID, -- Foreign key reference added after appointments table
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT unq_doctor_slot UNIQUE (doctor_id, clinic_id, slot_date, start_time)
);

-- ----------------------------------------------------------------------------
-- 7. TABLE: appointments
-- Stores booked appointments between patients and doctors
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS appointments CASCADE;
CREATE TABLE appointments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    doctor_id UUID NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    clinic_id UUID REFERENCES clinics(id) ON DELETE SET NULL,
    slot_id UUID NOT NULL UNIQUE REFERENCES doctor_slots(id) ON DELETE RESTRICT,
    appointment_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    appointment_type VARCHAR(20) NOT NULL DEFAULT 'clinic'
        CHECK (appointment_type IN ('clinic', 'video', 'audio', 'chat')),
    status VARCHAR(30) NOT NULL DEFAULT 'confirmed'
        CHECK (status IN ('pending', 'confirmed', 'checked_in', 'waiting', 'in_consultation', 'completed', 'cancelled', 'rejected', 'no_show')),
    payment_status VARCHAR(20) NOT NULL DEFAULT 'unpaid'
        CHECK (payment_status IN ('unpaid', 'pending', 'paid', 'failed', 'refunded')),
    amount NUMERIC(10, 2) NOT NULL DEFAULT 0.00 CHECK (amount >= 0),
    token_number INT,
    booking_source VARCHAR(30) NOT NULL DEFAULT 'patient'
        CHECK (booking_source IN ('patient', 'doctor', 'receptionist', 'admin')),
    patient_reason TEXT,
    doctor_notes TEXT,
    cancellation_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Add foreign key reference on doctor_slots.appointment_id
ALTER TABLE doctor_slots
    ADD CONSTRAINT fk_doctor_slots_appointment
    FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE SET NULL;

-- ----------------------------------------------------------------------------
-- 8. TABLE: prescriptions
-- Digital prescriptions issued by doctors for completed appointments
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS prescriptions CASCADE;
CREATE TABLE prescriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL UNIQUE REFERENCES appointments(id) ON DELETE CASCADE,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    doctor_id UUID NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    diagnosis TEXT,
    symptoms TEXT,
    advice TEXT,
    follow_up_date DATE,
    prescription_data JSONB NOT NULL DEFAULT '[]'::jsonb, -- List of medications [{name, dosage, frequency, duration}]
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- 9. TABLE: reviews
-- Patient feedback and ratings for doctors
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS reviews CASCADE;
CREATE TABLE reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    doctor_id UUID NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    appointment_id UUID UNIQUE REFERENCES appointments(id) ON DELETE SET NULL,
    rating NUMERIC(2, 1) NOT NULL CHECK (rating >= 1.0 AND rating <= 5.0),
    review TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- 10. TABLE: notifications
-- User notification logs for appointment status, reminders, and alerts
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS notifications CASCADE;
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'general',
    reference_id UUID,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- 11. TABLE: payments
-- Payment transactions for appointment bookings
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS payments CASCADE;
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    doctor_id UUID NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    amount NUMERIC(10, 2) NOT NULL CHECK (amount >= 0),
    gateway VARCHAR(50) NOT NULL DEFAULT 'stripe',
    transaction_id VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'completed', 'failed', 'refunded')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- 12. TABLE: doctor_staff
-- Receptionists and clinic assistants delegated by doctors
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS doctor_staff CASCADE;
CREATE TABLE doctor_staff (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id UUID NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    permissions JSONB NOT NULL DEFAULT '{"can_book": true, "can_cancel": true}'::jsonb,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT unq_doctor_staff UNIQUE (doctor_id, profile_id)
);


-- ============================================================================
-- PERFORMANCE INDEXES
-- ============================================================================
CREATE INDEX idx_doctors_specialty ON doctors(specialty);
CREATE INDEX idx_doctors_verification_status ON doctors(verification_status);
CREATE INDEX idx_doctors_is_active ON doctors(is_active);

CREATE INDEX idx_doctor_slots_slot_date ON doctor_slots(slot_date);
CREATE INDEX idx_doctor_slots_status ON doctor_slots(status);
CREATE INDEX idx_doctor_slots_doctor_id ON doctor_slots(doctor_id);
CREATE INDEX idx_doctor_slots_lookup ON doctor_slots(doctor_id, slot_date, status);

CREATE INDEX idx_appointments_patient_id ON appointments(patient_id);
CREATE INDEX idx_appointments_doctor_id ON appointments(doctor_id);
CREATE INDEX idx_appointments_appointment_date ON appointments(appointment_date);
CREATE INDEX idx_appointments_status ON appointments(status);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(user_id, is_read);


-- ============================================================================
-- AUTOMATIC PROFILE CREATION TRIGGER ON AUTH SIGNUP
-- ============================================================================
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER SET search_path = public
AS $$
DECLARE
    v_role VARCHAR(20);
BEGIN
    v_role := COALESCE(NEW.raw_user_meta_data->>'role', 'patient');

    INSERT INTO public.profiles (id, full_name, email, phone, avatar_url, role)
    VALUES (
        NEW.id,
        COALESCE(NEW.raw_user_meta_data->>'full_name', NEW.email),
        NEW.email,
        NEW.raw_user_meta_data->>'phone',
        NEW.raw_user_meta_data->>'avatar_url',
        v_role
    );

    IF v_role = 'patient' THEN
        INSERT INTO public.patients (id)
        VALUES (NEW.id);
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();


-- ============================================================================
-- DOUBLE BOOKING PREVENTION & ATOMIC BOOKING RPC
-- ============================================================================
-- Uses PostgreSQL Row Locking (FOR UPDATE) to guarantee thread safety.
-- If two patients attempt to book the exact same slot simultaneously,
-- PostgreSQL forces sequential execution. The first caller books the slot;
-- the second receives 'Slot is no longer available.'
-- ============================================================================
CREATE OR REPLACE FUNCTION book_appointment(
    p_patient_id UUID,
    p_slot_id UUID,
    p_appointment_type VARCHAR(20) DEFAULT 'clinic',
    p_patient_reason TEXT DEFAULT NULL,
    p_booking_source VARCHAR(30) DEFAULT 'patient'
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_slot RECORD;
    v_doctor RECORD;
    v_patient RECORD;
    v_appointment_id UUID;
    v_calculated_fee NUMERIC(10, 2);
    v_result JSONB;
BEGIN
    -- 1. Exclusively lock the slot row to prevent concurrent booking race conditions
    SELECT * INTO v_slot
    FROM doctor_slots
    WHERE id = p_slot_id
    FOR UPDATE;

    -- 2. Verify slot exists
    IF v_slot.id IS NULL THEN
        RAISE EXCEPTION 'Slot not found.' USING ERRCODE = 'P0002';
    END IF;

    -- 3. Verify slot availability
    IF v_slot.status != 'available' THEN
        RAISE EXCEPTION 'Slot is no longer available.' USING ERRCODE = 'P0001';
    END IF;

    -- 4. Verify slot is not in the past
    IF v_slot.slot_date < CURRENT_DATE OR (v_slot.slot_date = CURRENT_DATE AND v_slot.start_time <= CURRENT_TIME) THEN
        RAISE EXCEPTION 'Past slots cannot be booked.' USING ERRCODE = 'P0003';
    END IF;

    -- 5. Verify doctor exists, is active and verified
    SELECT * INTO v_doctor
    FROM doctors
    WHERE id = v_slot.doctor_id;

    IF v_doctor.id IS NULL THEN
        RAISE EXCEPTION 'Doctor not found.' USING ERRCODE = 'P0004';
    END IF;

    IF NOT v_doctor.is_active OR v_doctor.verification_status != 'verified' THEN
        RAISE EXCEPTION 'Doctor is inactive or unverified.' USING ERRCODE = 'P0005';
    END IF;

    -- 6. Verify patient exists
    SELECT * INTO v_patient
    FROM patients
    WHERE id = p_patient_id;

    IF v_patient.id IS NULL THEN
        RAISE EXCEPTION 'Patient profile not found.' USING ERRCODE = 'P0006';
    END IF;

    -- 7. Determine Fee based on appointment type
    IF p_appointment_type = 'video' THEN
        v_calculated_fee := v_doctor.video_fee;
    ELSIF p_appointment_type = 'audio' THEN
        v_calculated_fee := v_doctor.audio_fee;
    ELSIF p_appointment_type = 'chat' THEN
        v_calculated_fee := v_doctor.chat_fee;
    ELSE
        v_calculated_fee := v_doctor.consultation_fee;
    END IF;

    -- 8. Create Appointment record
    INSERT INTO appointments (
        patient_id,
        doctor_id,
        clinic_id,
        slot_id,
        appointment_date,
        start_time,
        end_time,
        appointment_type,
        status,
        payment_status,
        amount,
        booking_source,
        patient_reason
    )
    VALUES (
        p_patient_id,
        v_slot.doctor_id,
        v_slot.clinic_id,
        v_slot.id,
        v_slot.slot_date,
        v_slot.start_time,
        v_slot.end_time,
        p_appointment_type,
        'confirmed',
        'unpaid',
        v_calculated_fee,
        p_booking_source,
        p_patient_reason
    )
    RETURNING id INTO v_appointment_id;

    -- 9. Update Slot status to booked
    UPDATE doctor_slots
    SET
        status = 'booked',
        appointment_id = v_appointment_id
    WHERE id = v_slot.id;

    -- 10. Send Notification to Doctor
    INSERT INTO notifications (user_id, title, message, type, reference_id)
    VALUES (
        v_doctor.profile_id,
        'New Appointment Booked',
        'Appointment booked for ' || v_slot.slot_date || ' at ' || v_slot.start_time,
        'appointment',
        v_appointment_id
    );

    -- 11. Send Notification to Patient
    INSERT INTO notifications (user_id, title, message, type, reference_id)
    VALUES (
        p_patient_id,
        'Appointment Confirmed',
        'Your appointment on ' || v_slot.slot_date || ' at ' || v_slot.start_time || ' has been confirmed.',
        'appointment',
        v_appointment_id
    );

    -- 12. Construct response JSON
    v_result := jsonb_build_object(
        'success', true,
        'appointment_id', v_appointment_id,
        'slot_id', v_slot.id,
        'doctor_id', v_slot.doctor_id,
        'patient_id', p_patient_id,
        'slot_date', v_slot.slot_date,
        'start_time', v_slot.start_time,
        'end_time', v_slot.end_time,
        'amount', v_calculated_fee,
        'message', 'Appointment successfully booked'
    );

    RETURN v_result;
END;
$$;


-- ============================================================================
-- AUTOMATIC SLOT GENERATION RPC
-- Generates individual doctor_slots from doctor_availability schedules
-- ============================================================================
CREATE OR REPLACE FUNCTION generate_doctor_slots(
    p_doctor_id UUID,
    p_start_date DATE,
    p_end_date DATE
)
RETURNS INT
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_rule RECORD;
    v_curr_date DATE;
    v_dow INT;
    v_slot_start TIME;
    v_slot_end TIME;
    v_slots_created INT := 0;
BEGIN
    FOR v_rule IN
        SELECT * FROM doctor_availability
        WHERE doctor_id = p_doctor_id
          AND is_active = TRUE
    LOOP
        -- Loop through date range
        v_curr_date := GREATEST(p_start_date, v_rule.valid_from);

        WHILE v_curr_date <= LEAST(p_end_date, COALESCE(v_rule.valid_until, p_end_date)) LOOP
            v_dow := EXTRACT(DOW FROM v_curr_date)::INT;

            -- Check matching rule (Weekly schedule or specific date)
            IF (v_rule.day_of_week IS NOT NULL AND v_rule.day_of_week = v_dow) OR
               (v_rule.specific_date IS NOT NULL AND v_rule.specific_date = v_curr_date) THEN

                v_slot_start := v_rule.start_time;

                WHILE (v_slot_start + (v_rule.slot_duration || ' minutes')::INTERVAL) <= v_rule.end_time LOOP
                    v_slot_end := (v_slot_start + (v_rule.slot_duration || ' minutes')::INTERVAL)::TIME;

                    -- Exclude break time if configured
                    IF NOT (v_rule.break_start IS NOT NULL AND v_rule.break_end IS NOT NULL AND
                            v_slot_start >= v_rule.break_start AND v_slot_start < v_rule.break_end) THEN

                        -- Insert slot ignoring existing duplicates
                        INSERT INTO doctor_slots (
                            doctor_id, clinic_id, slot_date, start_time, end_time, status
                        )
                        VALUES (
                            p_doctor_id, v_rule.clinic_id, v_curr_date, v_slot_start, v_slot_end, 'available'
                        )
                        ON CONFLICT (doctor_id, clinic_id, slot_date, start_time) DO NOTHING;

                        IF FOUND THEN
                            v_slots_created := v_slots_created + 1;
                        END IF;
                    END IF;

                    v_slot_start := v_slot_end;
                END LOOP;

            END IF;

            v_curr_date := v_curr_date + 1;
        END LOOP;
    END LOOP;

    RETURN v_slots_created;
END;
$$;


-- ============================================================================
-- CANCEL APPOINTMENT RPC
-- Safely cancels appointment and releases the associated slot back to 'available'
-- ============================================================================
CREATE OR REPLACE FUNCTION cancel_appointment(
    p_appointment_id UUID,
    p_canceller_id UUID,
    p_cancellation_reason TEXT DEFAULT 'Cancelled by user'
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_appt RECORD;
    v_result JSONB;
BEGIN
    SELECT * INTO v_appt
    FROM appointments
    WHERE id = p_appointment_id
    FOR UPDATE;

    IF v_appt.id IS NULL THEN
        RAISE EXCEPTION 'Appointment not found.';
    END IF;

    IF v_appt.status IN ('cancelled', 'completed') THEN
        RAISE EXCEPTION 'Appointment cannot be cancelled in its current state.';
    END IF;

    -- Update appointment status
    UPDATE appointments
    SET
        status = 'cancelled',
        cancellation_reason = p_cancellation_reason,
        updated_at = NOW()
    WHERE id = p_appointment_id;

    -- Release slot back to available if slot date is in future
    IF v_appt.appointment_date >= CURRENT_DATE THEN
        UPDATE doctor_slots
        SET status = 'available', appointment_id = NULL
        WHERE id = v_appt.slot_id;
    END IF;

    v_result := jsonb_build_object(
        'success', true,
        'appointment_id', p_appointment_id,
        'message', 'Appointment cancelled successfully'
    );

    RETURN v_result;
END;
$$;


-- ============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ============================================================================
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE patients ENABLE ROW LEVEL SECURITY;
ALTER TABLE doctors ENABLE ROW LEVEL SECURITY;
ALTER TABLE clinics ENABLE ROW LEVEL SECURITY;
ALTER TABLE doctor_availability ENABLE ROW LEVEL SECURITY;
ALTER TABLE doctor_slots ENABLE ROW LEVEL SECURITY;
ALTER TABLE appointments ENABLE ROW LEVEL SECURITY;
ALTER TABLE prescriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE reviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE doctor_staff ENABLE ROW LEVEL SECURITY;

-- 1. profiles RLS
CREATE POLICY "Public profile reading" ON profiles FOR SELECT USING (true);
CREATE POLICY "Users can update own profile" ON profiles FOR UPDATE USING (auth.uid() = id);

-- 2. patients RLS
CREATE POLICY "Patients view own details" ON patients FOR SELECT USING (auth.uid() = id);
CREATE POLICY "Patients update own details" ON patients FOR UPDATE USING (auth.uid() = id);
CREATE POLICY "Doctors view patient details for appointments" ON patients FOR SELECT USING (
    EXISTS (
        SELECT 1 FROM appointments a
        JOIN doctors d ON a.doctor_id = d.id
        WHERE a.patient_id = patients.id AND d.profile_id = auth.uid()
    )
);

-- 3. doctors RLS
CREATE POLICY "Public can view active and verified doctors" ON doctors FOR SELECT USING (
    is_active = TRUE AND verification_status = 'verified'
);
CREATE POLICY "Doctor view own record" ON doctors FOR SELECT USING (profile_id = auth.uid());
CREATE POLICY "Doctor update own record" ON doctors FOR UPDATE USING (profile_id = auth.uid());

-- 4. clinics RLS
CREATE POLICY "Public view active clinics" ON clinics FOR SELECT USING (is_active = TRUE);
CREATE POLICY "Doctors manage own clinics" ON clinics FOR ALL USING (
    EXISTS (SELECT 1 FROM doctors WHERE id = clinics.doctor_id AND profile_id = auth.uid())
);

-- 5. doctor_availability RLS
CREATE POLICY "Public view doctor availability" ON doctor_availability FOR SELECT USING (is_active = TRUE);
CREATE POLICY "Doctors manage own availability" ON doctor_availability FOR ALL USING (
    EXISTS (SELECT 1 FROM doctors WHERE id = doctor_availability.doctor_id AND profile_id = auth.uid())
);

-- 6. doctor_slots RLS
CREATE POLICY "Public view available slots" ON doctor_slots FOR SELECT USING (true);
CREATE POLICY "Doctors manage own slots" ON doctor_slots FOR ALL USING (
    EXISTS (SELECT 1 FROM doctors WHERE id = doctor_slots.doctor_id AND profile_id = auth.uid())
);

-- 7. appointments RLS
CREATE POLICY "Patients view own appointments" ON appointments FOR SELECT USING (patient_id = auth.uid());
CREATE POLICY "Doctors view appointments" ON appointments FOR SELECT USING (
    EXISTS (SELECT 1 FROM doctors WHERE id = appointments.doctor_id AND profile_id = auth.uid())
);

-- 8. prescriptions RLS
CREATE POLICY "Patients view own prescriptions" ON prescriptions FOR SELECT USING (patient_id = auth.uid());
CREATE POLICY "Doctors manage own prescriptions" ON prescriptions FOR ALL USING (
    EXISTS (SELECT 1 FROM doctors WHERE id = prescriptions.doctor_id AND profile_id = auth.uid())
);

-- 9. reviews RLS
CREATE POLICY "Public view reviews" ON reviews FOR SELECT USING (true);
CREATE POLICY "Patients write reviews" ON reviews FOR INSERT WITH CHECK (patient_id = auth.uid());

-- 10. notifications RLS
CREATE POLICY "Users view own notifications" ON notifications FOR SELECT USING (user_id = auth.uid());
CREATE POLICY "Users update own notifications" ON notifications FOR UPDATE USING (user_id = auth.uid());

-- 11. payments RLS
CREATE POLICY "Patients view own payments" ON payments FOR SELECT USING (patient_id = auth.uid());

-- 12. doctor_staff RLS
CREATE POLICY "Doctors manage staff" ON doctor_staff FOR ALL USING (
    EXISTS (SELECT 1 FROM doctors WHERE id = doctor_staff.doctor_id AND profile_id = auth.uid())
);
