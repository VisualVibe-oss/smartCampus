-- ============================================================
-- SmartCampus - Schéma de base de données (PostgreSQL)
-- Modules : AUTH | ROOM | BOOK
-- ============================================================

-- ------------------------------------------------------------
-- Module AUTH : Utilisateurs
-- ------------------------------------------------------------
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,   -- e-mail institutionnel
    password    VARCHAR(255) NOT NULL,          -- hash bcrypt
    full_name   VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL,          -- STUDENT | PROFESSOR | STAFF | ADMIN
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ------------------------------------------------------------
-- Module ROOM : Salles & Équipements
-- ------------------------------------------------------------
CREATE TABLE rooms (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    building    VARCHAR(100) NOT NULL,
    capacity    INTEGER      NOT NULL CHECK (capacity > 0),
    description TEXT,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    UNIQUE (name, building)
);

CREATE TABLE equipment (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE   -- ex: Projecteur, Climatisation, ...
);

-- Relation N:N salle <-> équipements
CREATE TABLE room_equipment (
    room_id     BIGINT NOT NULL REFERENCES rooms (id)     ON DELETE CASCADE,
    equipment_id BIGINT NOT NULL REFERENCES equipment (id) ON DELETE CASCADE,
    PRIMARY KEY (room_id, equipment_id)
);

-- ------------------------------------------------------------
-- Module BOOK : Réservations
-- ------------------------------------------------------------
CREATE TABLE reservations (
    id         BIGSERIAL PRIMARY KEY,
    room_id    BIGINT      NOT NULL REFERENCES rooms (id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title      VARCHAR(255) NOT NULL,
    starts_at  TIMESTAMPTZ NOT NULL,
    ends_at    TIMESTAMPTZ NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',   -- CONFIRMED | CANCELLED
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_reservation_range CHECK (ends_at > starts_at)
);

CREATE INDEX idx_reservations_user ON reservations (user_id);
CREATE INDEX idx_reservations_room ON reservations (room_id, starts_at, ends_at);

-- Contrôle des conflits : empêche deux réservations CONFIRMÉES
-- qui se chevauchent pour une même salle (exclusion PostgreSQL).
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE reservations
    ADD CONSTRAINT excl_reservations_no_overlap
    EXCLUDE USING gist (
        room_id WITH =,
        tstzrange(starts_at, ends_at) WITH &&
    ) WHERE (status = 'CONFIRMED');
