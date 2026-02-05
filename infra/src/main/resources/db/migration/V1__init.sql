CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE app_users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  provider TEXT NOT NULL,
  provider_subject TEXT NOT NULL,
  email TEXT,
  name TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_user_provider_sub UNIQUE (provider, provider_subject)
);

CREATE TABLE user_refresh_tokens (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
  token_hash TEXT NOT NULL,
  issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  expires_at TIMESTAMPTZ NOT NULL,
  revoked_at TIMESTAMPTZ,
  user_agent TEXT,
  ip TEXT
);

CREATE INDEX idx_refresh_user ON user_refresh_tokens(user_id);
CREATE INDEX idx_refresh_expires ON user_refresh_tokens(expires_at);

CREATE TABLE stations (
  id BIGSERIAL PRIMARY KEY,
  provider TEXT NOT NULL DEFAULT 'AIRKOREA',
  station_code TEXT,
  station_name TEXT NOT NULL,
  addr TEXT,
  sido TEXT,
  region_code TEXT,
  lat DOUBLE PRECISION,
  lng DOUBLE PRECISION,
  nx INTEGER,
  ny INTEGER,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_station_provider_name_sido UNIQUE (provider, station_name, sido),
  CONSTRAINT chk_station_lat CHECK (lat IS NULL OR (lat >= 33 AND lat <= 43)),
  CONSTRAINT chk_station_lng CHECK (lng IS NULL OR (lng >= 124 AND lng <= 132))
);

CREATE INDEX idx_station_sido ON stations(sido);
CREATE INDEX idx_station_active ON stations(is_active);
CREATE INDEX idx_station_lat_lng ON stations(lat, lng);
CREATE INDEX idx_station_nx_ny ON stations(nx, ny);
CREATE INDEX idx_station_provider_name ON stations(provider, station_name);
CREATE INDEX idx_station_sido_active ON stations(sido, is_active);
CREATE INDEX idx_station_station_code ON stations(station_code);

CREATE TABLE air_observations (
  id BIGSERIAL PRIMARY KEY,
  station_id BIGINT NOT NULL REFERENCES stations(id) ON DELETE CASCADE,
  observed_at TIMESTAMPTZ NOT NULL,
  pm10 REAL,
  pm25 REAL,
  o3 REAL,
  no2 REAL,
  co REAL,
  so2 REAL,
  grade INTEGER,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_air_observation UNIQUE (station_id, observed_at),
  CONSTRAINT chk_air_observation_grade CHECK (grade IS NULL OR (grade >= 1 AND grade <= 4))
);

CREATE INDEX idx_air_observation_observed_at ON air_observations(observed_at);
CREATE INDEX idx_air_observation_station_time ON air_observations(station_id, observed_at DESC);

CREATE TABLE weather_observations (
  id BIGSERIAL PRIMARY KEY,
  station_id BIGINT NOT NULL REFERENCES stations(id) ON DELETE CASCADE,
  observed_at TIMESTAMPTZ NOT NULL,
  temp REAL,
  humidity REAL,
  wind_speed REAL,
  precip REAL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_weather_observation UNIQUE (station_id, observed_at)
);

CREATE INDEX idx_weather_observation_observed_at ON weather_observations(observed_at);
CREATE INDEX idx_weather_observation_station_time ON weather_observations(station_id, observed_at DESC);

CREATE TABLE weather_forecasts (
  id BIGSERIAL PRIMARY KEY,
  station_id BIGINT NOT NULL REFERENCES stations(id) ON DELETE CASCADE,
  forecast_at TIMESTAMPTZ NOT NULL,
  temp REAL,
  humidity REAL,
  wind_speed REAL,
  pop REAL,
  sky TEXT,
  precip_type TEXT,
  precip_mm REAL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_weather_forecast UNIQUE (station_id, forecast_at)
);

CREATE INDEX idx_weather_forecast_station_time ON weather_forecasts(station_id, forecast_at);
CREATE INDEX idx_weather_forecast_time ON weather_forecasts(forecast_at);

CREATE TABLE air_currents (
  station_id BIGINT PRIMARY KEY REFERENCES stations(id) ON DELETE CASCADE,
  data_time TIMESTAMPTZ NOT NULL,
  pm10 REAL,
  pm25 REAL,
  grade INTEGER,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT chk_air_grade CHECK (grade IS NULL OR (grade >= 1 AND grade <= 4))
);

CREATE INDEX idx_air_current_time ON air_currents(data_time DESC);
CREATE INDEX idx_air_current_station_time ON air_currents(station_id, data_time DESC);

CREATE TABLE station_weather_currents (
  station_id BIGINT PRIMARY KEY REFERENCES stations(id) ON DELETE CASCADE,
  obs_time VARCHAR(16) NOT NULL,
  temp DOUBLE PRECISION,
  rain DOUBLE PRECISION,
  wind DOUBLE PRECISION,
  humidity DOUBLE PRECISION,
  updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_station_weather_updated_at ON station_weather_currents(updated_at);
CREATE INDEX idx_station_weather_station ON station_weather_currents(station_id);

CREATE TABLE ingestion_runs (
  id BIGSERIAL PRIMARY KEY,
  job_type TEXT NOT NULL,
  started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  ended_at TIMESTAMPTZ,
  status TEXT NOT NULL,
  last_error TEXT,
  success_count INTEGER NOT NULL DEFAULT 0,
  fail_count INTEGER NOT NULL DEFAULT 0,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ingestion_run_job_started ON ingestion_runs(job_type, started_at DESC);

CREATE TABLE favorite_locations (
  id BIGSERIAL PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  lat DOUBLE PRECISION NOT NULL,
  lon DOUBLE PRECISION NOT NULL,
  nx INTEGER NOT NULL,
  ny INTEGER NOT NULL,
  station_id BIGINT,
  station_name TEXT,
  is_active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_favorite_locations_user_active ON favorite_locations(user_id, is_active);
CREATE INDEX idx_favorite_locations_station_id ON favorite_locations(station_id);
CREATE INDEX idx_favorite_locations_nxny ON favorite_locations(nx, ny);
