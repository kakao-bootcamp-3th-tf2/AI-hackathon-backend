# AGENT LOG

## 2025-12-18
- Spring Security 6 OAuth2 + JWT 파이프라인, Member/GoogleOAuthToken 엔티티, Auth/Calendar API, README, application.yaml 샘플 구성.
- 패키지 구조를 레이어(service/repository/domain) 중심으로 재조정하고, 전역 예외/에러코드 체계 및 공통 ErrorResponse(`BusinessException`, `GlobalExceptionHandler`)를 도입, OAuth/Calendar/Member 전반에 적용.
- 헬스체크 컨트롤러(`/health`)를 추가하고 Spring Security 허용 엔드포인트에 포함.
- APIResponse/SuccessCode 기반 공통 응답 래퍼를 도입하고 모든 컨트롤러·전역 예외처리를 해당 포맷으로 통일.
- Google OAuth 플로우를 백엔드 단독으로 검증할 수 있도록 데모 페이지(`/demo/**`)와 리디렉션 설정(`app.front.redirect-uri`)을 추가하고 SuccessHandler가 토큰 정보를 location hash로 전달하도록 개선.
- Google Calendar API Swagger 파라미터에 기본값/예시를 부여해 즉시 테스트 가능한 요청 샘플을 제공.
- `/index.html` 접근 시 데모 페이지로 자동 리디렉트되도록 정적 인덱스 페이지를 추가.
- Google Calendar 조회 파라미터를 선택 입력으로 바꾸고 기본값을 “오늘 00:00Z ~ +1일”로 처리하도록 백엔드 로직과 문서를 수정.
- Google Calendar 연동 시 항상 `JJDC` 캘린더를 사용하도록 서비스 레이어를 수정하고, 없을 경우 자동 생성하도록 구현. 관련 동작/스펙을 `docs/GOOGLE_CALENDAR_API.md`에 정리.
- 현재 변경 사항은 로컬 작업 상태(푸시 전).
- Google Calendar 컨트롤러 로직을 무거운 응답 처리만 담당하고, 서비스는 MemberPrincipal 검증, 토큰/캘린더 관리, 이벤트 조회/DTO 변환을 기능별 메서드로 분리하도록 리팩토링.
- Google Calendar 컨트롤러의 엔드포인트를 `/api/calendar/events`로 단순화하고 문서(README)도 일치시키는 경로 리팩토링 수행.
- Google Calendar 일정 등록 플로우를 AI 응답, 문서화, SuccessCode/POST API 포함으로 단순화하면서 Redis 관련 구조는 제거함.
- 기존 일정의 설명을 AI 응답으로 이어붙이는 PATCH `/api/calendar/events/ai-note`를 추가하고 관련 SuccessCode/DTO/API 문서를 확대함; 요청은 일정 ID 리스트만 받아 요약 정보를 파싱하고 실패한 일정은 `null`로, 성공한 일정은 `GoogleCalendarEventDto`로 반환하도록 조정. `GoogleCalendarEventDto`에 `suggestList`를 추가해 AI가 전달한 혜택 후보를 그대로 응답에 담도록 개선함.
- AI 서버 응답 `{message, code, data}` 스펙을 도입하고, `data` 상태(널/빈/값)에 따라 사용자 친화적인 설명을 생성해 일정 본문/AI note 생성에 반영함.
- Swagger용 DTO에 필드 설명/예시를 붙여 인텔리센스에서 Request/Response 예시를 바로 확인할 수 있도록 개선함.
- Swagger/API 응답에 `suggestList`/`content` 필드를 포함시켜 AI 추천 리스트와 Google Calendar description을 그대로 전달하도록 DTO/서비스를 조정함.
- `PATCH /api/calendar/events/ai-note` path가 OPTIONS preflight/인증 리디렉트 없이 동작하도록 `RestTemplate`에 Apache HttpComponents를 적용하고 Spring Security의 CORS/entry-point를 튜닝해 브라우저 접근성을 확보함.
- `PATCH /api/calendar/events/ai-note` 요청 바디를 `{"needSuggestList": [...]}` 구조로 바꿔, eventIds 리스트를 `GoogleCalendarSuggestRequest`로 감싸 보다 명시적인 형식으로 처리하면서 존재하지 않거나 파싱 실패한 일정에는 `event`를 빈 문자열로 응답함.
- Google Calendar API 호출/토큰 갱신 책임을 `GoogleCalendarClient`로 분리하여 `GoogleCalendarService`는 AI 흐름/DTO 변환에 집중하도록 리팩토링했고, 관련 DTO/Config 주석도 보강함.
- GoogleCalendarService 각 메서드에 역할 주석을 보강하고 `appendSuggest` 흐름을 정리했으며, `GoogleCalendarSuggestRequest`/`GoogleCalendarSuggestResponse`에 Swagger 필드 설명과 예시를 더해 API 문서 신뢰도를 높임.
- GoogleCalendarAiDummyService라는 이름으로 AI 시뮬레이터를 명시해 실제 서비스 전까지 더미 동작임을 분명히 표기함.
- AI 연동을 추상화하기 위해 `GoogleCalendarAiService` 인터페이스를 추가했고, `GoogleCalendarAiDummyService`가 이 인터페이스를 구현하도록 하여 향후 실 운영용 구현체 추가가 용이하도록 구성함.
- GoogleCalendarService의 `getEvents`가 이벤트 리스트만 반환하고 컨트롤러가 `GoogleCalendarEventsResponse`를 조립하도록 조정해, 응답 생성 책임을 컨트롤러 계층으로 명확히 이동시킴.
- 토큰 재발급 응답의 `expiresIn` 필드가 누락될 경우 NPE가 발생하던 문제를 `refreshAccessToken`에서 검증하고 명시적인 BusinessException으로 치환하도록 수정함.
- Swagger에서 `GoogleCalendarSuggestRequest` 예시가 중첩되어 보이는 문제를 해결하기 위해 리스트 필드 예시를 `["event1","event2"]`로 바로 잡음.
- Notity(알람) 도메인을 신설해 JPA 엔티티/리포지토리/서비스/컨트롤러를 추가하고, 사용자별 알람 조회(`GET /api/calendar/notities`)·삭제(`DELETE /api/calendar/notities/{id}`) API를 제공함.
- GoogleCalendarService는 일정을 생성하거나 AI 설명을 덧붙일 때 Description을 이어쓰기 대신 덮어쓰고, 같은 시점에 Notity를 upsert하여 `createEvent`/`appendSuggest` 응답이 알람 정보를 반환하도록 수정함. `appendSuggest`는 Google API PATCH 시 start/end/summary를 모두 포함해 400 오류를 예방.
- 프론트가 직접 일정/혜택 내용을 넘겨 덮어쓸 수 있도록 `PUT /api/calendar/events/manual` API(`GoogleCalendarManualUpdateRequest`)를 추가하고, 해당 플로우도 Notity 저장/반환 구조로 통일함.
- `PUT /api/calendar/events/manual` 요청 바디는 `eventId`, `startAt`, `suggest`만 받고, 종료 시각은 서버가 startAt+1분으로 자동 설정하여 Google Calendar의 start<end 요구 조건을 만족시키도록 단순화함.
- README에 새 API/알람 동작을 반영하고, 전체 플로우가 Notity 중심으로 동작함을 문서화함.
- `app.ai.base-url`, `app.ai.health-url` 속성을 추가하고 `AppAiProperties`/`AiGatewayService`/`AiController`를 통해 `GET /api/ai/health`로 43.203.233.231:8080 헬스 엔드포인트를 프록시하도록 구현함. AI 서버 통신 실패 시 `AI_SERVER_REQUEST_FAILED` 오류 코드를 반환.

## 2025-12-19
- 소셜 온보딩 완료를 위해 Member 엔티티에 통신사/결제수단 필드를 추가하고 문자열 리스트를 단일 컬럼으로 직렬화하는 `StringListConverter`를 도입함.
- `MemberJoinRequest` DTO 및 `/api/members/join` 컨트롤러를 추가해 통신사와 결제 리스트를 입력받아 `updateProfile`이 상태를 ACTIVE로 변경하도록 구성함.
- SuccessCode에 MEMBER_PROFILE_UPDATED를 추가해 회원가입 완료 응답을 공통 포맷으로 제공함.
- OAuth2 성공 시 리다이렉트 URI와 JSON 페이로드에 `memberId`를 포함해 프론트가 사용자 식별자를 즉시 확보하도록 수정함.
