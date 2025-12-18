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
