# 일상 소비 혜택 제안 서비스: JOJO discount
> 흩어져 있는 **일상 혜택 정보**를 자동으로 수집하고, 사용자의 **소비 일정에 맞춰 가장 유리한 혜택을 추천**하는 서비스  
> 개발 기간: ```25-12-17``` ~ ```25-12-19```

본 서비스는 사용자가 혜택을 찾아보는 것이 아닌, **자연스럽게 활용**하도록 지원하며, 건강한 소비 습관을 만드는 것을 목표로 합니다.

![](https://img.shields.io/badge/JavaScript-F7DF1E?style=flat-square&logo=javascript&logoColor=black)
![](https://img.shields.io/badge/React-61DAFB?style=flat-square&logo=react&logoColor=black)
![](https://img.shields.io/badge/Java-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![](https://img.shields.io/badge/SpringBoot-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![](https://img.shields.io/badge/GCP-4285F4?style=flat-square&logo=googlecloud&logoColor=white)
![](https://img.shields.io/badge/AWS-232F3E?style=flat-square&logo=amazonwebservices&logoColor=white)
![](https://img.shields.io/badge/GitHubActions-2088FF?style=flat-square&logo=githubactions&logoColor=white)


<br/>

## 문제 정의
- 혜택 정보는 여러 사이트·앱에 분산되어 있고, 변경 주기가 잦다.
- 실제 소비 시점에 어떤 카드/멤버십이 유리한지 즉시 비교하기 어렵다.
- 결과적으로 사용자는 혜택을 놓치거나, 비교 피로로 인해 혜택 활용을 포기한다.

<br/>

## 핵심 기능

1. 혜택 정보 자동 수집
    - 주기적으로 Google Search API를 통해 혜택 정보 수집
    - 텍스트/이미지에 대한 데이터 전처리 및 저장

2. 일정 기반 AI 추천
    - 사용자 일정을 기준으로 혜택 제안
    - 최적의 혜택을 누릴 수 있도록 일정 변경 제안

<br/>

## 팀 구성 및 역할 분담

<table>
  <tr>
    <th>AI</th>
    <th>Frontend</th>
    <th>Backend</th>
    <th>DevOps</th>
    <th>DevOps</th>
  </tr>
  <tr>
    <td align="center">
      <img src="https://github.com/KT20201224.png" width="120" alt="KT20201224" /><br/>
      <a href="https://github.com/KT20201224">@KT20201224</a>
    </td>
    <td align="center">
      <img src="https://github.com/jieun0824.png" width="120" alt="jieun0824" /><br/>
      <a href="https://github.com/jieun0824">@jieun0824</a>
    </td>
    <td align="center">
      <img src="https://github.com/rogi-rogi.png" width="120" alt="rogi-rogi" /><br/>
      <a href="https://github.com/rogi-rogi">@rogi-rogi</a>
    </td>
    <td align="center">
      <img src="https://github.com/Min-su-Jeong.png" width="120" alt="Min-su-Jeong" /><br/>
      <a href="https://github.com/Min-su-Jeong">@Min-su-Jeong</a>
    </td>
    <td align="center">
      <img src="https://github.com/tayobus.png" width="120" alt="tayobus" /><br/>
      <a href="https://github.com/tayobus">@tayobus</a>
    </td>
  </tr>
</table>

- KT20201224: AI 서버 개발, 데이터 수집 파이프라인 설계
- jieun0824: 프론트 전담, 디자인, UI/UX 설계
- rogi-rogi: PM, 백엔드 전담, 구글 캘린더 연동
- Min-su-Jeong: AWS 인프라 설계 전담, CI/CD 파이프라인 구축 및 안정화
- tayobus: AWS 인프라 관리 전담, FE 코드베이스 CI/CD

<br/>

## 트러블 슈팅(개인)

#### Min-su-Jeong
[![Notion](https://img.shields.io/badge/Notion-000000?style=for-the-badge&logo=notion&logoColor=white)](https://www.notion.so/2d03c62959b9802f8309e0f2b1618dd0?source=copy_link)


<br/>

## 회고(개인)

#### Min-su-Jeong
- Keep
    - GitHub CLI로 Actions 로그를 조회하며 원인 분석 → 피드백 시간 단축하기
    - 문제 발생 시 "증상 → 원인 → 해결" 흐름으로 문서화하여 재발 방지하기

- Problem
    - 로컬(x86)에서 테스트 후 배포 환경(ARM64)에서 실패 → 환경 차이 검증이 미비했음
    - Multi-stage Dockerfile 설계 시 에뮬레이션 제약을 사전에 파악하지 못함

- Try
    - PR 단계에서 타겟 아키텍처 빌드 테스트 추가 (ARM64 셀프호스트 러너 또는 네이티브 빌드 분리)
    - 인프라 변경 시 체크리스트 도입: 버전 호환성, 크로스 플랫폼, 권한 정책


#### KT20201224
-  Keep
    - api 사용부터 시작하는 것은 좋은 접근이다. (api로 시작하는 것은 MVP 단계에서 매우 강력)
    - 끊임 없이 새로 적용할 수 있는 기술들을 생각해 본 건 좋았다.
    - 기술적으로 미숙할 것 같은 사항은 사전에 기획 단계에서 어필

- Problem
    - 80% 정도 완성해두고, 생각이 앞서 나갔다. 필요한걸 100% 완성하고 push 한 후에 고도화를 진행해야 한다. "어느 정도 된 것 같아"라는 마인드를 버리자.
    - 아무리 급해도 commit과 push 한방에 하지 말고, commit을 어느 정도는 나눠놔야한다. 실제로 돌리고 싶은 지점이 존재했는데 돌리지 못했다.

- Try
    - VectorDB를 활용해 사용자 혜택과 이벤트 간의 유사도를 계산하는 방식을 사용했다면 단순 프롬프팅보다 더 "사용자 맞춤형 혜택을 제공할 수 있지 않았을까"라는 생각이 들었다.
    - 혜택 데이터 수집 시 OCR+LLM을 사용해 보는 과정에서 데이터 결측이 발생하는 지점을 기준을 가지고 테스트 해보지 않았는데, 어느 지점에서 데이터 유실이 발생하는지 알아보면 좋을것 같다.




