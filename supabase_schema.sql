-- ==========================================================
-- DOCTOR POINT - SUPABASE POSTGRESQL DATABASE SCHEMA SCRIPT
-- ==========================================================
-- Run this script in your Supabase SQL Editor:
-- https://supabase.com/dashboard/project/kewuxbvilvqxphmvyhkz/sql
-- ==========================================================

-- 1. DROP EXISTING TABLES IF NEEDED
DROP TABLE IF EXISTS appointments CASCADE;
DROP TABLE IF EXISTS doctors CASCADE;
DROP TABLE IF EXISTS specialities CASCADE;

-- 2. CREATE SPECIALITIES TABLE
CREATE TABLE specialities (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    icon_url TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. CREATE DOCTORS TABLE
CREATE TABLE doctors (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150),
    qualification VARCHAR(200),
    specialization VARCHAR(150),
    experience VARCHAR(50),
    rating NUMERIC(3, 2) DEFAULT 4.5,
    total_reviews INT DEFAULT 0,
    clinic_name VARCHAR(200),
    location VARCHAR(200),
    fee INT DEFAULT 500,
    image_url TEXT,
    clinic_photos TEXT,
    is_available_today BOOLEAN DEFAULT TRUE,
    gender VARCHAR(20) DEFAULT 'Female',
    is_nearby BOOLEAN DEFAULT TRUE,
    is_popular BOOLEAN DEFAULT TRUE,
    about TEXT,
    consult_type VARCHAR(100) DEFAULT 'In-Clinic & Video Consult',
    languages VARCHAR(200) DEFAULT 'English, Hindi',
    phone VARCHAR(20) DEFAULT '9876543210',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. CREATE APPOINTMENTS TABLE
CREATE TABLE appointments (
    id VARCHAR(100) PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    doctor_id VARCHAR(50) REFERENCES doctors(id) ON DELETE CASCADE,
    doctor_name VARCHAR(150),
    doctor_specialization VARCHAR(150),
    appointment_date VARCHAR(50) NOT NULL,
    appointment_time VARCHAR(50) NOT NULL,
    clinic_name VARCHAR(200),
    clinic_location VARCHAR(200),
    status VARCHAR(50) DEFAULT 'Confirmed',
    fee INT DEFAULT 500,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. ENABLE ROW LEVEL SECURITY (RLS) & SET POLICIES
ALTER TABLE specialities ENABLE ROW LEVEL SECURITY;
ALTER TABLE doctors ENABLE ROW LEVEL SECURITY;
ALTER TABLE appointments ENABLE ROW LEVEL SECURITY;

-- Allow anonymous & authenticated users to read specialities & doctors
CREATE POLICY "Allow public read access on specialities" ON specialities FOR SELECT USING (true);
CREATE POLICY "Allow public read access on doctors" ON doctors FOR SELECT USING (true);
CREATE POLICY "Allow doctor insert access on doctors" ON doctors FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow doctor update access on doctors" ON doctors FOR UPDATE USING (true);

-- Allow anonymous & authenticated users to read, insert, update appointments
CREATE POLICY "Allow read access on appointments" ON appointments FOR SELECT USING (true);
CREATE POLICY "Allow insert access on appointments" ON appointments FOR INSERT WITH CHECK (true);
CREATE POLICY "Allow update access on appointments" ON appointments FOR UPDATE USING (true);

-- 6. INSERT SAMPLE SPECIALITIES DATA
INSERT INTO specialities (name) VALUES
('General Physician'),
('Skin & Hair'),
('Women''s Health'),
('Dental Care'),
('Child Care'),
('ENT'),
('Mental Health'),
('Heart Care');

-- 7. INSERT SAMPLE DOCTORS DATA
INSERT INTO doctors (
    id, name, email, qualification, specialization, experience, rating, total_reviews,
    clinic_name, location, fee, image_url, is_available_today, gender, is_nearby, is_popular, about, consult_type, languages
) VALUES
('doc_1', 'Dr. Priya Sharma', 'priya.sharma@doctorpoint.com', 'MBBS, MD - Dermatology', 'Dermatology (Skin & Hair)', '12 Yrs Exp', 4.8, 120, 'Skin Care Clinic', 'Indiranagar, Bangalore', 900, 'https://images.unsplash.com/photo-1559839734-2b71ea197ec2?auto=format&fit=crop&q=80&w=200', true, 'Female', true, true, 'Dr. Priya Sharma is a senior dermatologist with over 12 years of experience in skin and hair care.', 'In-Clinic & Video Consult', 'English, Hindi, Kannada'),
('doc_2', 'Dr. Rajesh Kumar', 'rajesh.kumar@doctorpoint.com', 'MBBS, MS - Cardiology', 'Cardiology (Heart Care)', '15 Yrs Exp', 4.9, 210, 'Heart Care Centre', 'Koramangala, Bangalore', 1200, 'https://images.unsplash.com/photo-1622253692010-333f2da6031d?auto=format&fit=crop&q=80&w=200', true, 'Male', true, false, 'Dr. Rajesh Kumar specializes in cardiovascular treatments and preventive heart care with 15 years experience.', 'In-Clinic & Video Consult', 'English, Hindi, Telugu'),
('doc_3', 'Dr. Ananya Rao', 'ananya.rao@doctorpoint.com', 'MBBS, DGO - Gynaecology', 'Gynaecology (Women''s Health)', '10 Yrs Exp', 4.7, 95, 'Motherhood Hospital', 'HSR Layout, Bangalore', 800, 'https://images.unsplash.com/photo-1594824813566-78a931a2935e?auto=format&fit=crop&q=80&w=200', true, 'Female', false, true, 'Dr. Ananya Rao is an expert gynaecologist specializing in women''s healthcare and maternal medicine.', 'In-Clinic & Video Consult', 'English, Hindi, Kannada'),
('doc_4', 'Dr. Vikram Malhotra', 'vikram.malhotra@doctorpoint.com', 'BDS, MDS - Orthodontics', 'Dental Care', '8 Yrs Exp', 4.6, 85, 'Smile Dental Care', 'Whitefield, Bangalore', 700, 'https://images.unsplash.com/photo-1537368910025-700350fe46c7?auto=format&fit=crop&q=80&w=200', false, 'Male', true, true, 'Dr. Vikram Malhotra offers comprehensive dental treatments and orthodontics.', 'In-Clinic Visit Only', 'English, Hindi'),
('doc_5', 'Dr. Sunita Patel', 'sunita.patel@doctorpoint.com', 'MD - Pediatrics', 'Pediatrics (Child Care)', '14 Yrs Exp', 4.9, 180, 'Kids Health Clinic', 'Jayanagar, Bangalore', 1000, 'https://images.unsplash.com/photo-1559839734-2b71ea197ec2?auto=format&fit=crop&q=80&w=200', true, 'Female', true, false, 'Dr. Sunita Patel has 14 years of dedicated experience in child care and pediatric nutrition.', 'In-Clinic & Video Consult', 'English, Hindi, Gujarati');

-- 8. INSERT SAMPLE APPOINTMENTS DATA
INSERT INTO appointments (
    id, user_id, doctor_id, doctor_name, doctor_specialization, appointment_date, appointment_time, clinic_name, clinic_location, status, fee
) VALUES
('appt_1', 'user_default', 'doc_1', 'Dr. Priya Sharma', 'Dermatology (Skin & Hair)', '05 Sep', '10:30 AM', 'Skin Care Clinic', 'Indiranagar, Bangalore', 'Confirmed', 900),
('appt_2', 'user_default', 'doc_2', 'Dr. Rajesh Kumar', 'Cardiology (Heart Care)', '12 Sep', '02:15 PM', 'Heart Care Centre', 'Koramangala, Bangalore', 'Confirmed', 1200);

-- Query confirmation
SELECT 'Database schema and initial data created successfully!' AS status;
