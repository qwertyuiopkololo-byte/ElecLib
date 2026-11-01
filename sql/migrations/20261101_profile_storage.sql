-- Run once in Supabase SQL Editor. Safe to run again.
BEGIN;
CREATE TABLE IF NOT EXISTS public.monthly_reading_goals (
  user_id bigint NOT NULL REFERENCES public.users(user_id) ON DELETE CASCADE,
  month date NOT NULL CHECK (extract(day FROM month) = 1),
  target integer NOT NULL CHECK (target BETWEEN 1 AND 100),
  PRIMARY KEY (user_id, month)
);
CREATE TABLE IF NOT EXISTS public.monthly_reading_completions (
  user_id bigint NOT NULL REFERENCES public.users(user_id) ON DELETE CASCADE,
  month date NOT NULL CHECK (extract(day FROM month) = 1),
  book_id bigint NOT NULL REFERENCES public.books(book_id) ON DELETE CASCADE,
  finished_on date NOT NULL,
  PRIMARY KEY (user_id, month, book_id)
);
ALTER TABLE public.monthly_reading_goals ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.monthly_reading_completions ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.monthly_reading_goals, public.monthly_reading_completions FROM anon, authenticated;
GRANT ALL ON public.monthly_reading_goals, public.monthly_reading_completions TO service_role;
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES ('avatars', 'avatars', false, 1000000, ARRAY['image/png'])
ON CONFLICT (id) DO NOTHING;
NOTIFY pgrst, 'reload schema';
COMMIT;
