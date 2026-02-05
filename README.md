# 현재 완료상태

이 문서는 상태 통합으로 간소화되었습니다. 최신 업데이트는 `STATUS.md`만 진행합니다.

## 완료
- 인증: OAuth(google/naver/kakao) -> JWT 발급, refresh 토큰 해시 저장/회전/로그아웃
- 핵심 스키마: app_users, user_refresh_tokens, stations, air_observations, weather_observations, air_currents, station_weather_currents, ingestion_runs, favorite_locations
- 측정소 API: /api/v1/stations, /api/v1/stations/{id}, /api/v1/stations/by-sido
- 대시보드/즐겨찾기: /api/v1/dashboard, /api/v1/favorites(+/dashboard)
- 수집(ingestion): station seed, air_current, station_weather_current (cron 스케줄, ingestion 프로파일)

## 부분 완료
- 시간대 예보(nextHours): WeatherReadAdapter.getNextHours가 빈 리스트 반환(예보 데이터/수집 미구현)
- 지도 폴리곤/행정구역 API: 코드 없음(가이드만 존재)

## 미완/부재
- 자동 테스트 코드: 없음 (gradle test NO-SOURCE)

## 업데이트 원칙
- 앞으로 `STATUS.md`만 업데이트
