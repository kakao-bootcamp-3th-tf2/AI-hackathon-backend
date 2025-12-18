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
