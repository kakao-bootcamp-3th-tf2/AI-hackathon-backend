# Google Calendar API Reference (JJDC)

이 문서는 JJDC 백엔드에서 Google Calendar를 연동할 때 사용하는 OAuth Scope, 엔드포인트, 객체 형태를 정리한 것이다.  
모든 요청은 사용자 계정으로 발급된 Google Access Token(`https://www.googleapis.com/auth/calendar`)을 `Authorization: Bearer <token>` 헤더에 넣어야 한다.

## 핵심 시나리오

| 기능 | Google API | 요청 방식 | 비고 |
| --- | --- | --- | --- |
| JJDC 전용 캘린더 조회/생성 | `GET /users/me/calendarList`, `POST /calendars` | REST | 요약이 `JJDC`인 캘린더가 없으면 생성 |
| 일정 목록 조회 | `GET /calendars/{calendarId}/events` | REST | `timeMin/timeMax`로 조회 기간을 지정 |
| 일정 등록 | `POST /calendars/{calendarId}/events` | REST | body에 `summary`, `description`, `start/end` 작성 |
| 일정 수정 | `PATCH /calendars/{calendarId}/events/{eventId}` | REST | 부분 업데이트 허용 |
| 일정 삭제 | `DELETE /calendars/{calendarId}/events/{eventId}` | REST | 성공 시 204 |

## JJDC에서 사용하는 객체/필드

### Calendar List (`GET /users/me/calendarList`)
```json
{
  "items": [
    {
      "id": "abc123@group.calendar.google.com",
      "summary": "JJDC",
      "timeZone": "Asia/Seoul",
      "primary": false
    }
  ]
}
```
- `id`: 이후 모든 요청에서 사용할 캘린더 식별자.
- `summary`: 캘린더 이름. JJDC는 `"JJDC"`를 사용.

### Calendar Create (`POST /calendars`)
요청 예시:
```http
POST https://www.googleapis.com/calendar/v3/calendars
Content-Type: application/json

{
  "summary": "JJDC",
  "timeZone": "UTC"
}
```
 응답 본문에는 새 캘린더의 `id`, `summary`, `timeZone` 등이 포함된다.

### Events List (`GET /calendars/{calendarId}/events`)
주요 쿼리 파라미터:
- `timeMin`, `timeMax`: ISO-8601 UTC. 기간 지정 필수.
- `singleEvents=true`: 반복 이벤트를 개별 일정으로 확장.
- `orderBy=startTime`: 시작 시간 기준 정렬.

응답 객체:
```json
{
  "items": [
    {
      "id": "eventId",
      "summary": "미팅",
      "description": "회의 내용",
      "location": "회의실",
      "start": { "dateTime": "2025-12-18T09:00:00+09:00" },
      "end": { "dateTime": "2025-12-18T10:00:00+09:00" },
      "attendees": [
        { "email": "user@example.com", "responseStatus": "accepted" }
      ],
      "creator": { "email": "me@example.com" }
    }
  ]
}
```
JJDC 백엔드는 현재 `id`, `summary`, `start`, `end`만 변환하지만, 필요 시 `description`, `location`, `attendees`, `hangoutLink`, `recurrence`, `colorId` 등 다수 필드를 확장할 수 있다.

### Event Insert (`POST /calendars/{calendarId}/events`)
```json
{
  "summary": "CSync 회의",
  "description": "상세 내용",
  "start": { "dateTime": "2025-12-18T09:00:00+09:00" },
  "end": { "dateTime": "2025-12-18T10:00:00+09:00" },
  "attendees": [{ "email": "member@example.com" }]
}
```
응답으로 생성된 이벤트 전체 객체를 받는다.

### Event Update/Delete
- `PATCH /calendars/{calendarId}/events/{eventId}`: 변경할 필드만 전달하면 나머지는 유지된다.
- `DELETE /calendars/{calendarId}/events/{eventId}`: 본문 없이 204 반환.

## 기간 단축 파라미터
Google Calendar API는 “한 주/한 달” 같은 프리셋 엔드포인트를 제공하지 않는다.  
클라이언트가 원하는 구간(예: 오늘 00:00Z, 오늘+7일 등)을 계산해 `timeMin`/`timeMax`에 전달해야 한다.  
JJDC 백엔드도 같은 규칙을 적용하며, 필요한 경우 `range=week|month`와 같이 파라미터를 추가해 내부에서 시간 계산을 할 수 있다.

## 우리 서비스에서의 흐름
1. OAuth2 로그인 시 `https://www.googleapis.com/auth/calendar` Scope를 포함시켜 Access/Refresh Token을 저장한다.
2. 일정 조회 전 `users/me/calendarList`로 `"JJDC"` 캘린더 존재 여부를 확인하고, 없으면 `POST /calendars`로 생성한다.
3. 생성/조회한 캘린더 ID를 사용해 모든 이벤트 조작을 수행한다.
4. 필요에 따라 이벤트 등록/수정/삭제 API를 확장할 때 위 엔드포인트를 그대로 사용한다.

추가 참고: [Google Calendar API reference](https://developers.google.com/calendar/api/v3/reference).
