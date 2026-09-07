-- ==============================================================================
-- CZYTOTY? - DATA ACQUISITION & REPUTATION GRAPH PIPELINE
-- Migration: 20260907_create_data_acquisition_pipeline.sql
-- Description:
-- Kompleksowy schemat bazy danych dla audytowalnego, bezpiecznego i zgodnego
-- z prawem potoku pozyskiwania danych o numerach telefonów, organizacjach,
-- nadpisach SMS (Sender ID), domenach oraz sygnałach reputacyjnych.
--
-- Zasada naczelna: LEGAL ACCESS > DATA VOLUME
-- ==============================================================================

-- Włączenie rozszerzenia pgcrypto do operacji kryptograficznych i UUID
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ==============================================================================
-- 1. data_sources: Rejestr i audyt prawny wszystkich źródeł danych
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.data_sources (
    id VARCHAR(64) PRIMARY KEY,                         -- np. 'cert_polska', 'uke_tzn', 'csirt_nask_sender_id'
    name VARCHAR(255) NOT NULL,
    source_type VARCHAR(64) NOT NULL,                   -- 'SECURITY_FEED', 'PUBLIC_REGISTRY', 'FIRST_PARTY_COMMUNITY', 'COMMERCIAL_PLATFORM'
    base_url TEXT NOT NULL,
    license_type VARCHAR(128) NOT NULL,                 -- 'OPEN_DATA_CC0', 'STATUTORY_TASK', 'FIRST_PARTY', 'PROPRIETARY'
    commercial_use_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    redistribution_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    attribution_required BOOLEAN NOT NULL DEFAULT TRUE,
    attribution_text TEXT,
    robots_status VARCHAR(32) NOT NULL DEFAULT 'ALLOW', -- 'ALLOW', 'DISALLOW', 'NOT_APPLICABLE'
    api_available BOOLEAN NOT NULL DEFAULT TRUE,
    legal_status VARCHAR(32) NOT NULL,                  -- 'APPROVED', 'APPROVED_WITH_CONDITIONS', 'NEEDS_PERMISSION', 'API_ONLY', 'DO NOT INGEST'
    reviewed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    review_notes TEXT,
    update_frequency VARCHAR(64) NOT NULL DEFAULT 'DAILY',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ==============================================================================
-- 2. organizations: Zweryfikowane podmioty prawne i instytucje
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    legal_name VARCHAR(255),
    tax_id VARCHAR(32),                                 -- NIP
    registration_id VARCHAR(32),                        -- REGON / KRS
    category VARCHAR(64) NOT NULL,                      -- 'BANK', 'HEALTHCARE', 'GOVERNMENT', 'TELECOM', 'COURIER', 'COMMERCE'
    country VARCHAR(8) NOT NULL DEFAULT 'PL',
    official_domains JSONB NOT NULL DEFAULT '[]'::jsonb,
    official_emails JSONB NOT NULL DEFAULT '[]'::jsonb,
    logo_url TEXT,
    verification_level VARCHAR(32) NOT NULL DEFAULT 'OFFICIAL', -- 'STATUTORY', 'OFFICIAL', 'COMMUNITY_VERIFIED'
    primary_source_id VARCHAR(64) REFERENCES public.data_sources(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_verified_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_organizations_tax_id ON public.organizations(tax_id);
CREATE INDEX IF NOT EXISTS idx_organizations_category ON public.organizations(category);

-- ==============================================================================
-- 3. organization_numbers: Numery telefonów przypisane do organizacji (E.164)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.organization_numbers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES public.organizations(id) ON DELETE CASCADE,
    normalized_number VARCHAR(32) NOT NULL,             -- format E.164, np. '+48221234567'
    department_name VARCHAR(128),                       -- 'Infolinia Główna', 'Biuro Obsługi Klienta', 'Windykacja'
    number_direction VARCHAR(32) NOT NULL DEFAULT 'BOTH', -- 'INBOUND_ONLY', 'OUTBOUND_ONLY', 'BOTH'
    is_spoofing_target BOOLEAN NOT NULL DEFAULT FALSE,  -- True dla infolinii bankowych podatnych na CLI Spoofing
    verification_level VARCHAR(32) NOT NULL DEFAULT 'OFFICIAL',
    source_id VARCHAR(64) REFERENCES public.data_sources(id),
    last_verified_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_org_number UNIQUE(organization_id, normalized_number)
);

CREATE INDEX IF NOT EXISTS idx_org_numbers_phone ON public.organization_numbers(normalized_number);

-- ==============================================================================
-- 4. sender_ids: Oficjalne i zastrzeżone nadpisy SMS (Sender ID)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.sender_ids (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id VARCHAR(32) NOT NULL UNIQUE,              -- np. 'e-US', 'Policja', 'ZUS', 'mObywatel'
    organization_id UUID REFERENCES public.organizations(id) ON DELETE SET NULL,
    is_statutory_protected BOOLEAN NOT NULL DEFAULT FALSE, -- Rejestr CSIRT NASK (UOZnK)
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',       -- 'ACTIVE', 'BLOCKED', 'QUARANTINED'
    source_id VARCHAR(64) REFERENCES public.data_sources(id),
    description TEXT,
    registered_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_verified_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sender_ids_name ON public.sender_ids(sender_id);

-- ==============================================================================
-- 5. domains: Domeny internetowe (Oficjalne marki oraz Ostrzeżenia CERT)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.domains (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    domain_name VARCHAR(255) NOT NULL UNIQUE,           -- punycode / lowercase
    category VARCHAR(32) NOT NULL,                      -- 'OFFICIAL_BRAND', 'PHISHING', 'FRAUD', 'MALWARE', 'SUSPICIOUS'
    organization_id UUID REFERENCES public.organizations(id) ON DELETE SET NULL,
    source_id VARCHAR(64) REFERENCES public.data_sources(id),
    is_active_threat BOOLEAN NOT NULL DEFAULT FALSE,    -- True dopóki CERT nie oznaczy DeleteDate
    threat_description TEXT,
    reported_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    removed_at TIMESTAMPTZ,
    last_verified_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_domains_name ON public.domains(domain_name);
CREATE INDEX IF NOT EXISTS idx_domains_threat ON public.domains(is_active_threat) WHERE is_active_threat = TRUE;

-- ==============================================================================
-- 6. phone_identities: Baza wiedzy o numerach w standardzie E.164 (UKE TZN)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.phone_identities (
    normalized_number VARCHAR(32) PRIMARY KEY,          -- E.164 (+48XXXXXXXXX)
    country_code VARCHAR(8) NOT NULL DEFAULT '48',
    operator_name VARCHAR(128),                         -- Przydział według UKE (np. 'Orange Polska S.A.')
    network_type VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN',-- 'PLMN' (Komórkowy), 'PSTN' (Stacjonarny), 'VOIP', 'PREMIUM_RATE', 'TOLL_FREE'
    prefix_range VARCHAR(32),                           -- Prefiks UKE, np. '48732'
    is_business_number BOOLEAN NOT NULL DEFAULT FALSE,
    is_premium_rate BOOLEAN NOT NULL DEFAULT FALSE,
    privacy_pseudonym_token VARCHAR(64),                -- HMAC-SHA256(secret, number) dla numerów prywatnych
    data_classification VARCHAR(32) NOT NULL DEFAULT 'PUBLIC_TELECOM', -- 'PUBLIC_BUSINESS', 'PUBLIC_TELECOM', 'PRIVATE_RESIDENTIAL'
    source_id VARCHAR(64) REFERENCES public.data_sources(id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_phone_identities_operator ON public.phone_identities(operator_name);
CREATE INDEX IF NOT EXISTS idx_phone_identities_type ON public.phone_identities(network_type);

-- ==============================================================================
-- 7. community_reports: Własny crowdsourcing CzyToTy? (Structured Labels)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.community_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    normalized_number VARCHAR(32) NOT NULL,
    pseudonym_token VARCHAR(64) NOT NULL,               -- HMAC-SHA256 dla ochrony prywatności
    reporter_id VARCHAR(128) NOT NULL,                  -- Identyfikator konta użytkownika / instalacji
    reporter_trust_score REAL NOT NULL DEFAULT 1.0,     -- Wiarygodność zgłaszającego (0.1 .. 2.0)
    category VARCHAR(32) NOT NULL,                      -- 'OK', 'ANNOYING', 'TELEMARKETING', 'BOT', 'SUSPECTED_SCAM', 'IMPERSONATION', 'OTHER'
    channel VARCHAR(16) NOT NULL DEFAULT 'CALL',        -- 'CALL', 'SMS'
    interaction_occurred BOOLEAN NOT NULL DEFAULT TRUE, -- Czy użytkownik faktycznie odebrał / otworzył SMS
    call_duration_seconds INT DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_community_reports_phone ON public.community_reports(normalized_number);
CREATE INDEX IF NOT EXISTS idx_community_reports_reporter ON public.community_reports(reporter_id);
CREATE INDEX IF NOT EXISTS idx_community_reports_created ON public.community_reports(created_at DESC);

-- ==============================================================================
-- 8. security_signals: Zdarzenia z feedów cyberbezpieczeństwa (CERT, KNF)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.security_signals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    signal_type VARCHAR(32) NOT NULL,                   -- 'PHONE_ALERT', 'DOMAIN_ALERT', 'URL_ALERT', 'SENDER_ID_SPOOF', 'CAMPAIGN'
    target_value TEXT NOT NULL,                         -- Numer E.164, nazwa domeny lub Sender ID
    source_id VARCHAR(64) NOT NULL REFERENCES public.data_sources(id),
    severity VARCHAR(16) NOT NULL DEFAULT 'HIGH',       -- 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'
    threat_category VARCHAR(64) NOT NULL,               -- 'PHISHING_BANK', 'SUBSCRIPTION_FRAUD', 'SMISHING_PACKAGE'
    raw_payload JSONB,
    source_event_id VARCHAR(128),
    valid_from TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    valid_to TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_security_signals_target ON public.security_signals(target_value);
CREATE INDEX IF NOT EXISTS idx_security_signals_source ON public.security_signals(source_id);

-- ==============================================================================
-- 9. reputation_snapshots & 10. risk_snapshots: Wyniki obliczeń Silnika v1.0.0
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.reputation_snapshots (
    normalized_number VARCHAR(32) PRIMARY KEY,
    total_signals INT NOT NULL DEFAULT 0,
    dominant_category VARCHAR(32) NOT NULL DEFAULT 'OK',
    consensus_score REAL NOT NULL DEFAULT 1.0,
    telemarketing_ratio REAL NOT NULL DEFAULT 0.0,
    scam_ratio REAL NOT NULL DEFAULT 0.0,
    annoying_ratio REAL NOT NULL DEFAULT 0.0,
    last_signal_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.risk_snapshots (
    normalized_number VARCHAR(32) PRIMARY KEY,
    risk_score INT NOT NULL DEFAULT 0,                  -- 0 .. 100
    risk_level VARCHAR(16) NOT NULL DEFAULT 'LOW',      -- 'LOW', 'MODERATE', 'ELEVATED', 'HIGH'
    confidence_level VARCHAR(16) NOT NULL DEFAULT 'LOW',-- 'LOW', 'MEDIUM', 'HIGH'
    confidence_score REAL NOT NULL DEFAULT 0.1,
    primary_reason_code VARCHAR(64),                    -- z EngineReasonCode
    reasons_summary JSONB NOT NULL DEFAULT '[]'::jsonb,
    is_spoofing_warning BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ==============================================================================
-- 11. source_records: Strefa kwarantanny i walidacji surowych danych
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.source_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id VARCHAR(64) NOT NULL REFERENCES public.data_sources(id),
    import_job_id UUID,
    external_record_id VARCHAR(255),
    raw_payload JSONB NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,                  -- SHA-256 surowego rekordu do detekcji zmian (Change Detection)
    quarantine_status VARCHAR(32) NOT NULL DEFAULT 'QUARANTINE', -- 'QUARANTINE', 'PROMOTED', 'REJECTED'
    rejection_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_source_records_hash ON public.source_records(source_id, payload_hash);
CREATE INDEX IF NOT EXISTS idx_source_records_quarantine ON public.source_records(quarantine_status);

-- ==============================================================================
-- 12. import_jobs: Rejestr i obserwowalność zadań synchronizacyjnych
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.import_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id VARCHAR(64) NOT NULL REFERENCES public.data_sources(id),
    connector_version VARCHAR(32) NOT NULL DEFAULT '1.0.0',
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    finished_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL DEFAULT 'RUNNING',       -- 'RUNNING', 'COMPLETED', 'FAILED', 'REJECTED_BY_LEGAL'
    records_fetched INT NOT NULL DEFAULT 0,
    records_inserted INT NOT NULL DEFAULT 0,
    records_updated INT NOT NULL DEFAULT 0,
    records_rejected INT NOT NULL DEFAULT 0,
    error_summary TEXT
);

CREATE INDEX IF NOT EXISTS idx_import_jobs_source ON public.import_jobs(source_id, started_at DESC);

-- ==============================================================================
-- 13. import_errors: Szczegółowy audyt błędów i odrzuceń walidacji
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.import_errors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID REFERENCES public.import_jobs(id) ON DELETE CASCADE,
    source_id VARCHAR(64) NOT NULL,
    error_type VARCHAR(64) NOT NULL,                    -- 'LEGAL_VIOLATION', 'SCHEMA_MISMATCH', 'INVALID_PHONE_FORMAT', 'RATE_LIMIT', 'NETWORK_TIMEOUT'
    failed_record_sample TEXT,
    error_message TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ==============================================================================
-- SEED DATA: Oficjalny rejestr źródeł z wynikami audytu prawnego
-- ==============================================================================
INSERT INTO public.data_sources (
    id, name, source_type, base_url, license_type, commercial_use_allowed,
    redistribution_allowed, attribution_required, attribution_text, robots_status,
    api_available, legal_status, review_notes, update_frequency
) VALUES
(
    'cert_polska',
    'CERT Polska / NASK - Lista Ostrzeżeń',
    'SECURITY_FEED',
    'https://hole.cert.pl/domains/v2/domains.json',
    'STATUTORY_TASK',
    TRUE, TRUE, TRUE,
    'Źródło danych: CERT Polska (NASK-PIB)',
    'ALLOW', TRUE, 'APPROVED',
    'Oficjalne otwarte API feedu zagrożeń. Zapewnia natychmiastowe wykrywanie domen smishingowych i fałszywych linków w SMS.',
    'EVERY_5_MINUTES'
),
(
    'uke_tzn',
    'UKE - Tablice Zagospodarowania Numeracji',
    'PUBLIC_REGISTRY',
    'https://dane.gov.pl/dataset/tablice-numeracji',
    'OPEN_DATA_CC0',
    TRUE, TRUE, TRUE,
    'Źródło danych: Urząd Komunikacji Elektronicznej / Portal dane.gov.pl',
    'ALLOW', TRUE, 'APPROVED',
    'Oficjalny rejestr przydziałów zakresów numeracyjnych dla operatorów w Polsce. Fundament tożsamości telekomunikacyjnej.',
    'MONTHLY'
),
(
    'csirt_nask_sender_id',
    'CSIRT NASK - Wykaz Nadpisów SMS Podmiotów Publicznych',
    'STATUTORY_REGISTRY',
    'https://cert.pl/wykaz-nadpisow',
    'STATUTORY_TASK',
    TRUE, TRUE, TRUE,
    'Źródło danych: CSIRT NASK (Wykaz UOZnK)',
    'ALLOW', TRUE, 'APPROVED',
    'Ustawowy rejestr zastrzeżonych nadpisów SMS dla instytucji publicznych na mocy Ustawy o zwalczaniu nadużyć w komunikacji elektronicznej.',
    'WEEKLY'
),
(
    'csirt_knf_bozon',
    'CSIRT KNF - Baza Ostrzeżeń Publicznych (BOZON)',
    'SECURITY_FEED',
    'https://knf.gov.pl/dla_konsumenta/ostrzezenia_publiczne',
    'STATUTORY_TASK',
    TRUE, FALSE, TRUE,
    'Źródło danych: Urząd Komisji Nadzoru Finansowego',
    'ALLOW', TRUE, 'APPROVED_WITH_CONDITIONS',
    'Wymaga rejestracji i dedykowanego klucza BOZON API do celów przeciwdziałania przestępstwom finansowym.',
    'DAILY'
),
(
    'rpwdl_cez',
    'Rejestr Podmiotów Wykonujących Działalność Leczniczą',
    'PUBLIC_REGISTRY',
    'https://dane.gov.pl/dataset/rpwdl',
    'OPEN_DATA_CC0',
    TRUE, TRUE, TRUE,
    'Źródło danych: Centrum e-Zdrowia (CeZ) / dane.gov.pl',
    'ALLOW', TRUE, 'APPROVED',
    'Baza placówek medycznych, przychodni i szpitali. Służy do budowy białej listy zaufanych instytucji zdrowotnych.',
    'DAILY'
),
(
    'ceidg_biznes_gov',
    'CEIDG - Hurtownia Danych i Raporty',
    'PUBLIC_REGISTRY',
    'https://dane.biznes.gov.pl/api/ceidg/v2',
    'STATUTORY_TASK',
    TRUE, FALSE, TRUE,
    'Źródło danych: Ministerstwo Rozwoju i Technologii / Biznes.gov.pl',
    'ALLOW', TRUE, 'APPROVED_WITH_CONDITIONS',
    'Wymaga wniosku o klucz API. Ścisła kontrola RODO (zakaz scrapowania numerów prywatnych osób prowadzących JDG).',
    'DAILY'
),
(
    'nieznany_numer',
    'Nieznany-Numer.pl',
    'COMMERCIAL_PLATFORM',
    'https://nieznany-numer.pl',
    'PROPRIETARY',
    FALSE, FALSE, FALSE,
    NULL,
    'DISALLOW', FALSE, 'DO NOT INGEST',
    'Regulamin jawnie zabrania używania botów, scrapowania oraz kopiowania treści. Ochrona sui generis baz danych oraz wysokie ryzyko RODO.',
    'NEVER'
),
(
    'odebrac_pl',
    'Odebrac.pl',
    'COMMERCIAL_PLATFORM',
    'https://odebrac.pl',
    'PROPRIETARY',
    FALSE, FALSE, FALSE,
    NULL,
    'DISALLOW', FALSE, 'DO NOT INGEST',
    'Prywatny katalog. Zakaz automatycznego pobierania danych. Ryzyko pozwów o ochronę bazy danych i naruszenie dóbr osobistych.',
    'NEVER'
),
(
    'truecaller',
    'Truecaller Global Directory',
    'COMMERCIAL_PLATFORM',
    'https://truecaller.com',
    'PROPRIETARY',
    FALSE, FALSE, FALSE,
    NULL,
    'DISALLOW', FALSE, 'DO NOT INGEST',
    'Komercyjny serwis konkurencji. Przeznaczony wyłącznie do analizy porównawczej (Competitive Research). Zakaz scrapowania.',
    'NEVER'
)
ON CONFLICT (id) DO UPDATE SET
    legal_status = EXCLUDED.legal_status,
    review_notes = EXCLUDED.review_notes,
    updated_at = NOW();

-- ==============================================================================
-- Row Level Security (RLS)
-- ==============================================================================
ALTER TABLE public.data_sources ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.organizations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.organization_numbers ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.sender_ids ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.domains ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.phone_identities ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.community_reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.security_signals ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reputation_snapshots ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.risk_snapshots ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.source_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.import_jobs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.import_errors ENABLE ROW LEVEL SECURITY;

-- Polityki odczytu publicznego dla zaufanych danych kanonicznych (dla aplikacji mobilnej)
CREATE POLICY "Public read phone_identities" ON public.phone_identities FOR SELECT USING (true);
CREATE POLICY "Public read organizations" ON public.organizations FOR SELECT USING (is_active = true);
CREATE POLICY "Public read org_numbers" ON public.organization_numbers FOR SELECT USING (true);
CREATE POLICY "Public read sender_ids" ON public.sender_ids FOR SELECT USING (status = 'ACTIVE');
CREATE POLICY "Public read domains" ON public.domains FOR SELECT USING (true);
CREATE POLICY "Public read risk_snapshots" ON public.risk_snapshots FOR SELECT USING (true);
CREATE POLICY "Public read data_sources" ON public.data_sources FOR SELECT USING (true);

-- Tylko uwierzytelniony użytkownik może dodać community_report
CREATE POLICY "Users can insert community report" ON public.community_reports
    FOR INSERT WITH CHECK (auth.uid() IS NOT NULL);

-- Surowe dane, kwarantanna i błędy importu widoczne tylko dla roli serwisowej / backendu
CREATE POLICY "Service role only source_records" ON public.source_records
    FOR ALL USING (auth.jwt()->>'role' = 'service_role');
CREATE POLICY "Service role only import_jobs" ON public.import_jobs
    FOR ALL USING (auth.jwt()->>'role' = 'service_role');
CREATE POLICY "Service role only import_errors" ON public.import_errors
    FOR ALL USING (auth.jwt()->>'role' = 'service_role');
