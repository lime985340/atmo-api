CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE TABLE app_user (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  provider TEXT NOT NULL,
  provider_subject TEXT NOT NULL,
  email TEXT,
  name TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_user_provider_sub UNIQUE (provider, provider_subject)
);
CREATE TABLE user_refresh_token (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  token_hash TEXT NOT NULL,
  issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  expires_at TIMESTAMPTZ NOT NULL,
  revoked_at TIMESTAMPTZ,
  user_agent TEXT,
  ip TEXT
);
CREATE INDEX idx_refresh_user ON user_refresh_token(user_id);
CREATE INDEX idx_refresh_expires ON user_refresh_token(expires_at);
CREATE TABLE location (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  lat DOUBLE PRECISION NOT NULL,
  lon DOUBLE PRECISION NOT NULL,
  nx INTEGER NOT NULL,
  ny INTEGER NOT NULL,
  station_name TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_location_nxny ON location(nx, ny);
CREATE INDEX idx_location_station ON location(station_name);
CREATE TABLE favorite_location (
  user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  location_id BIGINT NOT NULL REFERENCES location(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, location_id)
);
CREATE TABLE weather_now (
  location_id BIGINT NOT NULL REFERENCES location(id) ON DELETE CASCADE,
  observed_at TIMESTAMPTZ NOT NULL,
  temp_c REAL,
  humidity REAL,
  wind_speed REAL,
  precip_type TEXT,
  precip_mm REAL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (location_id, observed_at)
);
CREATE INDEX idx_weather_now_latest ON weather_now(location_id, observed_at DESC);
CREATE TABLE weather_hourly_forecast (
  location_id BIGINT NOT NULL REFERENCES location(id) ON DELETE CASCADE,
  base_datetime TIMESTAMPTZ NOT NULL,
  forecast_at TIMESTAMPTZ NOT NULL,
  temp_c REAL,
  humidity REAL,
  wind_speed REAL,
  pop REAL,
  sky TEXT,
  precip_type TEXT,
  precip_mm REAL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (location_id, forecast_at)
);
CREATE INDEX idx_weather_fc_base ON weather_hourly_forecast(location_id, base_datetime DESC);
CREATE INDEX idx_weather_fc_range ON weather_hourly_forecast(location_id, forecast_at);
CREATE TABLE air_now (
  station_name TEXT NOT NULL,
  measured_at TIMESTAMPTZ NOT NULL,
  khai REAL,
  pm10 REAL,
  pm25 REAL,
  o3 REAL,
  grade TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (station_name, measured_at)
);
CREATE INDEX idx_air_now_latest ON air_now(station_name, measured_at DESC);
