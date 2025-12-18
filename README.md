## Google OAuth + JWT + Calendar 통합 개요

### 구성 요소
- **React SPA (`https://fe.com`)**가 `GET /oauth2/authorization/google`을 호출하면 Spring Security 6 OAuth2 Client가 Google Consent 화면으로 리다이렉트.
- 로그인 성공 시
  1. `GoogleOAuth2User`에서 `email`, `sub(providerId)` 추출.
  2. `MemberService`가 `provider=GOOGLE + providerId=sub` 기준으로 회원을 조회/생성하며 기본 상태는 `PENDING`.
  3. `OAuth2SuccessHandler`가 Google Access/Refresh Token을 `GoogleOAuthToken` 엔티티에 저장하고, 서비스 JWT(AT/RT)를 각각 JSON 본문 및 HttpOnly 쿠키로 발급.
  4. 응답은 `302 Location: https://fe.com/oauth2/redirect` + JSON `{accessToken, status, redirectUri}` 형태라 SPA Developer Tool에서 디버깅하기 쉽다.
  5. 프론트는 리다이렉트 페이지에서 JSON 응답으로 받은 Access Token을 저장하고 `GET /api/auth/status`를 호출해 `status`가 `PENDING`인지 확인 후 온보딩 화면으로 분기한다.
- Google Calendar 호출은 `GET /api/calendar/events` 샘플 API로 확인할 수 있다.

### 엔티티 설계
| 엔티티 | 주요 필드 | 비고 |
| --- | --- | --- |
| `Member` | `provider`, `providerId`, `email`, `status(PENDING/ACTIVE)` | provider+providerId로 Unique, 추가 입력 없이 최초 생성 시 `PENDING` |
| `GoogleOAuthToken` | `memberId`, `googleAccessToken`, `googleRefreshToken`, `accessTokenExpiredAt`, `scope` | Refresh Token은 평문 저장(TODO: 운영 전 암호화) |

### JWT 발급 정책
- Access Token: 30분(`jwt.access.ttl`), JSON Body로 프론트에 전달.
- Refresh Token: 2주(`jwt.refresh.ttl`), HttpOnly + `SameSite=None` 쿠키(`refresh_token`)로 보관하며 서버 무상태.
- Access/Refresh는 서로 다른 secret을 사용하고 `typ` Claim으로 토큰 유형을 구분한다.

### Security 설정 요약
- 허용 경로: `/`, `/login/**`, `/oauth2/**`, `/api/auth/**`.
- 나머지는 JWT 기반 인증필터(`JwtAuthFilter`) 적용.
- OAuth2 Authorization 요청에 `access_type=offline`, `prompt=consent`를 강제해서 Refresh Token 재발급 문제를 줄인다.
- **TODO(운영 전)**: 
  - CSRF, CORS, Cookie `secure`/`SameSite` 값은 운영 도메인에 맞춰 다시 잠근다.
  - Google Refresh Token은 KMS/암호화 컬럼 등 안전한 저장소로 이전.

### Google Calendar 연동
1. Google Cloud Console > OAuth Consent Screen + Calendar API 활성화.
2. OAuth Client ID에 `https://api-ec2-dns.com/login/oauth2/code/google`을 Redirect URI로 등록.
3. `spring.security.oauth2.client.registration.google.scope`에 `https://www.googleapis.com/auth/calendar`가 포함되어야 한다.
4. `GoogleCalendarService`는 Access Token 만료 시 Refresh Token으로 `https://oauth2.googleapis.com/token`에 재발급 요청 후 DB에 저장한다.
5. 샘플 API: `GET /api/calendar/events?from=2024-08-01T00:00:00Z&to=2024-08-07T00:00:00Z`
   - Authorization 헤더에 `Bearer {서비스 Access Token}`을 넣고 호출.
   - `from`/`to`를 생략하면 기본값은 “오늘 00:00Z”부터 “+1일” 구간으로 처리된다.
6. 사용자 계정에서 `"JJDC"`라는 캘린더만을 대상으로 동작하며, 존재하지 않으면 자동 생성한다.
7. 일정 등록: `POST /api/calendar/events`로 `category`, `brand`, `startAt`, `endAt`을 JSON 본문으로 보내면 "#카테고리#브랜드" 제목의 일정이 AI 혜택 설명(+`suggestList`)과 함께 생성되고, 동일한 정보가 알람 엔티티(Notity)로 저장되어 응답 본문에 담긴다.
8. AI 설명 추가: `PATCH /api/calendar/events/suggest`로 `{"needSuggestList":["eventId1","eventId2"]}` 형태를 보내면 각 이벤트 제목에서 카테고리/브랜드를 파싱해 AI 응답으로 일정 내용을 덮어쓰고, 성공한 건마다 Notity 정보(`eventId`, `summary`, `content`, `suggestList` 등)를 반환한다. 실패한 일정은 `notity = null`로 응답된다.
9. 수동 덮어쓰기: `PUT /api/calendar/events/manual`은 `eventId`, `startAt`, `suggest`만 받아 일정 시간을 `startAt ~ (startAt+1분)`으로 자동 설정하고 설명을 `suggest`로 덮어쓴 뒤 동일 내용을 Notity로 저장해 반환한다.
10. 알람 조회/삭제: `GET /api/calendar/notities`로 사용자의 모든 알람을 조회하고 `DELETE /api/calendar/notities/{id}`로 단일 알람을 제거할 수 있다.
11. AI 응답 스펙: AI 서버는 `{ "message", "code", "data" }` 구조로 응답하며, `data`가 `null`이면 오류, 빈 리스트면 혜택 없음, 값이 있으면 `{suggest, fromDate, toDate}` 항목을 사용자 친화적인 텍스트로 캘린더 설명에 포함시킨다.

> 자세한 Google Calendar API 호출 정보는 `docs/GOOGLE_CALENDAR_API.md` 참고.

### 로컬 테스트 시나리오
1. `.env` 혹은 `application-secret.yaml`에 Google Client/Secret, JWT Secret, DB 정보를 설정한다.
2. `./gradlew bootRun` 실행 후 브라우저에서 `http://localhost:8080/demo/index.html` 접속, “Google 로그인 시작” 버튼 클릭.
3. Google 로그인 → 동의 화면에서 Calendar 전체 권한 허용.
4. 리디렉션된 `http://localhost:8080/demo/oauth2-redirect.html`에서 Access Token과 회원 상태를 복사한다.
5. Swagger Authorize 혹은 API 클라이언트에서 복사한 Access Token을 Bearer 값으로 넣은 뒤 `GET /api/auth/status` 호출하여 온보딩 분기를 확인한다.
6. `GET /api/calendar/events` 호출로 Sample Calendar 연동 확인.

#### 백엔드 내장 데모 페이지
- 기본 설정(`app.front.redirect-uri`)은 백엔드가 제공하는 `demo/oauth2-redirect.html`을 가리킨다.
- 실제 FE와 연동할 때는 `FRONT_REDIRECT_URI` 환경변수를 원하는 도메인으로 덮어쓰면 된다.
- 데모 페이지는 Access Token을 location hash로 전달받으므로 네트워크 탭에서도 토큰이 노출되지 않는다.

### 운영 전 보안 체크 TODO
- [ ] CORS: 허용 Origin을 FE 도메인으로 제한하고 Credential 옵션 점검.
- [ ] CSRF: OAuth Callback 외 API 는 CSRF 방어 적용.
- [ ] Cookie: HTTPS 정식 도입 후 `secure=true` 유지, `SameSite=None`을 계속 사용할지 검토.
- [ ] Refresh Token 저장: 최소한 AES 암호화 혹은 Secret Manager 사용.
- [ ] OAuth Scope: Calendar 전체 권한을 지속적으로 사용할지, 필요한 최소 스코프로 축소 검토.
