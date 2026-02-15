-- favorite_locations 테이블 구조 변경 + PostGIS 공간 인덱스 추가
-- station_name -> address (전체 주소)
-- name -> description
-- station_id 제거

-- ============================================
-- Part 1: favorite_locations 테이블 구조 변경
-- ============================================

-- 1. 새 컬럼 추가
ALTER TABLE favorite_locations ADD COLUMN IF NOT EXISTS address TEXT;
ALTER TABLE favorite_locations ADD COLUMN IF NOT EXISTS description TEXT;

-- 2. 기존 데이터 마이그레이션
UPDATE favorite_locations
SET address = station_name,
    description = name;

-- 3. 기존 컬럼 제거
ALTER TABLE favorite_locations DROP COLUMN IF EXISTS station_name;
ALTER TABLE favorite_locations DROP COLUMN IF EXISTS station_id;

-- 4. 기존 name 컬럼을 description으로 변경 (NOT NULL 제약조건 유지)
ALTER TABLE favorite_locations DROP COLUMN IF EXISTS name;
ALTER TABLE favorite_locations ALTER COLUMN description SET NOT NULL;

-- 5. 인덱스 변경 (station_id 인덱스 제거)
DROP INDEX IF EXISTS idx_favorite_locations_station_id;

-- 6. 새 인덱스 추가 (필요한 경우)
CREATE INDEX IF NOT EXISTS idx_favorite_locations_address ON favorite_locations(address);

-- ============================================
-- Part 2: PostGIS 공간 인덱스 및 성능 최적화
-- ============================================

-- 1. PostGIS 확장 활성화
CREATE EXTENSION IF NOT EXISTS postgis;

-- 2. stations 테이블에 geometry 컬럼 추가
ALTER TABLE stations ADD COLUMN IF NOT EXISTS location geometry(Point, 4326);

-- 3. 기존 lat/lng 데이터를 geometry로 변환
UPDATE stations
SET location = ST_SetSRID(ST_MakePoint(lng, lat), 4326)
WHERE location IS NULL AND lat IS NOT NULL AND lng IS NOT NULL;

-- 4. 공간 인덱스 생성 (GiST 인덱스)
CREATE INDEX IF NOT EXISTS idx_stations_location_gist ON stations USING GIST(location);

-- 5. Grid(nx, ny) 복합 인덱스 추가
CREATE INDEX IF NOT EXISTS idx_stations_nx_ny ON stations(nx, ny);

-- 6. 활성 상태 + 공간 데이터 인덱스
CREATE INDEX IF NOT EXISTS idx_stations_active_location ON stations(is_active, location) WHERE is_active = true;

-- 7. 거리 계산 함수 생성 (Haversine 공식)
CREATE OR REPLACE FUNCTION calculate_distance(
    lat1 DOUBLE PRECISION,
    lon1 DOUBLE PRECISION,
    lat2 DOUBLE PRECISION,
    lon2 DOUBLE PRECISION
) RETURNS DOUBLE PRECISION AS $$
DECLARE
    r DOUBLE PRECISION := 6371.0088;
    dlat DOUBLE PRECISION;
    dlon DOUBLE PRECISION;
    a DOUBLE PRECISION;
    c DOUBLE PRECISION;
BEGIN
    dlat := RADIANS(lat2 - lat1);
    dlon := RADIANS(lon2 - lon1);
    a := SIN(dlat/2) * SIN(dlat/2) +
         COS(RADIANS(lat1)) * COS(RADIANS(lat2)) *
         SIN(dlon/2) * SIN(dlon/2);
    c := 2 * ATAN2(SQRT(a), SQRT(1-a));
    RETURN r * c;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- 8. 가장 가까운 측정소 찾기 함수 (PostGIS 활용)
CREATE OR REPLACE FUNCTION find_nearest_station(
    target_lat DOUBLE PRECISION,
    target_lng DOUBLE PRECISION,
    max_distance_km DOUBLE PRECISION DEFAULT 50.0
) RETURNS TABLE (
    station_id BIGINT,
    station_name VARCHAR,
    distance_km DOUBLE PRECISION
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        s.id AS station_id,
        s.station_name,
        ST_Distance(
            s.location::geography,
            ST_SetSRID(ST_MakePoint(target_lng, target_lat), 4326)::geography
        ) / 1000.0 AS distance_km
    FROM stations s
    WHERE s.is_active = true
      AND s.location IS NOT NULL
      AND ST_DWithin(
          s.location::geography,
          ST_SetSRID(ST_MakePoint(target_lng, target_lat), 4326)::geography,
          max_distance_km * 1000.0
      )
    ORDER BY distance_km
    LIMIT 1;
END;
$$ LANGUAGE plpgsql STABLE;

-- 9. 기존 인덱스 정리 (중복 인덱스 제거)
DROP INDEX IF EXISTS idx_station_lat_lng;
