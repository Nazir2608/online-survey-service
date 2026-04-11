-- ============================================================
-- V1__init_schema.sql
-- Initial schema for online-survey-service
-- NOTE: Using VARCHAR(50) for enum columns instead of native
-- PostgreSQL enum types. Hibernate @Enumerated(EnumType.STRING)
-- sends plain VARCHAR — this avoids type cast errors entirely.
-- CHECK constraints enforce valid values at the DB level.
-- ============================================================

-- ─── USERS ─────────────────────────────────────────────────
CREATE TABLE users (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email        VARCHAR(255) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    role         VARCHAR(50)  NOT NULL DEFAULT 'RESPONDENT',
    enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_user_role CHECK (role IN ('ADMIN', 'CREATOR', 'RESPONDENT'))
);

-- ─── REFRESH TOKENS ────────────────────────────────────────
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(512) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ─── SURVEYS ───────────────────────────────────────────────
CREATE TABLE surveys (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    creator_id    UUID         NOT NULL REFERENCES users(id),
    title         VARCHAR(255) NOT NULL,
    description   TEXT,
    slug          VARCHAR(300) NOT NULL UNIQUE,
    status        VARCHAR(50)  NOT NULL DEFAULT 'DRAFT',
    is_anonymous  BOOLEAN      NOT NULL DEFAULT FALSE,
    max_responses INTEGER,
    start_date    TIMESTAMPTZ,
    end_date      TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_survey_status CHECK (status IN ('DRAFT','PUBLISHED','CLOSED','ARCHIVED'))
);

-- ─── QUESTIONS ─────────────────────────────────────────────
CREATE TABLE questions (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    survey_id    UUID        NOT NULL REFERENCES surveys(id) ON DELETE CASCADE,
    text         TEXT        NOT NULL,
    help_text    TEXT,
    type         VARCHAR(50) NOT NULL,
    order_index  INTEGER     NOT NULL DEFAULT 0,
    is_required  BOOLEAN     NOT NULL DEFAULT TRUE,
    min_value    INTEGER,
    max_value    INTEGER,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_question_type CHECK (type IN (
        'TEXT','PARAGRAPH','SINGLE_CHOICE','MULTIPLE_CHOICE',
        'RATING','DATE','EMAIL','NUMBER'
    ))
);

-- ─── OPTIONS ───────────────────────────────────────────────
CREATE TABLE options (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID         NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    text        VARCHAR(500) NOT NULL,
    order_index INTEGER      NOT NULL DEFAULT 0
);

-- ─── RESPONSES ─────────────────────────────────────────────
CREATE TABLE responses (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    survey_id     UUID        NOT NULL REFERENCES surveys(id),
    respondent_id UUID        REFERENCES users(id),
    ip_address    VARCHAR(64),
    user_agent    VARCHAR(500),
    submitted_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Prevent duplicate submissions (1 per user per survey)
CREATE UNIQUE INDEX uq_response_user_survey
    ON responses(survey_id, respondent_id)
    WHERE respondent_id IS NOT NULL;

-- ─── ANSWERS ───────────────────────────────────────────────
CREATE TABLE answers (
    id           UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    response_id  UUID    NOT NULL REFERENCES responses(id) ON DELETE CASCADE,
    question_id  UUID    NOT NULL REFERENCES questions(id),
    text_value   TEXT,
    rating_value INTEGER,
    UNIQUE(response_id, question_id)
);

-- ─── ANSWER SELECTED OPTIONS (MCQ) ─────────────────────────
CREATE TABLE answer_selected_options (
    answer_id UUID NOT NULL REFERENCES answers(id) ON DELETE CASCADE,
    option_id UUID NOT NULL REFERENCES options(id),
    PRIMARY KEY (answer_id, option_id)
);

-- ─── INDEXES ───────────────────────────────────────────────
CREATE INDEX idx_surveys_creator    ON surveys(creator_id);
CREATE INDEX idx_surveys_status     ON surveys(status);
CREATE INDEX idx_surveys_slug       ON surveys(slug);
CREATE INDEX idx_questions_survey   ON questions(survey_id, order_index);
CREATE INDEX idx_options_question   ON options(question_id, order_index);
CREATE INDEX idx_responses_survey   ON responses(survey_id);
CREATE INDEX idx_responses_user     ON responses(respondent_id);
CREATE INDEX idx_answers_response   ON answers(response_id);
CREATE INDEX idx_refresh_token      ON refresh_tokens(token);
CREATE INDEX idx_refresh_user       ON refresh_tokens(user_id);