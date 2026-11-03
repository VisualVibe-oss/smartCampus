-- ============================================================
-- SmartCampus - Schéma de base de données (PostgreSQL)
-- Référence : Rapport technique SmartCampus v1.0
-- Modules : AUTH | ROOM | BOOK | ADMIN
-- ============================================================

-- Nécessaire pour empêcher les chevauchements sur une même salle.
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ------------------------------------------------------------
-- Module AUTH : Utilisateurs
-- ------------------------------------------------------------
CREATE TABLE users (
    id            UUID PRIMARY KEY,
    full_name     VARCHAR(150) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20) NOT NULL
                  CHECK (role IN ('STUDENT', 'STAFF', 'ADMIN')),
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX uq_users_email_ci ON users (LOWER(email));

-- ------------------------------------------------------------
-- Module ROOM : Salles & Équipements
-- ------------------------------------------------------------
CREATE TABLE rooms (
    id         UUID PRIMARY KEY,
    code       VARCHAR(30) NOT NULL,
    name       VARCHAR(120) NOT NULL,
    building   VARCHAR(120) NOT NULL,
    capacity   INTEGER NOT NULL CHECK (capacity > 0),
    is_active  BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (building, code)
);

CREATE TABLE equipments (
    id    UUID PRIMARY KEY,
    name  VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE room_equipments (
    room_id      UUID NOT NULL REFERENCES rooms(id),
    equipment_id UUID NOT NULL REFERENCES equipments(id),
    PRIMARY KEY (room_id, equipment_id)
);

-- ------------------------------------------------------------
-- Module BOOK : Réservations
-- ------------------------------------------------------------
CREATE TABLE reservations (
    id                UUID PRIMARY KEY,
    user_id           UUID NOT NULL REFERENCES users(id),
    room_id           UUID NOT NULL REFERENCES rooms(id),
    start_at          TIMESTAMPTZ NOT NULL,
    end_at            TIMESTAMPTZ NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED'
                      CHECK (status IN ('CONFIRMED', 'CANCELLED', 'REVOKED')),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    cancelled_at      TIMESTAMPTZ,
    revoked_by        UUID REFERENCES users(id),
    revoked_at        TIMESTAMPTZ,
    revocation_reason TEXT,
    CHECK (end_at - start_at = INTERVAL '168 hours'),
    CHECK (status <> 'CANCELLED' OR cancelled_at IS NOT NULL),
    CHECK (status <> 'REVOKED' OR
           (revoked_at IS NOT NULL AND revoked_by IS NOT NULL)),
    CONSTRAINT no_overlapping_reservations
        EXCLUDE USING gist (
            room_id WITH =,
            tstzrange(start_at, end_at, '[)') WITH &&
        ) WHERE (status = 'CONFIRMED')
);

CREATE INDEX idx_reservations_user ON reservations (user_id, start_at DESC);
