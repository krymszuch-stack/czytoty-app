-- ==============================================================================
-- CZYTOTY? - SUPABASE AUTH & USER PROFILES MIGRATION
-- Migration: 20260907_create_profiles_table.sql
-- Description:
-- 1. Tworzy tabelę `public.profiles` powiązaną 1:1 z `auth.users(id)`.
-- 2. Włącza Row Level Security (RLS) z restrykcyjnymi politykami dla użytkownika.
-- 3. Tworzy funkcję i trigger `on_auth_user_created`, automatycznie pobierający
--    dane z metadanych Google OAuth (imię i nazwisko, awatar, email).
-- ==============================================================================

-- 1. Utworzenie tabeli profili użytkowników
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    display_name TEXT,
    avatar_url TEXT,
    country_code TEXT DEFAULT 'PL',
    locale TEXT DEFAULT 'pl_PL',
    onboarding_completed BOOLEAN DEFAULT TRUE,
    reputation_contribution_enabled BOOLEAN DEFAULT TRUE,
    analytics_consent BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc'::text, NOW()) NOT NULL
);

-- Komentarze opisowe dla kolumn tabeli
COMMENT ON TABLE public.profiles IS 'Profile użytkowników CzyToTy? powiązane z kontem Supabase Auth (Google)';
COMMENT ON COLUMN public.profiles.id IS 'Klucz obcy odpowiadający identyfikatorowi auth.users(id)';
COMMENT ON COLUMN public.profiles.display_name IS 'Wyświetlana nazwa użytkownika pobrana z Google OAuth';
COMMENT ON COLUMN public.profiles.avatar_url IS 'Bezpośredni URL do zdjęcia profilowego użytkownika z Google';
COMMENT ON COLUMN public.profiles.reputation_contribution_enabled IS 'Czy oceny numerów telefonu użytkownika wpływają na algorytm reputacji';

-- 2. Konfiguracja Row Level Security (RLS)
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

-- Polityka: Każdy zalogowany użytkownik ma prawo odczytać wyłącznie swój profil
DROP POLICY IF EXISTS "Users can view own profile" ON public.profiles;
CREATE POLICY "Users can view own profile"
    ON public.profiles
    FOR SELECT
    USING (auth.uid() = id);

-- Polityka: Użytkownik ma prawo edytować wyłącznie swój profil
DROP POLICY IF EXISTS "Users can update own profile" ON public.profiles;
CREATE POLICY "Users can update own profile"
    ON public.profiles
    FOR UPDATE
    USING (auth.uid() = id);

-- Polityka: Użytkownik ma prawo wstawić swój profil (fallback dla logowania klienta)
DROP POLICY IF EXISTS "Users can insert own profile" ON public.profiles;
CREATE POLICY "Users can insert own profile"
    ON public.profiles
    FOR INSERT
    WITH CHECK (auth.uid() = id);

-- 3. Automatyczna funkcja wyzwalacza przy rejestracji/logowaniu użytkownika
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_display_name TEXT;
    v_avatar_url TEXT;
BEGIN
    -- Ekstrakcja nazwy użytkownika z metadanych Google OAuth
    v_display_name := COALESCE(
        NEW.raw_user_meta_data->>'full_name',
        NEW.raw_user_meta_data->>'name',
        split_part(NEW.email, '@', 1),
        'Użytkownik'
    );

    -- Ekstrakcja adresu awatara z metadanych Google OAuth
    v_avatar_url := COALESCE(
        NEW.raw_user_meta_data->>'avatar_url',
        NEW.raw_user_meta_data->>'picture',
        NULL
    );

    INSERT INTO public.profiles (
        id,
        display_name,
        avatar_url,
        country_code,
        locale,
        onboarding_completed,
        reputation_contribution_enabled,
        analytics_consent,
        created_at,
        updated_at
    )
    VALUES (
        NEW.id,
        v_display_name,
        v_avatar_url,
        'PL',
        'pl_PL',
        TRUE,
        TRUE,
        FALSE,
        NOW(),
        NOW()
    )
    ON CONFLICT (id) DO UPDATE SET
        display_name = EXCLUDED.display_name,
        avatar_url = EXCLUDED.avatar_url,
        updated_at = NOW();

    RETURN NEW;
END;
$$;

-- 4. Rejestracja wyzwalacza po utworzeniu rekordu w auth.users
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_new_user();
