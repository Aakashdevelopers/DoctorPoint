-- ==========================================================
-- DOCTOR POINT - COMPLETE SUPABASE DATABASE SCHEMA UPDATE SCRIPT
-- Includes: Profiles, Doctors, Doctor Slots, & Appointments
-- ==========================================================
-- Run this script in your Supabase SQL Editor:
-- https://supabase.com/dashboard/project/kewuxbvilvqxphmvyhkz/sql
-- ==========================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ----------------------------------------------------------
-- 1. PROFILES TABLE (PATIENT & USER ACCOUNTS)
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS profiles (
    id UUID PRIMARY KEY,
    full_name TEXT,
    email TEXT,
    phone TEXT,
    role TEXT DEFAULT 'patient',
    avatar_url TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow public read access on profiles" ON profiles;
CREATE POLICY "Allow public read access on profiles" ON profiles FOR SELECT USING (true);

DROP POLICY IF EXISTS "Allow public insert access on profiles" ON profiles;
CREATE POLICY "Allow public insert access on profiles" ON profiles FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Allow public update access on profiles" ON profiles;
CREATE POLICY "Allow public update access on profiles" ON profiles FOR UPDATE USING (true);


-- ----------------------------------------------------------
-- 2. DOCTORS TABLE
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS doctors (
    id UUID PRIMARY KEY,
    user_id UUID,
    name TEXT NOT NULL,
    email VARCHAR(150),
    phone TEXT,
    image_url TEXT,            -- Profile Picture URL (ImgBB)
    experience TEXT,           -- e.g. '10+ Years'
    specialization TEXT,       -- Category / Specialization
    location TEXT,             -- Location / City
    clinic_name TEXT,          -- Clinic / Hospital Name
    clinic_photos TEXT,        -- Clinic / Hospital Photos (comma-separated ImgBB URLs)
    qualification TEXT,        -- Education / Qualification
    fee INT DEFAULT 500,       -- Consultation Fee (₹)
    about TEXT,                -- Bio / About Doctor
    rating NUMERIC(3, 2) DEFAULT 4.8,
    review_count INT DEFAULT 120,
    consult_type VARCHAR(100) DEFAULT 'In-Clinic & Video Consult',
    languages VARCHAR(200) DEFAULT 'English, Hindi',
    gender TEXT DEFAULT 'Doctor',
    is_verified BOOLEAN DEFAULT TRUE,
    is_available_today BOOLEAN DEFAULT TRUE,
    is_nearby BOOLEAN DEFAULT TRUE,
    is_popular BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Ensure all columns exist on doctors table
ALTER TABLE doctors
ADD COLUMN IF NOT EXISTS email VARCHAR(150),
ADD COLUMN IF NOT EXISTS phone TEXT,
ADD COLUMN IF NOT EXISTS image_url TEXT,
ADD COLUMN IF NOT EXISTS experience TEXT,
ADD COLUMN IF NOT EXISTS specialization TEXT,
ADD COLUMN IF NOT EXISTS location TEXT,
ADD COLUMN IF NOT EXISTS clinic_name TEXT,
ADD COLUMN IF NOT EXISTS clinic_photos TEXT,
ADD COLUMN IF NOT EXISTS qualification TEXT,
ADD COLUMN IF NOT EXISTS fee INT DEFAULT 500,
ADD COLUMN IF NOT EXISTS about TEXT,
ADD COLUMN IF NOT EXISTS consult_type VARCHAR(100) DEFAULT 'In-Clinic & Video Consult',
ADD COLUMN IF NOT EXISTS languages VARCHAR(200) DEFAULT 'English, Hindi',
ADD COLUMN IF NOT EXISTS gender TEXT DEFAULT 'Doctor',
ADD COLUMN IF NOT EXISTS is_verified BOOLEAN DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS is_available_today BOOLEAN DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS is_nearby BOOLEAN DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS is_popular BOOLEAN DEFAULT TRUE,
ADD COLUMN IF NOT EXISTS morning_start TIME DEFAULT '09:00:00',
ADD COLUMN IF NOT EXISTS morning_end TIME DEFAULT '13:00:00',
ADD COLUMN IF NOT EXISTS evening_start TIME DEFAULT '17:00:00',
ADD COLUMN IF NOT EXISTS evening_end TIME DEFAULT '20:00:00';

CREATE INDEX IF NOT EXISTS idx_doctors_email ON doctors(email);
CREATE INDEX IF NOT EXISTS idx_doctors_specialization ON doctors(specialization);

ALTER TABLE doctors ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow public read access on doctors" ON doctors;
CREATE POLICY "Allow public read access on doctors" ON doctors FOR SELECT USING (true);

DROP POLICY IF EXISTS "Allow doctor insert access on doctors" ON doctors;
CREATE POLICY "Allow doctor insert access on doctors" ON doctors FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Allow doctor update access on doctors" ON doctors;
CREATE POLICY "Allow doctor update access on doctors" ON doctors FOR UPDATE USING (true);


-- ----------------------------------------------------------
-- 3. DOCTOR SLOTS TABLE (SCHEDULE GENERATOR & AVAILABILITY)
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS doctor_slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id UUID REFERENCES doctors(id) ON DELETE CASCADE,
    clinic_id UUID,
    slot_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    session TEXT DEFAULT 'morning',
    status TEXT DEFAULT 'available', -- 'available', 'booked', 'blocked'
    max_patients INT DEFAULT 1,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE doctor_slots
ADD COLUMN IF NOT EXISTS doctor_id UUID REFERENCES doctors(id) ON DELETE CASCADE,
ADD COLUMN IF NOT EXISTS slot_date DATE,
ADD COLUMN IF NOT EXISTS start_time TIME,
ADD COLUMN IF NOT EXISTS end_time TIME,
ADD COLUMN IF NOT EXISTS session TEXT DEFAULT 'morning',
ADD COLUMN IF NOT EXISTS status TEXT DEFAULT 'available',
ADD COLUMN IF NOT EXISTS max_patients INT DEFAULT 1;

CREATE INDEX IF NOT EXISTS idx_slots_doctor_date ON doctor_slots(doctor_id, slot_date);
CREATE INDEX IF NOT EXISTS idx_slots_session ON doctor_slots(session);

ALTER TABLE doctor_slots ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow public read access on doctor_slots" ON doctor_slots;
CREATE POLICY "Allow public read access on doctor_slots" ON doctor_slots FOR SELECT USING (true);

DROP POLICY IF EXISTS "Allow doctor insert access on doctor_slots" ON doctor_slots;
CREATE POLICY "Allow doctor insert access on doctor_slots" ON doctor_slots FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Allow doctor update access on doctor_slots" ON doctor_slots;
CREATE POLICY "Allow doctor update access on doctor_slots" ON doctor_slots FOR UPDATE USING (true);

DROP POLICY IF EXISTS "Allow doctor delete access on doctor_slots" ON doctor_slots;
CREATE POLICY "Allow doctor delete access on doctor_slots" ON doctor_slots FOR DELETE USING (true);


-- ----------------------------------------------------------
-- 4. APPOINTMENTS TABLE
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS appointments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id UUID REFERENCES doctors(id) ON DELETE CASCADE,
    patient_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    slot_id UUID REFERENCES doctor_slots(id) ON DELETE SET NULL,
    appointment_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    appointment_type TEXT DEFAULT 'In-Clinic',
    token_number INT DEFAULT 1,
    status TEXT DEFAULT 'Confirmed', -- 'Pending', 'Confirmed', 'In Consultation', 'Completed', 'Cancelled'
    clinic_name TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE appointments
ADD COLUMN IF NOT EXISTS clinic_name TEXT,
ADD COLUMN IF NOT EXISTS token_number INT DEFAULT 1,
ADD COLUMN IF NOT EXISTS status TEXT DEFAULT 'Confirmed';

CREATE INDEX IF NOT EXISTS idx_appts_doctor_date ON appointments(doctor_id, appointment_date);

ALTER TABLE appointments ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow public read access on appointments" ON appointments;
CREATE POLICY "Allow public read access on appointments" ON appointments FOR SELECT USING (true);

DROP POLICY IF EXISTS "Allow insert access on appointments" ON appointments;
CREATE POLICY "Allow insert access on appointments" ON appointments FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Allow update access on appointments" ON appointments;
CREATE POLICY "Allow update access on appointments" ON appointments FOR UPDATE USING (true);

DROP POLICY IF EXISTS "Allow delete access on appointments" ON appointments;
CREATE POLICY "Allow delete access on appointments" ON appointments FOR DELETE USING (true);


SELECT 'Supabase Doctor Point database schema updated successfully!' AS status;
