-- ============================================================================
-- DOCTOR POINT COMPLETE SUPABASE DATABASE SETUP SCRIPT
-- Application: Doctor Point (Android App + Admin Panel + Super Panel)
-- Database Engine: PostgreSQL / Supabase
-- Description: Executes full schema setup, tables, indexes, triggers,
--              RPC stored procedures, RLS policies, storage buckets,
--              and seed data in a single run.
-- ============================================================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- 1. DROP EXISTING TABLES & FUNCTIONS (Clean Re-run Support)
-- ============================================================================
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
DROP FUNCTION IF EXISTS public.handle_new_user CASCADE;
DROP FUNCTION IF EXISTS public.book_appointment CASCADE;
DROP FUNCTION IF EXISTS public.cancel_appointment CASCADE;
DROP FUNCTION IF EXISTS public.update_doctor_earnings CASCADE;
DROP FUNCTION IF EXISTS public.recalculate_doctor_rating CASCADE;
DROP FUNCTION IF EXISTS public.update_updated_at_column CASCADE;

DROP TABLE IF EXISTS public.refund_requests CASCADE;
DROP TABLE IF EXISTS public.withdrawals CASCADE;
DROP TABLE IF EXISTS public.doctor_reviews CASCADE;
DROP TABLE IF EXISTS public.medical_records CASCADE;
DROP TABLE IF EXISTS public.notifications CASCADE;
DROP TABLE IF EXISTS public.appointments CASCADE;
DROP TABLE IF EXISTS public.doctor_slots CASCADE;
DROP TABLE IF EXISTS public.doctor_schedules CASCADE;
DROP TABLE IF EXISTS public.clinics CASCADE;
DROP TABLE IF EXISTS public.specialities CASCADE;
DROP TABLE IF EXISTS public.commission_settings CASCADE;
DROP TABLE IF EXISTS public.doctors CASCADE;
DROP TABLE IF EXISTS public.patients CASCADE;
DROP TABLE IF EXISTS public.profiles CASCADE;

-- ============================================================================
-- 2. CREATE TABLES
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Table: profiles (Users / Patients / Admins)
-- ----------------------------------------------------------------------------
CREATE TABLE public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name TEXT NOT NULL,
    email TEXT UNIQUE,
    phone TEXT,
    avatar_url TEXT,
    role TEXT DEFAULT 'patient', -- 'patient', 'doctor', 'admin', 'superadmin'
    date_of_birth TEXT,
    gender TEXT,
    blood_group TEXT,
    emergency_contact TEXT,
    address TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- Table: patients (Detailed patient profile information)
-- ----------------------------------------------------------------------------
CREATE TABLE public.patients (
    id UUID PRIMARY KEY REFERENCES public.profiles(id) ON DELETE CASCADE,
    full_name TEXT NOT NULL,
    email TEXT,
    phone TEXT,
    avatar_url TEXT,
    date_of_birth TEXT,
    gender TEXT,
    blood_group TEXT,
    emergency_contact TEXT,
    address TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- Table: doctors (Doctor profiles, fees, earnings, locations & settings)
-- ----------------------------------------------------------------------------
CREATE TABLE public.doctors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    qualification TEXT,
    specialization TEXT,
    experience TEXT,
    rating NUMERIC(3,2) DEFAULT 5.0,
    review_count INTEGER DEFAULT 0,
    clinic_name TEXT,
    location TEXT,
    state TEXT DEFAULT 'Karnataka',
    clinic_latitude NUMERIC(10,7),
    clinic_longitude NUMERIC(10,7),
    fee INTEGER DEFAULT 500,
    followup_fee INTEGER DEFAULT 300,
    image_url TEXT,
    clinic_photos TEXT,
    about TEXT,
    doctor_phone TEXT,
    reception_phone TEXT,
    today_earning NUMERIC(12,2) DEFAULT 0,
    total_earning NUMERIC(12,2) DEFAULT 0,
    wallet_balance NUMERIC(12,2) DEFAULT 0,
    current_balance NUMERIC(12,2) DEFAULT 0,
    new_patient_commission NUMERIC(5,2),
    followup_commission NUMERIC(5,2),
    commission_type TEXT DEFAULT 'percentage', -- 'percentage' or 'flat'
    is_verified BOOLEAN DEFAULT TRUE,
    gender TEXT DEFAULT 'Female',
    is_available_today BOOLEAN DEFAULT TRUE,
    is_nearby BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- Table: specialities (Medical specializations / categories)
-- ----------------------------------------------------------------------------
CREATE TABLE public.specialities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    name TEXT,
    icon_url TEXT,
    image_url TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- Table: clinics (Doctor clinics & branches)
-- ----------------------------------------------------------------------------
CREATE TABLE public.clinics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id UUID REFERENCES public.doctors(id) ON DELETE CASCADE,
    clinic_name TEXT NOT NULL,
    address TEXT,
    city TEXT,
    state TEXT,
    pincode TEXT,
    phone TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- Table: doctor_slots (Time slots for doctor appointments)
-- ----------------------------------------------------------------------------
CREATE TABLE public.doctor_slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id UUID REFERENCES public.doctors(id) ON DELETE CASCADE,
    clinic_id UUID REFERENCES public.clinics(id) ON DELETE SET NULL,
    slot_date DATE NOT NULL,
    start_time TEXT NOT NULL,
    end_time TEXT,
    status TEXT DEFAULT 'available', -- 'available', 'booked', 'blocked', 'cancelled'
    appointment_id UUID,
    morning_start TEXT,
    morning_end TEXT,
    evening_start TEXT,
    evening_end TEXT,
    slot_duration INTEGER DEFAULT 20,
    consult_duration INTEGER DEFAULT 15,
    buffer_gap INTEGER DEFAULT 5,
    fee INTEGER DEFAULT 500,
    followup_fee INTEGER DEFAULT 300,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- Table: doctor_schedules (Daily recurring schedule templates for doctors)
-- ----------------------------------------------------------------------------
CREATE TABLE public.doctor_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id UUID REFERENCES public.doctors(id) ON DELETE CASCADE,
    slot_date DATE NOT NULL,
    morning_start TEXT,
    morning_end TEXT,
    evening_start TEXT,
    evening_end TEXT,
    slot_duration INTEGER DEFAULT 20,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- Table: appointments (Booked doctor appointments)
-- ----------------------------------------------------------------------------
CREATE TABLE public.appointments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    patient_name TEXT,
    doctor_id UUID REFERENCES public.doctors(id) ON DELETE CASCADE,
    doctor_name TEXT,
    doctor_specialization TEXT,
    clinic_id UUID REFERENCES public.clinics(id) ON DELETE SET NULL,
    clinic_name TEXT,
    clinic_location TEXT,
    slot_id UUID REFERENCES public.doctor_slots(id) ON DELETE SET NULL,
    appointment_date DATE NOT NULL,
    start_time TEXT NOT NULL,
    end_time TEXT,
    appointment_type TEXT DEFAULT 'clinic', -- 'clinic', 'online'
    status TEXT DEFAULT 'Pending', -- 'Pending', 'Confirmed', 'Checked_In', 'Waiting', 'In_Consultation', 'Completed', 'Cancelled', 'Rejected', 'No_Show'
    payment_status TEXT DEFAULT 'Pending', -- 'Pending', 'Paid', 'Refunded', 'Failed'
    amount INTEGER DEFAULT 0,
    token_number INTEGER DEFAULT 1,
    patient_reason TEXT,
    prescription TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Foreign key back link for slot
ALTER TABLE public.doctor_slots
    ADD CONSTRAINT fk_doctor_slots_appointment
    FOREIGN KEY (appointment_id) REFERENCES public.appointments(id) ON DELETE SET NULL;

-- ----------------------------------------------------------------------------
-- Table: doctor_reviews (Patient feedback & ratings for doctors)
-- ----------------------------------------------------------------------------
CREATE TABLE public.doctor_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID REFERENCES public.appointments(id) ON DELETE CASCADE,
    doctor_id UUID REFERENCES public.doctors(id) ON DELETE CASCADE,
    patient_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    patient_name TEXT,
    patient_avatar TEXT,
    rating NUMERIC(3,2) DEFAULT 5.0,
    review_text TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT unique_appointment_review UNIQUE (appointment_id)
);

-- ----------------------------------------------------------------------------
-- Table: medical_records (Patient prescriptions, lab reports, doctor records)
-- ----------------------------------------------------------------------------
CREATE TABLE public.medical_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    record_type TEXT DEFAULT 'prescription', -- 'prescription', 'lab_report', 'medical_report', 'scan'
    title TEXT NOT NULL,
    doctor_name TEXT,
    clinic_name TEXT,
    record_date DATE DEFAULT CURRENT_DATE,
    file_path TEXT,
    file_url TEXT,
    notes TEXT,
    diagnosis TEXT,
    symptoms TEXT,
    medicines TEXT,
    dosage TEXT,
    duration TEXT,
    advice TEXT,
    follow_up_date DATE,
    is_doctor_generated BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- Table: notifications (In-app notifications)
-- ----------------------------------------------------------------------------
CREATE TABLE public.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    type TEXT DEFAULT 'appointment_confirmed',
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- Table: refund_requests (Cancelled appointment refund requests)
-- ----------------------------------------------------------------------------
CREATE TABLE public.refund_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID REFERENCES public.appointments(id) ON DELETE CASCADE,
    patient_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    patient_name TEXT,
    patient_phone TEXT,
    doctor_id UUID REFERENCES public.doctors(id) ON DELETE CASCADE,
    doctor_name TEXT,
    doctor_specialization TEXT,
    clinic_name TEXT,
    appointment_date DATE,
    amount INTEGER DEFAULT 0,
    reason TEXT,
    patient_upi TEXT,
    status TEXT DEFAULT 'pending', -- 'pending', 'approved', 'rejected'
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT unique_appointment_refund UNIQUE (appointment_id)
);

-- ----------------------------------------------------------------------------
-- Table: withdrawals (Doctor earnings withdrawal requests)
-- ----------------------------------------------------------------------------
CREATE TABLE public.withdrawals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id UUID REFERENCES public.doctors(id) ON DELETE CASCADE,
    doctor_name TEXT,
    amount NUMERIC(12,2) NOT NULL,
    upi_id TEXT,
    bank_details TEXT,
    status TEXT DEFAULT 'pending', -- 'pending', 'approved', 'rejected'
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- Table: commission_settings (Global platform commission settings)
-- ----------------------------------------------------------------------------
CREATE TABLE public.commission_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    setting_key TEXT UNIQUE DEFAULT 'global_default',
    new_patient_commission NUMERIC(5,2) DEFAULT 10.0,
    new_patient_type TEXT DEFAULT 'percentage', -- 'percentage' or 'flat'
    followup_commission NUMERIC(5,2) DEFAULT 5.0,
    followup_type TEXT DEFAULT 'percentage', -- 'percentage' or 'flat'
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================================
-- 3. INDEXES FOR HIGH PERFORMANCE QUERYING
-- ============================================================================
CREATE INDEX idx_profiles_email ON public.profiles(email);
CREATE INDEX idx_profiles_role ON public.profiles(role);

CREATE INDEX idx_doctors_specialization ON public.doctors(specialization);
CREATE INDEX idx_doctors_location ON public.doctors(location);
CREATE INDEX idx_doctors_state ON public.doctors(state);
CREATE INDEX idx_doctors_is_verified ON public.doctors(is_verified);

CREATE INDEX idx_clinics_doctor_id ON public.clinics(doctor_id);

CREATE INDEX idx_doctor_slots_doctor_date ON public.doctor_slots(doctor_id, slot_date);
CREATE INDEX idx_doctor_slots_status ON public.doctor_slots(status);

CREATE INDEX idx_doctor_schedules_doctor_date ON public.doctor_schedules(doctor_id, slot_date);

CREATE INDEX idx_appointments_patient_id ON public.appointments(patient_id);
CREATE INDEX idx_appointments_doctor_id ON public.appointments(doctor_id);
CREATE INDEX idx_appointments_date ON public.appointments(appointment_date);
CREATE INDEX idx_appointments_status ON public.appointments(status);

CREATE INDEX idx_doctor_reviews_doctor_id ON public.doctor_reviews(doctor_id);
CREATE INDEX idx_medical_records_patient_id ON public.medical_records(patient_id);
CREATE INDEX idx_notifications_patient_id ON public.notifications(patient_id);
CREATE INDEX idx_refund_requests_doctor_id ON public.refund_requests(doctor_id);
CREATE INDEX idx_withdrawals_doctor_id ON public.withdrawals(doctor_id);

-- ============================================================================
-- 4. AUTOMATIC TRIGGERS & UTILITY FUNCTIONS
-- ============================================================================

-- Automatically update updated_at timestamp on row modification
CREATE OR REPLACE FUNCTION public.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_profiles_modtime
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER update_patients_modtime
    BEFORE UPDATE ON public.patients
    FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER update_doctors_modtime
    BEFORE UPDATE ON public.doctors
    FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER update_appointments_modtime
    BEFORE UPDATE ON public.appointments
    FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();

CREATE TRIGGER update_withdrawals_modtime
    BEFORE UPDATE ON public.withdrawals
    FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();

-- Automatically create Profile and Patient record on new Auth user registration
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
DECLARE
    user_name TEXT;
    user_phone TEXT;
    user_address TEXT;
BEGIN
    user_name := COALESCE(NEW.raw_user_meta_data->>'full_name', NEW.raw_user_meta_data->>'name', 'Patient User');
    user_phone := NEW.raw_user_meta_data->>'phone';
    user_address := NEW.raw_user_meta_data->>'address';

    -- Insert into public.profiles
    INSERT INTO public.profiles (id, full_name, email, phone, address, role)
    VALUES (NEW.id, user_name, NEW.email, user_phone, user_address, 'patient')
    ON CONFLICT (id) DO UPDATE SET
        full_name = EXCLUDED.full_name,
        email = EXCLUDED.email,
        phone = COALESCE(EXCLUDED.phone, public.profiles.phone),
        address = COALESCE(EXCLUDED.address, public.profiles.address);

    -- Insert into public.patients
    INSERT INTO public.patients (id, full_name, email, phone, address)
    VALUES (NEW.id, user_name, NEW.email, user_phone, user_address)
    ON CONFLICT (id) DO UPDATE SET
        full_name = EXCLUDED.full_name,
        email = EXCLUDED.email,
        phone = COALESCE(EXCLUDED.phone, public.patients.phone),
        address = COALESCE(EXCLUDED.address, public.patients.address);

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- Automatically recalculate doctor rating and review count when reviews are submitted
CREATE OR REPLACE FUNCTION public.recalculate_doctor_rating()
RETURNS TRIGGER AS $$
DECLARE
    target_doc_id UUID;
    avg_rating NUMERIC(3,2);
    total_revs INT;
BEGIN
    IF (TG_OP = 'DELETE') THEN
        target_doc_id := OLD.doctor_id;
    ELSE
        target_doc_id := NEW.doctor_id;
    END IF;

    SELECT COALESCE(AVG(rating), 5.0), COUNT(*)
    INTO avg_rating, total_revs
    FROM public.doctor_reviews
    WHERE doctor_id = target_doc_id;

    UPDATE public.doctors
    SET rating = ROUND(avg_rating, 1),
        review_count = total_revs
    WHERE id = target_doc_id;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER trigger_update_doctor_rating
    AFTER INSERT OR UPDATE OR DELETE ON public.doctor_reviews
    FOR EACH ROW EXECUTE FUNCTION public.recalculate_doctor_rating();

-- ============================================================================
-- 5. RPC STORED PROCEDURES (BOOKING & CANCELLATION & EARNINGS)
-- ============================================================================

-- RPC: book_appointment
CREATE OR REPLACE FUNCTION public.book_appointment(
    p_patient_id UUID,
    p_slot_id UUID,
    p_appointment_type TEXT DEFAULT 'clinic',
    p_patient_reason TEXT DEFAULT 'General Consultation',
    p_booking_source TEXT DEFAULT 'patient'
)
RETURNS JSON AS $$
DECLARE
    v_slot RECORD;
    v_doctor RECORD;
    v_patient RECORD;
    v_next_token INT;
    v_appointment_id UUID;
    v_amount INT;
BEGIN
    -- 1. Validate slot availability
    SELECT * INTO v_slot FROM public.doctor_slots WHERE id = p_slot_id FOR UPDATE;
    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'message', 'Requested slot does not exist');
    END IF;

    IF v_slot.status != 'available' THEN
        RETURN json_build_object('success', false, 'message', 'Slot is no longer available');
    END IF;

    -- 2. Fetch Doctor details
    SELECT * INTO v_doctor FROM public.doctors WHERE id = v_slot.doctor_id;
    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'message', 'Doctor record not found');
    END IF;

    -- 3. Fetch Patient details
    SELECT * INTO v_patient FROM public.profiles WHERE id = p_patient_id;

    -- 4. Calculate token number for doctor on that date
    SELECT COALESCE(MAX(token_number), 0) + 1 INTO v_next_token
    FROM public.appointments
    WHERE doctor_id = v_slot.doctor_id AND appointment_date = v_slot.slot_date;

    v_appointment_id := gen_random_uuid();
    v_amount := COALESCE(v_slot.fee, v_doctor.fee, 500);

    -- 5. Insert Appointment
    INSERT INTO public.appointments (
        id, patient_id, patient_name, doctor_id, doctor_name, doctor_specialization,
        clinic_id, clinic_name, clinic_location, slot_id, appointment_date,
        start_time, end_time, appointment_type, status, payment_status,
        amount, token_number, patient_reason
    ) VALUES (
        v_appointment_id, p_patient_id, COALESCE(v_patient.full_name, 'Patient'),
        v_doctor.id, v_doctor.name, COALESCE(v_doctor.specialization, 'General Physician'),
        v_slot.clinic_id, COALESCE(v_doctor.clinic_name, 'Care Clinic'),
        COALESCE(v_doctor.location, 'Main Branch'), v_slot.id, v_slot.slot_date,
        v_slot.start_time, v_slot.end_time, COALESCE(p_appointment_type, 'clinic'),
        'Confirmed', 'Paid', v_amount, v_next_token, p_patient_reason
    );

    -- 6. Mark slot as booked
    UPDATE public.doctor_slots
    SET status = 'booked', appointment_id = v_appointment_id
    WHERE id = p_slot_id;

    -- 7. Insert Notification for patient
    INSERT INTO public.notifications (
        patient_id, title, message, type
    ) VALUES (
        p_patient_id,
        'Appointment Confirmed',
        'Your appointment with ' || v_doctor.name || ' is confirmed for ' || TO_CHAR(v_slot.slot_date, 'Mon DD, YYYY') || ' at ' || v_slot.start_time || '. Token #' || v_next_token,
        'appointment_confirmed'
    );

    RETURN json_build_object(
        'success', true,
        'appointment_id', v_appointment_id,
        'slot_id', p_slot_id,
        'doctor_id', v_doctor.id,
        'patient_id', p_patient_id,
        'slot_date', v_slot.slot_date,
        'start_time', v_slot.start_time,
        'amount', v_amount,
        'token_number', v_next_token,
        'message', 'Appointment booked successfully'
    );
EXCEPTION WHEN OTHERS THEN
    RETURN json_build_object('success', false, 'message', SQLERRM);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- RPC: cancel_appointment
CREATE OR REPLACE FUNCTION public.cancel_appointment(
    p_appointment_id UUID,
    p_canceller_id UUID,
    p_cancellation_reason TEXT DEFAULT 'Cancelled by patient'
)
RETURNS JSON AS $$
DECLARE
    v_appointment RECORD;
BEGIN
    SELECT * INTO v_appointment FROM public.appointments WHERE id = p_appointment_id FOR UPDATE;
    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'message', 'Appointment not found');
    END IF;

    -- Update appointment status
    UPDATE public.appointments
    SET status = 'Cancelled', updated_at = NOW()
    WHERE id = p_appointment_id;

    -- Release slot
    IF v_appointment.slot_id IS NOT NULL THEN
        UPDATE public.doctor_slots
        SET status = 'available', appointment_id = NULL
        WHERE id = v_appointment.slot_id;
    END IF;

    -- Send notification if patient exists
    IF v_appointment.patient_id IS NOT NULL THEN
        INSERT INTO public.notifications (
            patient_id, title, message, type
        ) VALUES (
            v_appointment.patient_id,
            'Appointment Cancelled',
            'Your appointment with ' || v_appointment.doctor_name || ' on ' || TO_CHAR(v_appointment.appointment_date, 'Mon DD, YYYY') || ' was cancelled.',
            'appointment_cancelled'
        );
    END IF;

    RETURN json_build_object('success', true, 'message', 'Appointment cancelled successfully');
EXCEPTION WHEN OTHERS THEN
    RETURN json_build_object('success', false, 'message', SQLERRM);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ============================================================================
-- 6. ROW LEVEL SECURITY (RLS) POLICIES
-- ============================================================================

-- Enable RLS on all tables
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.patients ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.doctors ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.specialities ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.clinics ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.doctor_slots ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.doctor_schedules ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.appointments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.doctor_reviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.medical_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.refund_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.withdrawals ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.commission_settings ENABLE ROW LEVEL SECURITY;

-- ----------------------------------------------------------------------------
-- PROFILES RLS
-- ----------------------------------------------------------------------------
CREATE POLICY "Profiles read policy" ON public.profiles
    FOR SELECT USING (true);

CREATE POLICY "Profiles insert policy" ON public.profiles
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Profiles update policy" ON public.profiles
    FOR UPDATE USING (true);

CREATE POLICY "Profiles delete policy" ON public.profiles
    FOR DELETE USING (true);

-- ----------------------------------------------------------------------------
-- PATIENTS RLS
-- ----------------------------------------------------------------------------
CREATE POLICY "Patients read policy" ON public.patients
    FOR SELECT USING (true);

CREATE POLICY "Patients insert policy" ON public.patients
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Patients update policy" ON public.patients
    FOR UPDATE USING (true);

CREATE POLICY "Patients delete policy" ON public.patients
    FOR DELETE USING (true);

-- ----------------------------------------------------------------------------
-- DOCTORS RLS
-- ----------------------------------------------------------------------------
CREATE POLICY "Doctors read policy" ON public.doctors
    FOR SELECT USING (true);

CREATE POLICY "Doctors insert policy" ON public.doctors
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Doctors update policy" ON public.doctors
    FOR UPDATE USING (true);

CREATE POLICY "Doctors delete policy" ON public.doctors
    FOR DELETE USING (true);

-- ----------------------------------------------------------------------------
-- SPECIALITIES RLS
-- ----------------------------------------------------------------------------
CREATE POLICY "Specialities read policy" ON public.specialities
    FOR SELECT USING (true);

CREATE POLICY "Specialities insert policy" ON public.specialities
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Specialities update policy" ON public.specialities
    FOR UPDATE USING (true);

CREATE POLICY "Specialities delete policy" ON public.specialities
    FOR DELETE USING (true);

-- ----------------------------------------------------------------------------
-- CLINICS RLS
-- ----------------------------------------------------------------------------
CREATE POLICY "Clinics read policy" ON public.clinics
    FOR SELECT USING (true);

CREATE POLICY "Clinics insert policy" ON public.clinics
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Clinics update policy" ON public.clinics
    FOR UPDATE USING (true);

CREATE POLICY "Clinics delete policy" ON public.clinics
    FOR DELETE USING (true);

-- ----------------------------------------------------------------------------
-- DOCTOR SLOTS RLS
-- ----------------------------------------------------------------------------
CREATE POLICY "Doctor Slots read policy" ON public.doctor_slots
    FOR SELECT USING (true);

CREATE POLICY "Doctor Slots insert policy" ON public.doctor_slots
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Doctor Slots update policy" ON public.doctor_slots
    FOR UPDATE USING (true);

CREATE POLICY "Doctor Slots delete policy" ON public.doctor_slots
    FOR DELETE USING (true);

-- ----------------------------------------------------------------------------
-- DOCTOR SCHEDULES RLS
-- ----------------------------------------------------------------------------
CREATE POLICY "Doctor Schedules read policy" ON public.doctor_schedules
    FOR SELECT USING (true);

CREATE POLICY "Doctor Schedules insert policy" ON public.doctor_schedules
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Doctor Schedules update policy" ON public.doctor_schedules
    FOR UPDATE USING (true);

CREATE POLICY "Doctor Schedules delete policy" ON public.doctor_schedules
    FOR DELETE USING (true);

-- ----------------------------------------------------------------------------
-- APPOINTMENTS RLS
-- ----------------------------------------------------------------------------
CREATE POLICY "Appointments read policy" ON public.appointments
    FOR SELECT USING (true);

CREATE POLICY "Appointments insert policy" ON public.appointments
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Appointments update policy" ON public.appointments
    FOR UPDATE USING (true);

CREATE POLICY "Appointments delete policy" ON public.appointments
    FOR DELETE USING (true);

-- ----------------------------------------------------------------------------
-- DOCTOR REVIEWS RLS
-- ----------------------------------------------------------------------------
CREATE POLICY "Doctor Reviews read policy" ON public.doctor_reviews
    FOR SELECT USING (true);

CREATE POLICY "Doctor Reviews insert policy" ON public.doctor_reviews
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Doctor Reviews update policy" ON public.doctor_reviews
    FOR UPDATE USING (true);

CREATE POLICY "Doctor Reviews delete policy" ON public.doctor_reviews
    FOR DELETE USING (true);

-- ----------------------------------------------------------------------------
-- MEDICAL RECORDS RLS
-- ----------------------------------------------------------------------------
CREATE POLICY "Medical Records read policy" ON public.medical_records
    FOR SELECT USING (true);

CREATE POLICY "Medical Records insert policy" ON public.medical_records
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Medical Records update policy" ON public.medical_records
    FOR UPDATE USING (true);

CREATE POLICY "Medical Records delete policy" ON public.medical_records
    FOR DELETE USING (true);

-- ----------------------------------------------------------------------------
-- NOTIFICATIONS RLS
-- ----------------------------------------------------------------------------
CREATE POLICY "Notifications read policy" ON public.notifications
    FOR SELECT USING (true);

CREATE POLICY "Notifications insert policy" ON public.notifications
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Notifications update policy" ON public.notifications
    FOR UPDATE USING (true);

CREATE POLICY "Notifications delete policy" ON public.notifications
    FOR DELETE USING (true);

-- ----------------------------------------------------------------------------
-- REFUND REQUESTS RLS
-- ----------------------------------------------------------------------------
CREATE POLICY "Refund Requests read policy" ON public.refund_requests
    FOR SELECT USING (true);

CREATE POLICY "Refund Requests insert policy" ON public.refund_requests
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Refund Requests update policy" ON public.refund_requests
    FOR UPDATE USING (true);

CREATE POLICY "Refund Requests delete policy" ON public.refund_requests
    FOR DELETE USING (true);

-- ----------------------------------------------------------------------------
-- WITHDRAWALS RLS
-- ----------------------------------------------------------------------------
CREATE POLICY "Withdrawals read policy" ON public.withdrawals
    FOR SELECT USING (true);

CREATE POLICY "Withdrawals insert policy" ON public.withdrawals
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Withdrawals update policy" ON public.withdrawals
    FOR UPDATE USING (true);

CREATE POLICY "Withdrawals delete policy" ON public.withdrawals
    FOR DELETE USING (true);

-- ----------------------------------------------------------------------------
-- COMMISSION SETTINGS RLS
-- ----------------------------------------------------------------------------
CREATE POLICY "Commission Settings read policy" ON public.commission_settings
    FOR SELECT USING (true);

CREATE POLICY "Commission Settings insert policy" ON public.commission_settings
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Commission Settings update policy" ON public.commission_settings
    FOR UPDATE USING (true);

CREATE POLICY "Commission Settings delete policy" ON public.commission_settings
    FOR DELETE USING (true);


-- ============================================================================
-- 7. STORAGE BUCKETS SETUP FOR MEDICAL RECORDS & AVATARS
-- ============================================================================
INSERT INTO storage.buckets (id, name, public)
VALUES
    ('medical-records', 'medical-records', true),
    ('avatars', 'avatars', true),
    ('doctor-images', 'doctor-images', true)
ON CONFLICT (id) DO UPDATE SET public = true;

-- Storage RLS Policies
CREATE POLICY "Public Read Storage Access" ON storage.objects
    FOR SELECT USING (true);

CREATE POLICY "Public Upload Storage Access" ON storage.objects
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Public Update Storage Access" ON storage.objects
    FOR UPDATE USING (true);

CREATE POLICY "Public Delete Storage Access" ON storage.objects
    FOR DELETE USING (true);


-- ============================================================================
-- 8. INITIAL SEED DATA
-- ============================================================================

-- Seed Commission Settings
INSERT INTO public.commission_settings (
    setting_key, new_patient_commission, new_patient_type, followup_commission, followup_type, notes
) VALUES (
    'global_default', 10.0, 'percentage', 5.0, 'percentage', 'Global platform commission rate defaults'
) ON CONFLICT (setting_key) DO UPDATE SET
    new_patient_commission = EXCLUDED.new_patient_commission,
    followup_commission = EXCLUDED.followup_commission;

-- Seed Specialities
INSERT INTO public.specialities (id, title, name, icon_url) VALUES
    ('10000000-0000-0000-0000-000000000001', 'General Physician', 'General Physician', 'https://cdn-icons-png.flaticon.com/512/387/387561.png'),
    ('10000000-0000-0000-0000-000000000002', 'Women''s Health', 'Women''s Health', 'https://cdn-icons-png.flaticon.com/512/2966/2966327.png'),
    ('10000000-0000-0000-0000-000000000003', 'Skin Specialist', 'Skin Specialist', 'https://cdn-icons-png.flaticon.com/512/2818/2818366.png'),
    ('10000000-0000-0000-0000-000000000004', 'Dentist', 'Dentist', 'https://cdn-icons-png.flaticon.com/512/2818/2818305.png'),
    ('10000000-0000-0000-0000-000000000005', 'Eye Specialist', 'Eye Specialist', 'https://cdn-icons-png.flaticon.com/512/3004/3004416.png'),
    ('10000000-0000-0000-0000-000000000006', 'Ear, Nose & Throat', 'Ear, Nose & Throat', 'https://cdn-icons-png.flaticon.com/512/3063/3063822.png'),
    ('10000000-0000-0000-0000-000000000007', 'Child Care', 'Child Care', 'https://cdn-icons-png.flaticon.com/512/3050/3050525.png'),
    ('10000000-0000-0000-0000-000000000008', 'Heart Care', 'Heart Care', 'https://cdn-icons-png.flaticon.com/512/822/822123.png')
ON CONFLICT (id) DO UPDATE SET title = EXCLUDED.title, icon_url = EXCLUDED.icon_url;

-- Seed Sample Doctors
INSERT INTO public.doctors (
    id, name, qualification, specialization, experience, rating, review_count,
    clinic_name, location, state, fee, followup_fee, image_url, about,
    doctor_phone, reception_phone, gender, is_verified, is_available_today, is_nearby
) VALUES
    (
        'doc_1', 'Dr. Priya Sharma', 'MBBS, MD - Dermatology', 'Skin Specialist',
        '8+ Years Exp', 4.9, 128, 'Skin Care Clinic', 'Koramangala, Bangalore', 'Karnataka',
        600, 350, 'https://images.unsplash.com/photo-1559839734-2b71ea197ec2?auto=format&fit=crop&q=80&w=400',
        'Dr. Priya Sharma is a renowned dermatologist with extensive expertise in skin treatments, laser therapy, and cosmetic dermatology.',
        '9876543210', '080-12345678', 'Female', true, true, true
    ),
    (
        'doc_2', 'Dr. Rajesh Verma', 'MBBS, MD - General Medicine', 'General Physician',
        '12+ Years Exp', 4.8, 210, 'HealthFirst Clinic', 'Indiranagar, Bangalore', 'Karnataka',
        500, 300, 'https://images.unsplash.com/photo-1622253692010-333f2da6031d?auto=format&fit=crop&q=80&w=400',
        'Dr. Rajesh Verma specializes in general medicine, diabetes management, and preventive healthcare.',
        '9876543211', '080-87654321', 'Male', true, true, true
    ),
    (
        'doc_3', 'Dr. Ananya Roy', 'MBBS, DNB - Obstetrics & Gynaecology', 'Women''s Health',
        '10+ Years Exp', 4.9, 185, 'Motherhood Care', 'HSR Layout, Bangalore', 'Karnataka',
        700, 400, 'https://images.unsplash.com/photo-1594824813566-88855ce7890f?auto=format&fit=crop&q=80&w=400',
        'Dr. Ananya Roy offers comprehensive gynecological care, prenatal counseling, and women health services.',
        '9876543212', '080-23456789', 'Female', true, true, true
    ),
    (
        'doc_4', 'Dr. Amit Patel', 'BDS, MDS - Orthodontics', 'Dentist',
        '6+ Years Exp', 4.7, 95, 'Smile Craft Clinic', 'BTM Layout, Bangalore', 'Karnataka',
        400, 250, 'https://images.unsplash.com/photo-1612349317150-e413f6a5b16d?auto=format&fit=crop&q=80&w=400',
        'Dr. Amit Patel provides advanced dental care, root canal treatments, and cosmetic dentistry.',
        '9876543213', '080-34567890', 'Male', true, true, true
    ),
    (
        'doc_5', 'Dr. Sneha Kulkarni', 'MBBS, DCH - Paediatrics', 'Child Care',
        '9+ Years Exp', 4.9, 142, 'Kids Health Clinic', 'Jayanagar, Bangalore', 'Karnataka',
        550, 300, 'https://images.unsplash.com/photo-1582750433449-648ed127bb54?auto=format&fit=crop&q=80&w=400',
        'Dr. Sneha Kulkarni is a dedicated pediatrician providing compassionate healthcare for newborns and children.',
        '9876543214', '080-45678901', 'Female', true, true, true
    ),
    (
        'doc_6', 'Dr. Alok Kumar', 'MBBS, MS - ENT', 'Ear, Nose & Throat',
        '14+ Years Exp', 4.8, 160, 'Patna Care Clinic', 'Boring Road, Patna', 'Bihar',
        500, 300, 'https://images.unsplash.com/photo-1537368910025-700350fe46c7?auto=format&fit=crop&q=80&w=400',
        'Dr. Alok Kumar is a senior ENT specialist treating sinus disorders, hearing issues, and throat ailments.',
        '9876543215', '0612-123456', 'Male', true, true, true
    )
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    specialization = EXCLUDED.specialization,
    clinic_name = EXCLUDED.clinic_name,
    fee = EXCLUDED.fee;

-- Seed Clinics for Doctors
INSERT INTO public.clinics (
    id, doctor_id, clinic_name, address, city, state, pincode, phone, is_active
) VALUES
    ('c1000000-0000-0000-0000-000000000001', 'doc_1', 'Skin Care Clinic', '100 Feet Road, Koramangala', 'Bangalore', 'Karnataka', '560034', '080-12345678', true),
    ('c1000000-0000-0000-0000-000000000002', 'doc_2', 'HealthFirst Clinic', '12th Main Road, Indiranagar', 'Bangalore', 'Karnataka', '560038', '080-87654321', true),
    ('c1000000-0000-0000-0000-000000000003', 'doc_3', 'Motherhood Care', '27th Main Road, HSR Layout', 'Bangalore', 'Karnataka', '560102', '080-23456789', true),
    ('c1000000-0000-0000-0000-000000000004', 'doc_4', 'Smile Craft Clinic', 'Outer Ring Road, BTM Layout', 'Bangalore', 'Karnataka', '560076', '080-34567890', true),
    ('c1000000-0000-0000-0000-000000000005', 'doc_5', 'Kids Health Clinic', '4th Block, Jayanagar', 'Bangalore', 'Karnataka', '560011', '080-45678901', true),
    ('c1000000-0000-0000-0000-000000000006', 'doc_6', 'Patna Care Clinic', 'Boring Road Crossing', 'Patna', 'Bihar', '800001', '0612-123456', true)
ON CONFLICT (id) DO UPDATE SET clinic_name = EXCLUDED.clinic_name;

-- ============================================================================
-- SETUP COMPLETE!
-- You can run this file directly in Supabase SQL Editor.
-- ============================================================================
