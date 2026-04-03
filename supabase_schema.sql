-- Supabase schema for ElecLib
-- Выполните в Supabase → SQL Editor.
-- Можно запускать повторно: используются IF NOT EXISTS и DROP POLICY IF EXISTS.

-- =========================================================
-- Core tables
-- =========================================================

CREATE TABLE IF NOT EXISTS users (
  user_id bigserial PRIMARY KEY,
  login text NOT NULL UNIQUE,
  password text NOT NULL,
  first_name text NOT NULL,
  last_name text NOT NULL,
  role text NOT NULL DEFAULT 'app_user'
    CHECK (role IN ('app_user', 'admin'))
);

CREATE TABLE IF NOT EXISTS authors (
  author_id bigserial PRIMARY KEY,
  first_name text NOT NULL,
  last_name text NOT NULL,
  biography text
);

CREATE TABLE IF NOT EXISTS genres (
  genre_id bigserial PRIMARY KEY,
  name text NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS books (
  book_id bigserial PRIMARY KEY,
  title text NOT NULL,
  description text,
  text text NOT NULL,
  author_id bigint NOT NULL REFERENCES authors(author_id) ON DELETE RESTRICT,
  genre_id bigint NOT NULL REFERENCES genres(genre_id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS subscriptions (
  subscription_id bigserial PRIMARY KEY,
  user_id bigint NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
  start_date date NOT NULL,
  end_date date NOT NULL,
  status text NOT NULL CHECK (status IN ('active', 'expired', 'cancelled')),
  CHECK (end_date > start_date)
);

CREATE TABLE IF NOT EXISTS favorites (
  user_id bigint NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  book_id bigint NOT NULL REFERENCES books(book_id) ON DELETE CASCADE,
  PRIMARY KEY (user_id, book_id)
);

CREATE TABLE IF NOT EXISTS book_ratings (
  user_id bigint NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  book_id bigint NOT NULL REFERENCES books(book_id) ON DELETE CASCADE,
  rating int NOT NULL CHECK (rating BETWEEN 1 AND 5),
  review text,
  created_at timestamptz DEFAULT now(),
  PRIMARY KEY (user_id, book_id)
);

-- Optional: character offset progress (not used by current reader UI)
CREATE TABLE IF NOT EXISTS reading_progress (
  user_id bigint NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  book_id bigint NOT NULL REFERENCES books(book_id) ON DELETE CASCADE,
  last_position int NOT NULL DEFAULT 0,
  updated_at timestamptz DEFAULT now(),
  PRIMARY KEY (user_id, book_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_books_title ON books(title);
CREATE INDEX IF NOT EXISTS idx_books_author_id ON books(author_id);
CREATE INDEX IF NOT EXISTS idx_books_genre_id ON books(genre_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_status ON subscriptions(user_id, status);

-- =========================================================
-- Reader features: page position, bookmarks, notes
-- =========================================================

CREATE TABLE IF NOT EXISTS reading_position (
  user_id bigint NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  book_id bigint NOT NULL REFERENCES books(book_id) ON DELETE CASCADE,
  last_page int NOT NULL DEFAULT 1,
  updated_at timestamptz DEFAULT now(),
  PRIMARY KEY (user_id, book_id)
);

ALTER TABLE reading_position ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all for service role" ON reading_position;
CREATE POLICY "Allow all for service role" ON reading_position FOR ALL
USING (true)
WITH CHECK (true);

CREATE TABLE IF NOT EXISTS book_marks (
  id bigserial PRIMARY KEY,
  user_id bigint NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  book_id bigint NOT NULL REFERENCES books(book_id) ON DELETE CASCADE,
  page_number int NOT NULL,
  title text,
  created_at timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_book_marks_user_book ON book_marks (user_id, book_id);

ALTER TABLE book_marks ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all for service role" ON book_marks;
CREATE POLICY "Allow all for service role" ON book_marks FOR ALL
USING (true)
WITH CHECK (true);

CREATE TABLE IF NOT EXISTS reading_notes (
  id bigserial PRIMARY KEY,
  user_id bigint NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  book_id bigint NOT NULL REFERENCES books(book_id) ON DELETE CASCADE,
  page_number int NOT NULL,
  quote_text text NOT NULL,
  title text,
  created_at timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_reading_notes_user_book ON reading_notes (user_id, book_id);
CREATE INDEX IF NOT EXISTS idx_reading_notes_user_book_page ON reading_notes (user_id, book_id, page_number);

ALTER TABLE reading_notes ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow all for service role" ON reading_notes;
CREATE POLICY "Allow all for service role" ON reading_notes FOR ALL
USING (true)
WITH CHECK (true);

