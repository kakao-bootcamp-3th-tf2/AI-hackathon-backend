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
      <img src="https://github.com/KT20201224.png" width="120" height="120" alt="KT20201224" /><br/>
      <a href="https://github.com/KT20201224">@KT20201224</a>
    </td>
    <td align="center">
      <img src="https://github.com/jieun0824.png" width="120" height="120" alt="jieun0824" /><br/>
      <a href="https://github.com/jieun0824">@jieun0824</a>
    </td>
    <td align="center">
      <img src="https://github.com/rogi-rogi.png" width="120" height="120" alt="rogi-rogi" /><br/>
      <a href="https://github.com/rogi-rogi">@rogi-rogi</a>
    </td>
    <td align="center">
      <img src="https://github.com/Min-su-Jeong.png" width="120" height="120" alt="Min-su-Jeong" /><br/>
      <a href="https://github.com/Min-su-Jeong">@Min-su-Jeong</a>
    </td>
    <td align="center">
      <img src="https://github.com/tayobus.png" width="120" height="120" alt="tayobus" /><br/>
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






