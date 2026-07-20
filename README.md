<div align="center">

<table border="0" cellspacing="0" cellpadding="0">
<tr>

<td width="40%" align="center" valign="middle">

<img width="140" alt="image (3)" src="https://github.com/user-attachments/assets/1019216c-972b-419d-a2ee-fcc3945be36a" />

</td>

<td width="60%" align="center" valign="middle">

<img width="700" alt="image (2)" src="https://github.com/user-attachments/assets/cf6a91af-a994-488b-912e-5d51b1b63062" />

</td>

</tr>
</table>

</div>

# ASG - All Social Gather
> AI 기반 통합 SNS 마케팅 자동화 플랫폼

## 📌 프로젝트 소개

ASG(All Social Gather)는 생성형 AI와 데이터 기반 마케팅 분석 기술을 융합한 통합 SNS 마케팅 자동화 플랫폼입니다.

소상공인이 반복적으로 수행해야 하는 SNS 콘텐츠 제작, 게시 관리, 고객 응대, 성과 분석 업무를 하나의 플랫폼에서 통합 관리할 수 있도록 설계되었습니다.

기존 SNS 운영 환경은 콘텐츠 제작, 이미지 생성, 게시 일정 관리, 댓글 및 리뷰 대응, 성과 분석이 각각 분리되어 있어 운영 효율이 낮고 많은 시간과 비용이 요구되었습니다.  
ASG는 이러한 문제를 해결하기 위해 생성형 AI, 외부 SNS API, 데이터 분석 기술을 결합하여 최소 입력만으로 콘텐츠 생성부터 예약 게시, 고객 반응 대응, 전략 추천까지 자동화된 마케팅 운영 환경을 제공합니다.

특히 Instagram, Facebook, 네이버 플레이스, 카카오맵, Google 리뷰 등 다양한 플랫폼의 데이터를 통합 관리할 수 있도록 설계하여 소상공인의 SNS 운영 시간을 최대 80%까지 절감하는 것을 핵심 목표로 합니다.

---

# 🎯 프로젝트 목표

- 생성형 AI 기반 SNS 콘텐츠 자동 생성
- 플랫폼별 최적화 콘텐츠 자동 제공
- 예약 게시 및 일정 관리 자동화
- 리뷰 및 댓글 통합 관리
- 데이터 기반 마케팅 전략 추천
- AI 기반 고객 응답 초안 생성
- 콘텐츠 생성 → 게시 → 고객 대응 → 성과 분석까지 단일 플랫폼 통합

---

# ✨ 주요 기능

## A. 계정 및 사용자 설정

- OAuth 2.0 기반 소셜 로그인 지원
- 업종별 비즈니스 정보 및 키워드 설정
- AI 콘텐츠 생성 개인화 설정 제공
  - 말투 설정
  - 이모지 사용 여부
  - 글자 수 제한
  - 인트로/아웃트로 템플릿 설정

---

## B. AI 콘텐츠 자동 생성

- 상품명 및 키워드 최소 입력만으로 SNS 콘텐츠 자동 생성
- 플랫폼 특성 기반 콘텐츠 생성
  - Instagram → 감성형 콘텐츠
  - Facebook → 참여 유도형 콘텐츠
- Google Gemini API 기반 텍스트 생성
- Pexels API 기반 무료 이미지 추천
- Stability AI 기반 AI 이미지 생성
- MyMemory API 기반 한글 프롬프트 영어 전처리
- Cloudinary 기반 이미지 자산 관리
- Meta Graph API 기반 Instagram / Facebook 자동 게시

---

## C. 콘텐츠 가이드 기능

- SNS 플랫폼별 최적 게시 형식 제공
- 업종별 인기 해시태그 추천
- 실시간 키워드 추천 기능 제공

---

## D. 스마트 마케팅 캘린더

- 게시 일정 시각화
- 드래그 앤 드롭 기반 일정 변경
- 콘텐츠 상태 자동 관리
  - `PENDING`
  - `SCHEDULED`

---

## E. 고객 반응 통합 관리

- Instagram / Facebook / 네이버 플레이스 / 카카오맵 / Google 리뷰 통합 수집
- AI 기반 답글 초안 자동 생성
- 브랜드 페르소나 기반 맞춤 응답 생성
- 자동 답변(Auto) 및 관리자 검토 기반 답변(Manual) 지원

---

## F. 채널 성과 분석

- 좋아요 / 댓글 / 공유 / 팔로워 / 리뷰 평점 통합 시각화
- 네이버 DataLab 검색량 분석
- SerpAPI 기반 키워드 트렌드 분석
- 주간 / 월간 / 연간 성과 비교 분석 제공
- AI 기반 업로드 전략 추천

---

## G. 고객센터 및 관리자 기능

- FAQ 및 공지사항 관리
- 문의 등록 및 첨부파일 업로드
- 관리자 문의 상태 관리
- 답변 등록 / 수정 / 삭제
- 이메일 자동 발송 기능 제공

---

## H. 마이페이지

- 매장 정보 및 영업시간 관리
- 대표 이미지 관리(Cloudinary)
- SNS 계정 연동 상태 관리
- 콘텐츠 생성 이력 조회
- 회원 탈퇴(Soft Delete) 지원

---

## I. AI 전략 추천

- 채널별 최적 업로드 시간 추천
- 채널별 포괄적인 작성 가이드 추천

---

# 🏗 시스템 아키텍처

## 1. Multi-Language Runtime Architecture

본 시스템은 AI 처리 성능과 서비스 안정성을 고려하여 Java 기반 메인 서버와 Python 기반 AI 서버를 분리한 Multi-Language Runtime 구조로 설계하였습니다.

### Main Backend (Spring Boot)

- 비즈니스 로직 처리
- OAuth 인증 및 권한 관리
- 회원 및 콘텐츠 데이터 관리
- 예약 게시 및 플랫폼 연동 관리

### AI Backend (Flask)

- 프롬프트 엔지니어링 처리
- Stability AI 이미지 생성 처리
- 이미지 필터링 및 전처리 수행
- AI 기반 응답 생성 처리

### Database

- RDBMS 기반 데이터 저장 구조
- 회원 / 콘텐츠 / 리뷰 / 플랫폼 연동 데이터 관리
- JPA + MyBatis 기반 데이터 영속화 처리

---

# 🔄 주요 프로세스 설계

## 1. AI 콘텐츠 생성 및 배포 프로세스

```text
사용자 키워드 입력
        ↓
MyMemory 기반 번역 수행
        ↓
PromptEngine 기반 프롬프트 최적화
        ↓
Stability AI 이미지 생성
        ↓
Gemini API 기반 SNS 문구 생성
        ↓
스마트 캘린더 등록
        ↓
예약 시간 도달
        ↓
Meta Graph API 자동 게시
```

---

## 2. 통합 리뷰 대응 프로세스

```text
리뷰 데이터 수집
        ↓
JSON ObjectMapper 기반 데이터 정규화
        ↓
브랜드 페르소나 로드
        ↓
Gemini API 기반 답변 초안 생성
        ↓
자동 답변 또는 관리자 검토 후 게시
```

---

## 3. 데이터 분석 및 전략 추천 프로세스

```text
채널별 성과 데이터 수집
        ↓
통합 지표 집계
        ↓
검색 트렌드 분석
        ↓
키워드 마인드맵 생성
        ↓
AI 기반 업로드 전략 추천
```

---

# 🗂 데이터 도메인 모델

## Member
- 회원 정보
- OAuth 로그인 정보
- 회원 상태 관리 (`ACTIVE`, `INACTIVE`)

## Brand
- 매장 정보
- 위치 및 영업시간 관리

## BrandPlatform
- SNS 플랫폼 연동 정보
- Access Token 및 연동 상태 관리

## ContentPost
- 생성 콘텐츠 정보
- 게시 상태 및 예약 일정 관리

## CustomerResponse
- 리뷰 및 댓글 데이터
- 감성 분석 결과
- AI 답변 초안 및 처리 상태 관리

---

# ⚙ 기술적 특이사항

## 시스템 일관성 유지

- 영업시간 저장 시 delete-then-reinsert 전략 적용
- 데이터 정합성 유지 및 복잡한 수정 로직 단순화

## AI 처리 효율성 향상

- Flask AI 서버 분리 구조 적용
- 무거운 AI 연산을 메인 서버와 분리하여 성능 최적화

## 사용자 경험 강화

- 플랫폼별 콘텐츠 미리보기 기능 제공
- 게시 전 최종 검수 단계 지원
- 드래그 앤 드롭 기반 일정 관리

## 법적 준수 사항 반영

- 개인정보 및 마케팅 수신 동의 이력 관리 기능 포함
- Soft Delete 기반 회원 탈퇴 처리 지원

## 통합 마케팅 운영 환경 제공

- 콘텐츠 생성 → 예약 → 게시 → 고객 대응 → 성과 분석까지 단일 플랫폼에서 수행 가능하도록 설계

---

# 🛠 기술 스택

## 개발환경

| 구분 | 기술 |
|---|---|
| OS | Windows 11 |
| IDE & Tools | STS (Spring Tool Suite), VS Code, Postman |
| Build Tool | Maven |
| Version Control | Git, GitHub |
| Runtime | JDK 17 |
| Deployment | 로컬 기반 개발 환경 |
| Cloud Storage | Cloudinary |

---

## Backend

| 구분 | 기술 |
|---|---|
| Language | Java 17+ |
| Framework | Spring Boot |
| Security | Spring Security, OAuth2, JWT |
| Persistence | Spring Data JPA, MyBatis |
| Template Engine | Thymeleaf |
| Mail | Gmail SMTP |
| Server | Servlet |

---

## AI Backend

| 구분 | 기술 |
|---|---|
| Language | Python |
| Framework | Flask |
| AI Processing | Prompt Engineering, AI Image Processing |

---

## Frontend

| 구분 | 기술 |
|---|---|
| Core | HTML5, CSS3, JavaScript |
| Visualization | Chart.js |
| Calendar | FullCalendar |
| Template Engine | Thymeleaf |

---

## Database

| 구분 | 기술 |
|---|---|
| DBMS | MySQL |

---

## AI & 분석

| 구분 | 기술 |
|---|---|
| LLM | Google Gemini API |
| Image Generation | Stability AI |
| Trend Analysis | SerpApi, Naver DataLab |
| Translation | MyMemory API |

---

## External APIs

### 인증 API
- Kakao Developers API
- Google OAuth2 API
- Naver Developers API

### SNS & 마케팅 API
- Meta Graph API (Instagram / Facebook)
- Naver DataLab API
- SerpApi
- YouTube Data API

### AI API
- Google Gemini API
- Stability AI API
- MyMemory API

### 알림 및 미디어 API
- Firebase Cloud Messaging API
- Cloudinary API
- Pexels API

---

# 📈 기대 효과

- 소상공인의 SNS 운영 시간 최대 80% 절감
- 콘텐츠 제작 비용 절감
- 플랫폼별 운영 효율 향상
- 고객 응대 속도 개선
- 데이터 기반 마케팅 전략 수립 가능
- 통합 관리 환경 제공을 통한 운영 생산성 향상

---

# 🚀 결론

ASG(All Social Gather)는 생성형 AI와 데이터 기반 분석 기술을 결합하여 콘텐츠 생성, 예약 게시, 고객 응대, 성과 분석까지 SNS 마케팅의 전체 사이클을 하나의 플랫폼에서 수행할 수 있도록 설계된 통합 마케팅 자동화 시스템입니다.

단순한 콘텐츠 생성 도구를 넘어, 소상공인이 실제 운영 과정에서 반복적으로 겪는 문제를 해결하고 데이터 기반 의사결정을 지원하는 AI 마케팅 운영 플랫폼을 목표로 개발되었습니다.

---

<div align="center">

Made with 📢 by Team All Social Gather

</div>
