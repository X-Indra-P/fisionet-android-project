-- ============================================================
-- MIGRATION: Klik Fisio Terapi — Fitur Multi-Cabang & Status Baru
-- Jalankan di: Supabase Dashboard > SQL Editor
-- ============================================================

-- ================================================================
-- 1. TABEL: profiles
--    Tambah kolom 'clinic' (sudah ada) — pastikan ada
--    Update constraint status untuk mendukung 'inactive' & 'suspended'
-- ================================================================

-- 1a. Tambah kolom clinic jika belum ada
ALTER TABLE public.profiles
ADD COLUMN IF NOT EXISTS clinic TEXT;

-- 1b. Hapus constraint lama, tambah constraint baru dengan status inactive & suspended
ALTER TABLE public.profiles
DROP CONSTRAINT IF EXISTS profiles_status_check;

ALTER TABLE public.profiles
ADD CONSTRAINT profiles_status_check
CHECK (status IN ('pending', 'verified', 'rejected', 'inactive', 'suspended'));

-- 1c. Update trigger agar menyertakan clinic (null saat register, diisi admin kemudian)
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER SET search_path = public
AS $$
BEGIN
  INSERT INTO public.profiles (id, display_name, role, status, clinic)
  VALUES (
    NEW.id,
    NEW.raw_user_meta_data ->> 'display_name',
    2,         -- Default role: 2 (Therapist)
    'pending', -- Default status: menunggu verifikasi admin
    NULL       -- Clinic: diisi admin setelah approve
  );
  RETURN NEW;
END;
$$;

-- ================================================================
-- 2. TABEL: appointments
--    Tambah kolom 'clinic' untuk filter per cabang
-- ================================================================

ALTER TABLE public.appointments
ADD COLUMN IF NOT EXISTS clinic TEXT;

-- Index agar query filter per clinic cepat
CREATE INDEX IF NOT EXISTS idx_appointments_clinic
ON public.appointments (clinic);

-- ================================================================
-- 3. TABEL: transactions
--    Pastikan kolom 'cabang' sudah ada (untuk filter per cabang)
--    Tambah kolom 'xendit_id' untuk payment gateway
-- ================================================================

-- 3a. Pastikan cabang ada
ALTER TABLE public.transactions
ADD COLUMN IF NOT EXISTS cabang TEXT;

-- 3b. Pastikan xendit_id ada (untuk payment gateway Xendit)
ALTER TABLE public.transactions
ADD COLUMN IF NOT EXISTS xendit_id TEXT;

-- 3c. Pastikan payment_status ada dengan default 'pending'
ALTER TABLE public.transactions
ADD COLUMN IF NOT EXISTS payment_status TEXT DEFAULT 'pending';

-- 3d. Pastikan user_name ada (nama terapis snapshot saat transaksi dibuat)
ALTER TABLE public.transactions
ADD COLUMN IF NOT EXISTS user_name VARCHAR(255);

-- 3e. Pastikan package_id ada
ALTER TABLE public.transactions
ADD COLUMN IF NOT EXISTS package_id SMALLINT REFERENCES packages(id);

-- 3f. Index agar query filter per cabang cepat
CREATE INDEX IF NOT EXISTS idx_transactions_cabang
ON public.transactions (cabang);

-- 3g. Index filter by date (untuk laporan bulanan)
CREATE INDEX IF NOT EXISTS idx_transactions_date
ON public.transactions (date);

-- ================================================================
-- 4. IZIN UPDATE untuk Admin (admin bisa update profile terapis)
-- ================================================================

-- Hapus policy lama yang terlalu terbatas
DROP POLICY IF EXISTS "Users can update their own profile" ON public.profiles;
DROP POLICY IF EXISTS "Admin can update any profile" ON public.profiles;

-- User bisa update profil sendiri
CREATE POLICY "Users can update their own profile"
  ON public.profiles FOR UPDATE
  TO authenticated
  USING (auth.uid() = id);

-- Admin (role=1) bisa update profil terapis mana saja
CREATE POLICY "Admin can update any profile"
  ON public.profiles FOR UPDATE
  TO authenticated
  USING (
    EXISTS (
      SELECT 1 FROM public.profiles
      WHERE id = auth.uid() AND role = 1
    )
  );

-- ================================================================
-- 5. VERIFIKASI — Cek semua kolom sudah ada
-- ================================================================

-- Uncomment untuk verifikasi:
-- SELECT column_name, data_type FROM information_schema.columns
-- WHERE table_name = 'profiles' ORDER BY ordinal_position;

-- SELECT column_name, data_type FROM information_schema.columns
-- WHERE table_name = 'appointments' ORDER BY ordinal_position;

-- SELECT column_name, data_type FROM information_schema.columns
-- WHERE table_name = 'transactions' ORDER BY ordinal_position;
