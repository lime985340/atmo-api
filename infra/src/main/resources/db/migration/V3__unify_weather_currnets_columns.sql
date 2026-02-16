-- weather_currnets 컬럼명 통일: rain -> precip, wind -> wind_speed
DO $$
BEGIN
  -- rain 컬럼을 precip으로 변경 (존재하는 경우)
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'weather_currnets'
      AND column_name = 'rain'
  ) THEN
    ALTER TABLE weather_currnets RENAME COLUMN rain TO precip;
  END IF;

  -- wind 컬럼을 wind_speed로 변경 (존재하는 경우)
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'weather_currnets'
      AND column_name = 'wind'
  ) THEN
    ALTER TABLE weather_currnets RENAME COLUMN wind TO wind_speed;
  END IF;
END $$;
