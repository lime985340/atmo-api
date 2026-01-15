# Weather + Air Dashboard Backend (Spring Boot 3 / Kotlin / Postgres(Neon)) - v2

이 레포는 다음을 **실제로 동작하는 형태**로 포함합니다.

## 포함된 기능(코어)
- Flyway V1 스키마 (`infra/src/main/resources/db/migration/V1__init.sql`)
- JWT Access/Refresh 발급/갱신/로그아웃
  - Refresh 토큰은 DB에 **해시로 저장**(탈취 대응 / 강제 로그아웃)
- 소셜 로그인(Authorization Code → Token → UserInfo → 우리 JWT)
  - Google / Naver / Kakao (각 Provider endpoint는 `app.oauth.providers.*`로 구성)
- /me, /favorites (DB 연동), /dashboard(DB 조회)
- Ingestion 모듈(잡 구조/옵션/프로퍼티) + Upsert 뼈대

> 공공데이터 수집/파싱은 공급 API(키/요청 파라미터) 확정이 필요하므로,
> v2에서는 "잡 구조 + upsert 패턴"까지 제공하고, 실제 공급 API 호출부는 TODO로 분리했습니다.

---

## 빠른 실행(로컬 Postgres)
### 1) DB
```bash
cd local
docker compose up -d
```

### 2) API 실행
```bash
cd ..
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/app"
export SPRING_DATASOURCE_USERNAME="app"
export SPRING_DATASOURCE_PASSWORD="app"
export JWT_ACCESS_SECRET="dev-access-secret-please-change"
export JWT_REFRESH_SECRET="dev-refresh-secret-please-change"
./gradlew :api:bootRun
```

### 3) location 1개만 seed(테스트용)
```sql
INSERT INTO location (name, lat, lon, nx, ny, station_name)
VALUES ('서울(샘플)', 37.5665, 126.9780, 60, 127, '종로구');
```

### 4) 확인
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Health: http://localhost:8080/actuator/health
- Dashboard: GET http://localhost:8080/api/v1/dashboard?locationId=1

---

## 소셜 로그인 테스트(개발)
Provider 콘솔에 Redirect URI를 등록하고 아래 환경변수를 세팅한 뒤:

- `APP_OAUTH_PROVIDERS_GOOGLE_CLIENT_ID`
- `APP_OAUTH_PROVIDERS_GOOGLE_CLIENT_SECRET`
- `APP_OAUTH_PROVIDERS_GOOGLE_REDIRECT_URI` (예: http://localhost:8080/api/v1/auth/oauth/google/callback)

로그인 URL 생성:
- GET `/api/v1/auth/oauth/google/authorize-url?returnTo=http://localhost:5173/`

인증 후 callback:
- provider가 callback으로 code를 붙여 호출 → 백엔드가 우리 JWT 반환

---

## GitOps 설정 포인트
- `spring.datasource.*`는 **환경변수**로 주입(Deployment에서 SecretRef)
- 평문 Secret을 Git에 올리지 말고:
  - Sealed Secrets(암호문 커밋) 또는
  - ESO + (Vault/OpenBao/Secret Manager) 방식 사용


## GitLab CI / ArgoCD Image Updater 태그 규칙

- dev 브랜치 push → 이미지 태그: `dev-<CI_COMMIT_SHORT_SHA>` (예: `dev-0000000`)
- Git tag `vX.Y.Z` 생성 → 이미지 태그: `vX.Y.Z` (예: `v0.0.0`)

ArgoCD Image Updater가 `kustomization.yaml`의 아래 형태를 업데이트하도록 맞추면 됩니다.

```yaml
images:
  - name: registry.gitlab.com/<GROUP>/<REPO>   # GitLab 프로젝트의 CI_REGISTRY_IMAGE와 동일
    newTag: dev-0000000
```

prod도 동일하게 `newTag: v0.0.0` 형태로 운용합니다.
