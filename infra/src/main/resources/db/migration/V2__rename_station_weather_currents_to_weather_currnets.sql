DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.tables
    WHERE table_schema = 'public'
      AND table_name = 'station_weather_currents'
  ) THEN
    ALTER TABLE station_weather_currents RENAME TO weather_currnets;
  END IF;
END $$;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM pg_indexes
    WHERE schemaname = 'public'
      AND indexname = 'idx_station_weather_updated_at'
  ) THEN
    ALTER INDEX idx_station_weather_updated_at RENAME TO idx_weather_currnets_updated_at;
  END IF;

  IF EXISTS (
    SELECT 1
    FROM pg_indexes
    WHERE schemaname = 'public'
      AND indexname = 'idx_station_weather_station'
  ) THEN
    ALTER INDEX idx_station_weather_station RENAME TO idx_weather_currnets_station;
  END IF;
END $$;
