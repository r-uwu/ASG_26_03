/* 
  0_gather-setting.sql : DB, table 생성 및 최소한의 seed data insert

  정렬 및 실행 순서
  create > index > alter > 최소한의 seed

  [실행 순서 보장 근거]
  platform          : brand_platform 의 platform_id FK 선행 필요
  brand             : brand_platform 의 brand_id FK 선행 필요
  brand_platform    : content_post / platform_metric_daily / performance_impact_analysis 의 brand_platform_id FK 선행 필요
  date_dimension    : content_post(published_date_key) / *_metric_daily 의 date_key FK 선행 필요
  content_post      : post_metric_daily 의 post_id FK 선행 필요
  users             : brand UPDATE(user_id) 는 users INSERT 이후 → 마지막 블록
  customer_feedback : FK 없음 (독립 테이블) — 실행 순서 무관
  sns_guides        : FK 없음 (독립 테이블) — 실행 순서 무관
  keywords          : FK 없음 (독립 테이블) — 실행 순서 무관

  [customerCenter 관련 테이블]
  inquiry        : type / body / attachment_names / attachment_saved_names 컬럼 포함, status 기본값 '미처리'
  notice         : 공지사항 (독립 테이블, FK 없음)
  faq            : 자주묻는질문 (독립 테이블, FK 없음)
  reply          : inquiry_id 논리적 참조 (물리 FK 없음 — cascade 충돌 방지)
*/

DROP DATABASE IF EXISTS gather;
CREATE DATABASE gather;
USE gather;


-- ══════════════════════════════════════════════
-- CREATE TABLE
-- ══════════════════════════════════════════════

-- users
CREATE TABLE `users` (
  `id`                 BIGINT       NOT NULL AUTO_INCREMENT,
  `email`              VARCHAR(100) NOT NULL,
  `name`               VARCHAR(50)  NULL,
  `nickname`           VARCHAR(50)  NULL,
  `provider`           VARCHAR(20)  NOT NULL COMMENT 'kakao / naver / google',
  `provider_id`        VARCHAR(255) NOT NULL,
  `contact_phone`      VARCHAR(20)  NULL,
  `company_name`       VARCHAR(100) NULL,
  `business_category`  VARCHAR(50)  NULL,
  `preferred_channel`  VARCHAR(20)  NULL,
  `store_phone_number` VARCHAR(20)  NULL,
  `road_addr_part1`    VARCHAR(200) NULL COMMENT '도로명주소 본체',
  `addr_detail`        VARCHAR(100) NULL COMMENT '상세주소',
  `terms_agreed`       TINYINT(1)   NOT NULL DEFAULT 0,
  `privacy_agreed`     TINYINT(1)   NOT NULL DEFAULT 0,
  `location_agreed`    TINYINT(1)   NOT NULL DEFAULT 0,
  `marketing_consent`  TINYINT(1)   NULL,
  `event_consent`      TINYINT(1)   NULL,
  `signup_completed`   TINYINT(1)   NOT NULL DEFAULT 0,
  `status`             VARCHAR(30)  NOT NULL DEFAULT 'SIGNUP_PENDING' COMMENT 'SIGNUP_PENDING / ACTIVE',
  `role`               VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER',
  `created_at`         DATETIME     NULL,
  `updated_at`         DATETIME     NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_users_provider` (`provider`, `provider_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4;

-- refresh_token (JWT 인증용)
CREATE TABLE `refresh_token` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `token` VARCHAR(500) NOT NULL,
  `user_id` BIGINT NOT NULL,
  `expires_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_refresh_token` (`token`),
  KEY `idx_refresh_user` (`user_id`),
  CONSTRAINT `fk_refresh_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
    ON DELETE CASCADE
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4;

-- business_hours
CREATE TABLE `business_hours` (
  `id`          BIGINT     NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT     NOT NULL,
  `day_of_week` TINYINT    NOT NULL COMMENT '0=월 1=화 2=수 3=목 4=금 5=토 6=일',
  `is_open`     TINYINT(1) NOT NULL DEFAULT 1,
  `open_time`   VARCHAR(5) NULL     COMMENT 'HH:mm',
  `close_time`  VARCHAR(5) NULL     COMMENT 'HH:mm',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_bh_user_day` (`user_id`, `day_of_week`),
  CONSTRAINT `fk_bh_user` FOREIGN KEY (`user_id`)
    REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4;

-- content_settings
CREATE TABLE `content_settings` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`        BIGINT       NOT NULL,
  `intro_template` VARCHAR(100) NULL,
  `outro_template` VARCHAR(100) NULL,
  `tone`           VARCHAR(20)  NOT NULL DEFAULT '기본',
  `emoji_level`    VARCHAR(10)  NOT NULL DEFAULT '적당히',
  `target_length`  INT          NOT NULL DEFAULT 300,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_cs_user` (`user_id`),
  CONSTRAINT `fk_cs_user` FOREIGN KEY (`user_id`)
    REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4;

-- brand
CREATE TABLE `brand` (
  `brand_id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`           BIGINT       NULL         COMMENT '유저 ID (로그인 연동 후 사용)',
  `brand_name`        VARCHAR(100) NOT NULL     COMMENT '브랜드명',
  `service_name`      TEXT         NULL         COMMENT 'sns 계정 아이디',
  `industry_type`     VARCHAR(50)  NULL         COMMENT '업종',
  `location_name`     VARCHAR(150) NULL         COMMENT '매장명/지점명',
  `address`           VARCHAR(255) NULL         COMMENT '도로명 주소',
  `phone`             VARCHAR(20)  NULL         COMMENT '가게 전화번호',
  `profile_image_url` VARCHAR(500) NULL         COMMENT '대표 이미지 URL',
  `created_at`        DATETIME     NOT NULL     COMMENT '생성일시',
  `updated_at`        DATETIME     NOT NULL     COMMENT '수정일시',
  PRIMARY KEY (`brand_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='브랜드/매장 기준 정보';

-- brand_operation_profile
CREATE TABLE `brand_operation_profile` (
  `operation_profile_id`   BIGINT      NOT NULL AUTO_INCREMENT,
  `brand_id`               BIGINT      UNIQUE NOT NULL COMMENT '브랜드 ID',
  `open_time`              TIME        NULL   COMMENT '영업 시작 시간',
  `close_time`             TIME        NULL   COMMENT '영업 종료 시간',
  `regular_closed_weekday` TINYINT     NULL   COMMENT '정기 휴무 요일(1=월 ~ 7=일)',
  `weekend_impact_type`    VARCHAR(20) NULL   COMMENT 'positive / neutral / negative',
  `holiday_impact_type`    VARCHAR(20) NULL   COMMENT 'positive / neutral / negative',
  `peak_business_time`     VARCHAR(50) NULL   COMMENT '피크 영업 시간대',
  `note`                   TEXT        NULL   COMMENT '운영 특이사항',
  `created_at`             DATETIME    NOT NULL COMMENT '생성일시',
  `updated_at`             DATETIME    NOT NULL COMMENT '수정일시',
  PRIMARY KEY (`operation_profile_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='사업장 운영 특성 정보';

-- platform
CREATE TABLE `platform` (
  `platform_id`   BIGINT      NOT NULL AUTO_INCREMENT,
  `platform_code` VARCHAR(30) UNIQUE NOT NULL COMMENT 'instagram / facebook / naver / kakao / google',
  `platform_name` VARCHAR(50) NOT NULL,
  `brand_color`   VARCHAR(20) NULL,
  `is_active`     BOOLEAN     NOT NULL DEFAULT TRUE,
  PRIMARY KEY (`platform_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='플랫폼 마스터';

-- brand_platform
CREATE TABLE `brand_platform` (
  `brand_platform_id` BIGINT       NOT NULL AUTO_INCREMENT,
  `brand_id`          BIGINT       NOT NULL,
  `platform_id`       BIGINT       NOT NULL,
  `channel_name`      VARCHAR(100) NULL,
  `channel_url`       VARCHAR(255) NULL,
  `is_connected`      BOOLEAN      NOT NULL DEFAULT FALSE,
  `access_token`      TEXT         NULL     COMMENT 'SNS 액세스 토큰',
  `refresh_token`     TEXT         NULL     COMMENT 'SNS 리프레시 토큰',
  `token_expires_at`  DATETIME     NULL     COMMENT '토큰 만료 일시',
  `token_status`      VARCHAR(20)  NULL     DEFAULT 'ACTIVE' COMMENT 'ACTIVE / EXPIRED',
  `connected_at`      DATETIME     NULL,
  `created_at`        DATETIME     NOT NULL,
  `updated_at`        DATETIME     NOT NULL,
  PRIMARY KEY (`brand_platform_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='브랜드별 운영 채널 정보';

-- customer_feedback
CREATE TABLE `customer_feedback` (
  `feedback_id`   BIGINT(20)   NOT NULL AUTO_INCREMENT              COMMENT '피드백 ID',
  `external_id`   VARCHAR(255) NOT NULL UNIQUE                      COMMENT '플랫폼별 댓글 고유 ID',
  `author_name`   VARCHAR(255) NULL                                 COMMENT '작성자명',
  `original_text` TEXT         NULL                                 COMMENT '원문',
  `origin_url`    VARCHAR(255) NULL                                 COMMENT '원문 링크',
  `platform`      VARCHAR(50)  NULL                                 COMMENT '플랫폼',
  `type`          ENUM('COMMENT','REVIEW')                          NULL COMMENT '피드백 타입',
  `status`        ENUM('CHECKED','COMPLETED','SENDING','UNCHECKED','UNRESOLVED') NULL COMMENT '전체 진행 상태',
  `ai_status`     ENUM('DONE','IDLE')      NULL                     COMMENT 'AI 응답 상태',
  `ai_reply`      TEXT         NULL                                 COMMENT 'AI 응답 내용',
  `sent_reply`    TEXT         NULL                                 COMMENT '보낸 응답 내용',
  `source_id`     BIGINT(20)   NULL                                 COMMENT '플랫폼별 게시글 고유 ID',
  `created_at`    DATETIME     NULL                                 COMMENT '댓글 작성일',
  `updated_at`    DATETIME     NULL                                 COMMENT '피드백 업데이트일',
  PRIMARY KEY (`feedback_id`),
  INDEX `idx_customer_feedback_created_at` (`created_at` DESC)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='피드백 리스트';

-- 채널 성과 분석 테이블
CREATE TABLE `date_dimension` (
  `date_key`        INT          NOT NULL,
  `full_date`       DATE         UNIQUE NOT NULL,
  `year_no`         INT          NOT NULL,
  `half_no`         TINYINT      NOT NULL,
  `quarter_no`      TINYINT      NOT NULL,
  `month_no`        TINYINT      NOT NULL,
  `month_name`      VARCHAR(20)  NULL,
  `week_of_year`    TINYINT      NULL,
  `week_of_month`   TINYINT      NULL,
  `day_of_month`    TINYINT      NOT NULL,
  `day_of_week`     TINYINT      NOT NULL,
  `day_name_kr`     VARCHAR(10)  NOT NULL,
  `is_weekend`      BOOLEAN      NOT NULL DEFAULT FALSE,
  `is_holiday`      BOOLEAN      NOT NULL DEFAULT FALSE,
  `is_business_day` BOOLEAN      NOT NULL DEFAULT TRUE,
  `is_month_start`  BOOLEAN      NOT NULL DEFAULT FALSE,
  `is_month_end`    BOOLEAN      NOT NULL DEFAULT FALSE,
  `is_year_start`   BOOLEAN      NOT NULL DEFAULT FALSE,
  `is_year_end`     BOOLEAN      NOT NULL DEFAULT FALSE,
  `season_code`     VARCHAR(20)  NULL,
  `holiday_name`    VARCHAR(100) NULL,
  PRIMARY KEY (`date_key`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `content_post` (
  `post_id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `brand_platform_id`  BIGINT       NOT NULL,
  `post_title`         VARCHAR(200) NULL,
  `post_type`          VARCHAR(50)  NULL,
  `post_body`          TEXT         NULL,
  `published_at`       DATETIME     NULL,
  `published_date_key` INT          NULL,
  `published_hour`     TINYINT      NULL,
  `status`             VARCHAR(30)  NOT NULL DEFAULT 'published',
  `created_at`         DATETIME     NOT NULL,
  `updated_at`         DATETIME     NOT NULL,
  PRIMARY KEY (`post_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `post_metric_daily` (
  `post_metric_daily_id` BIGINT   NOT NULL AUTO_INCREMENT,
  `post_id`              BIGINT   NOT NULL,
  `date_key`             INT      NOT NULL,
  `like_count`           INT      NOT NULL DEFAULT 0,
  `comment_count`        INT      NOT NULL DEFAULT 0,
  `share_count`          INT      NOT NULL DEFAULT 0,
  `follower_gain`        INT      NOT NULL DEFAULT 0,
  `review_count`         INT      NOT NULL DEFAULT 0,
  `save_count`           INT      NOT NULL DEFAULT 0,
  `click_count`          INT      NOT NULL DEFAULT 0,
  `created_at`           DATETIME NOT NULL,
  PRIMARY KEY (`post_metric_daily_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `platform_metric_daily` (
  `platform_metric_daily_id` BIGINT        NOT NULL AUTO_INCREMENT,
  `brand_platform_id`        BIGINT        NOT NULL,
  `date_key`                 INT           NOT NULL,
  `total_likes`              INT           NOT NULL DEFAULT 0,
  `total_comments`           INT           NOT NULL DEFAULT 0,
  `total_shares`             INT           NOT NULL DEFAULT 0,
  `total_reviews`            INT           NOT NULL DEFAULT 0,
  `follower_growth`          INT           NOT NULL DEFAULT 0,
  `engagement_score`         DECIMAL(10,2) NOT NULL DEFAULT 0,
  `created_at`               DATETIME      NOT NULL,
  PRIMARY KEY (`platform_metric_daily_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `platform_hourly_metric` (
  `hourly_metric_id`  BIGINT   NOT NULL AUTO_INCREMENT,
  `brand_platform_id` BIGINT   NOT NULL,
  `date_key`          INT      NOT NULL,
  `hour_of_day`       TINYINT  NOT NULL,
  `like_count`        INT      NOT NULL DEFAULT 0,
  `comment_count`     INT      NOT NULL DEFAULT 0,
  `share_count`       INT      NOT NULL DEFAULT 0,
  `created_at`        DATETIME NOT NULL,
  PRIMARY KEY (`hourly_metric_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `search_performance_daily` (
  `search_perf_id` BIGINT      NOT NULL AUTO_INCREMENT,
  `brand_id`       BIGINT      NOT NULL,
  `date_key`       INT         NOT NULL,
  `search_engine`  VARCHAR(30) NOT NULL,
  `search_count`   INT         NOT NULL DEFAULT 0,
  `click_count`    INT         NOT NULL DEFAULT 0,
  `created_at`     DATETIME    NOT NULL,
  PRIMARY KEY (`search_perf_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `keyword_master` (
  `keyword_id`   BIGINT       NOT NULL AUTO_INCREMENT,
  `brand_id`     BIGINT       NOT NULL,
  `keyword_text` VARCHAR(100) NOT NULL,
  `keyword_type` VARCHAR(30)  NULL,
  `created_at`   DATETIME     NOT NULL,
  PRIMARY KEY (`keyword_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `keyword_performance_daily` (
  `keyword_perf_id` BIGINT   NOT NULL AUTO_INCREMENT,
  `keyword_id`      BIGINT   NOT NULL,
  `date_key`        INT      NOT NULL,
  `search_count`    INT      NOT NULL DEFAULT 0,
  `click_count`     INT      NOT NULL DEFAULT 0,
  `rank_no`         INT      NULL,
  `created_at`      DATETIME NOT NULL,
  PRIMARY KEY (`keyword_perf_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `platform_metric_monthly_summary` (
  `monthly_summary_id`  BIGINT        NOT NULL AUTO_INCREMENT,
  `brand_platform_id`   BIGINT        NOT NULL,
  `year_no`             INT           NOT NULL,
  `month_no`            TINYINT       NOT NULL,
  `start_date_key`      INT           NOT NULL,
  `end_date_key`        INT           NOT NULL,
  `total_likes`         INT           NOT NULL DEFAULT 0,
  `total_comments`      INT           NOT NULL DEFAULT 0,
  `total_shares`        INT           NOT NULL DEFAULT 0,
  `total_reviews`       INT           NOT NULL DEFAULT 0,
  `follower_growth`     INT           NOT NULL DEFAULT 0,
  `engagement_score`    DECIMAL(10,2) NOT NULL DEFAULT 0,
  `channel_share_ratio` DECIMAL(5,2)  NOT NULL DEFAULT 0,
  `created_at`          DATETIME      NOT NULL,
  `updated_at`          DATETIME      NOT NULL,
  PRIMARY KEY (`monthly_summary_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `platform_metric_yearly_summary` (
  `yearly_summary_id`   BIGINT        NOT NULL AUTO_INCREMENT,
  `brand_platform_id`   BIGINT        NOT NULL,
  `year_no`             INT           NOT NULL,
  `start_date_key`      INT           NOT NULL,
  `end_date_key`        INT           NOT NULL,
  `total_likes`         INT           NOT NULL DEFAULT 0,
  `total_comments`      INT           NOT NULL DEFAULT 0,
  `total_shares`        INT           NOT NULL DEFAULT 0,
  `total_reviews`       INT           NOT NULL DEFAULT 0,
  `follower_growth`     INT           NOT NULL DEFAULT 0,
  `engagement_score`    DECIMAL(10,2) NOT NULL DEFAULT 0,
  `channel_share_ratio` DECIMAL(5,2)  NOT NULL DEFAULT 0,
  `created_at`          DATETIME      NOT NULL,
  `updated_at`          DATETIME      NOT NULL,
  PRIMARY KEY (`yearly_summary_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `platform_metric_monthly_trend` (
  `monthly_trend_id`  BIGINT   NOT NULL AUTO_INCREMENT,
  `brand_platform_id` BIGINT   NOT NULL,
  `year_no`           INT      NOT NULL,
  `month_no`          TINYINT  NOT NULL,
  `likes`             INT      NOT NULL DEFAULT 0,
  `comments`          INT      NOT NULL DEFAULT 0,
  `shares`            INT      NOT NULL DEFAULT 0,
  `followers`         INT      NOT NULL DEFAULT 0,
  `created_at`        DATETIME NOT NULL,
  PRIMARY KEY (`monthly_trend_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `platform_metric_yearly_trend` (
  `yearly_trend_id`   BIGINT   NOT NULL AUTO_INCREMENT,
  `brand_platform_id` BIGINT   NOT NULL,
  `year_no`           INT      NOT NULL,
  `likes`             INT      NOT NULL DEFAULT 0,
  `comments`          INT      NOT NULL DEFAULT 0,
  `shares`            INT      NOT NULL DEFAULT 0,
  `followers`         INT      NOT NULL DEFAULT 0,
  `created_at`        DATETIME NOT NULL,
  PRIMARY KEY (`yearly_trend_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `performance_impact_analysis` (
  `impact_analysis_id`   BIGINT        NOT NULL AUTO_INCREMENT,
  `brand_platform_id`    BIGINT        NOT NULL,
  `analysis_period_type` VARCHAR(20)   NOT NULL,
  `base_year`            INT           NOT NULL,
  `base_month`           TINYINT       NULL,
  `weekend_effect_score` DECIMAL(10,2) NOT NULL DEFAULT 0,
  `holiday_effect_score` DECIMAL(10,2) NOT NULL DEFAULT 0,
  `best_day_of_week`     TINYINT       NULL,
  `worst_day_of_week`    TINYINT       NULL,
  `best_hour_range`      VARCHAR(50)   NULL,
  `created_at`           DATETIME      NOT NULL,
  PRIMARY KEY (`impact_analysis_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `strategy_recommendation` (
  `strategy_id`         BIGINT      NOT NULL AUTO_INCREMENT,
  `brand_id`            BIGINT      NOT NULL,
  `period_type`         VARCHAR(20) NOT NULL,
  `based_on_start_date` DATE        NOT NULL,
  `based_on_end_date`   DATE        NOT NULL,
  `summary_text`        TEXT        NULL,
  `generated_at`        DATETIME    NOT NULL,
  `created_at`          DATETIME    NOT NULL,
  PRIMARY KEY (`strategy_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `strategy_recommendation_item` (
  `strategy_item_id`      BIGINT       NOT NULL AUTO_INCREMENT,
  `strategy_id`           BIGINT       NOT NULL,
  `sort_order`            INT          NOT NULL,
  `recommendation_title`  VARCHAR(100) NOT NULL,
  `platform_id`           BIGINT       NOT NULL,
  `recommended_time_slot` VARCHAR(50)  NULL,
  `content_type`          VARCHAR(100) NULL,
  `detail_text`           TEXT         NULL,
  `created_at`            DATETIME     NOT NULL,
  PRIMARY KEY (`strategy_item_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4;

-- customerCenter 관련 테이블
CREATE TABLE `inquiry` (
  `id`                    BIGINT       NOT NULL AUTO_INCREMENT,
  `type`                  VARCHAR(50)  NULL     COMMENT '문의 유형 (가입·연동/콘텐츠 생성/시스템오류/계정/기타)',
  `email`                 VARCHAR(255) NOT NULL,
  `title`                 VARCHAR(255) NOT NULL,
  `body`                  TEXT         NULL     COMMENT '문의 본문 (content 컬럼과 동일 역할, customerCenter 호환용)',
  `content`               TEXT         NULL     COMMENT '문의 본문 (myPage 기존 호환용)',
  `status`                VARCHAR(50)  NOT NULL DEFAULT '미처리' COMMENT '미처리 / 처리중 / 처리완료',
  `attachment_names`      TEXT         NULL     COMMENT '첨부파일 원본 파일명 (콤마 구분)',
  `attachment_saved_names` TEXT        NULL     COMMENT '첨부파일 서버 저장명 (콤마 구분)',
  `created_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `notice` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `title`      VARCHAR(200) NOT NULL,
  `content`    LONGTEXT     NOT NULL,
  `category`   VARCHAR(30)  NOT NULL DEFAULT '공지' COMMENT '공지 / 업데이트 / 점검',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='공지사항';

CREATE TABLE `faq` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `category`   VARCHAR(100) NOT NULL COMMENT '가입·연동 / 콘텐츠 생성 / 시스템오류 / 계정',
  `question`   VARCHAR(255) NOT NULL,
  `answer`     LONGTEXT     NOT NULL,
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='자주 묻는 질문';

CREATE TABLE `reply` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT,
  `inquiry_id` BIGINT   NOT NULL COMMENT 'inquiry.id 논리적 참조',
  `content`    TEXT     NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_reply_inquiry_id` (`inquiry_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='문의 답변';

-- sns_guides (콘텐츠 생성 가이드 — 플랫폼별 업로드 시간 및 멘트)
CREATE TABLE `sns_guides` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `platform`      VARCHAR(30)  NOT NULL COMMENT 'instagram / facebook / naver / kakao / community',
  `guide_details` JSON         NOT NULL COMMENT '가이드 문구 + 추천 시간 JSON 배열',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_sns_guides_platform` (`platform`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='플랫폼별 콘텐츠 업로드 가이드';

-- keywords (업종별 키워드 마스터)
CREATE TABLE `keywords` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `industry_code` VARCHAR(20)  NOT NULL COMMENT 'CAFE / FOOD / BEAUTY / FASHION / STAY / SPORTS / EDU / MED / RETAIL / DEFAULT',
  `category`      VARCHAR(50)  NOT NULL COMMENT '키워드 분류',
  `name`          VARCHAR(100) NOT NULL COMMENT '키워드명',
  PRIMARY KEY (`id`),
  KEY `idx_keywords_industry` (`industry_code`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='업종별 키워드 마스터';

-- ══════════════════════════════════════════════
-- CREATE INDEX
-- ══════════════════════════════════════════════
CREATE UNIQUE INDEX `brand_platform_index_0`                    ON `brand_platform`                  (`brand_id`, `platform_id`);
CREATE UNIQUE INDEX `post_metric_daily_index_6`                 ON `post_metric_daily`               (`post_id`, `date_key`);
CREATE UNIQUE INDEX `platform_metric_daily_index_8`             ON `platform_metric_daily`           (`brand_platform_id`, `date_key`);
CREATE UNIQUE INDEX `platform_hourly_metric_index_10`           ON `platform_hourly_metric`          (`brand_platform_id`, `date_key`, `hour_of_day`);
CREATE UNIQUE INDEX `search_performance_daily_index_12`         ON `search_performance_daily`        (`brand_id`, `search_engine`, `date_key`);
CREATE UNIQUE INDEX `keyword_master_index_14`                   ON `keyword_master`                  (`brand_id`, `keyword_text`);
CREATE UNIQUE INDEX `keyword_performance_daily_index_15`        ON `keyword_performance_daily`       (`keyword_id`, `date_key`);
CREATE UNIQUE INDEX `platform_metric_monthly_summary_index_18`  ON `platform_metric_monthly_summary` (`brand_platform_id`, `year_no`, `month_no`);
CREATE UNIQUE INDEX `platform_metric_yearly_summary_index_21`   ON `platform_metric_yearly_summary`  (`brand_platform_id`, `year_no`);
CREATE UNIQUE INDEX `platform_metric_monthly_trend_index_23`    ON `platform_metric_monthly_trend`   (`brand_platform_id`, `year_no`, `month_no`);
CREATE UNIQUE INDEX `platform_metric_yearly_trend_index_24`     ON `platform_metric_yearly_trend`    (`brand_platform_id`, `year_no`);
CREATE UNIQUE INDEX `performance_impact_analysis_index_25`      ON `performance_impact_analysis`     (`brand_platform_id`, `analysis_period_type`, `base_year`, `base_month`);
CREATE UNIQUE INDEX `strategy_recommendation_item_index_27`     ON `strategy_recommendation_item`    (`strategy_id`, `sort_order`);


-- ══════════════════════════════════════════════
-- ALTER TABLE (FK)
-- ══════════════════════════════════════════════
ALTER TABLE `content_settings`
  ADD COLUMN `preferred_sns` VARCHAR(100) NULL COMMENT 'comma-separated: instagram,naver,kakao,facebook';

ALTER TABLE `brand`                           ADD FOREIGN KEY (`user_id`)            REFERENCES `users`           (`id`);
ALTER TABLE `brand_operation_profile`         ADD FOREIGN KEY (`brand_id`)           REFERENCES `brand`           (`brand_id`);
ALTER TABLE `brand_platform`                  ADD FOREIGN KEY (`brand_id`)           REFERENCES `brand`           (`brand_id`);
ALTER TABLE `brand_platform`                  ADD FOREIGN KEY (`platform_id`)        REFERENCES `platform`        (`platform_id`);
ALTER TABLE `content_post`                    ADD FOREIGN KEY (`brand_platform_id`)  REFERENCES `brand_platform`  (`brand_platform_id`);
ALTER TABLE `content_post`                    ADD FOREIGN KEY (`published_date_key`) REFERENCES `date_dimension`  (`date_key`);
ALTER TABLE `post_metric_daily`               ADD FOREIGN KEY (`post_id`)            REFERENCES `content_post`    (`post_id`);
ALTER TABLE `post_metric_daily`               ADD FOREIGN KEY (`date_key`)           REFERENCES `date_dimension`  (`date_key`);
ALTER TABLE `platform_metric_daily`           ADD FOREIGN KEY (`brand_platform_id`)  REFERENCES `brand_platform`  (`brand_platform_id`);
ALTER TABLE `platform_metric_daily`           ADD FOREIGN KEY (`date_key`)           REFERENCES `date_dimension`  (`date_key`);
ALTER TABLE `platform_hourly_metric`          ADD FOREIGN KEY (`brand_platform_id`)  REFERENCES `brand_platform`  (`brand_platform_id`);
ALTER TABLE `platform_hourly_metric`          ADD FOREIGN KEY (`date_key`)           REFERENCES `date_dimension`  (`date_key`);
ALTER TABLE `search_performance_daily`        ADD FOREIGN KEY (`brand_id`)           REFERENCES `brand`           (`brand_id`);
ALTER TABLE `search_performance_daily`        ADD FOREIGN KEY (`date_key`)           REFERENCES `date_dimension`  (`date_key`);
ALTER TABLE `keyword_master`                  ADD FOREIGN KEY (`brand_id`)           REFERENCES `brand`           (`brand_id`);
ALTER TABLE `keyword_performance_daily`       ADD FOREIGN KEY (`keyword_id`)         REFERENCES `keyword_master`  (`keyword_id`);
ALTER TABLE `keyword_performance_daily`       ADD FOREIGN KEY (`date_key`)           REFERENCES `date_dimension`  (`date_key`);
ALTER TABLE `platform_metric_monthly_summary` ADD FOREIGN KEY (`brand_platform_id`)  REFERENCES `brand_platform`  (`brand_platform_id`);
ALTER TABLE `platform_metric_monthly_summary` ADD FOREIGN KEY (`start_date_key`)     REFERENCES `date_dimension`  (`date_key`);
ALTER TABLE `platform_metric_monthly_summary` ADD FOREIGN KEY (`end_date_key`)       REFERENCES `date_dimension`  (`date_key`);
ALTER TABLE `platform_metric_yearly_summary`  ADD FOREIGN KEY (`brand_platform_id`)  REFERENCES `brand_platform`  (`brand_platform_id`);
ALTER TABLE `platform_metric_yearly_summary`  ADD FOREIGN KEY (`start_date_key`)     REFERENCES `date_dimension`  (`date_key`);
ALTER TABLE `platform_metric_yearly_summary`  ADD FOREIGN KEY (`end_date_key`)       REFERENCES `date_dimension`  (`date_key`);
ALTER TABLE `platform_metric_monthly_trend`   ADD FOREIGN KEY (`brand_platform_id`)  REFERENCES `brand_platform`  (`brand_platform_id`);
ALTER TABLE `platform_metric_yearly_trend`    ADD FOREIGN KEY (`brand_platform_id`)  REFERENCES `brand_platform`  (`brand_platform_id`);
ALTER TABLE `performance_impact_analysis`     ADD FOREIGN KEY (`brand_platform_id`)  REFERENCES `brand_platform`  (`brand_platform_id`);
ALTER TABLE `strategy_recommendation`         ADD FOREIGN KEY (`brand_id`)           REFERENCES `brand`           (`brand_id`);
ALTER TABLE `strategy_recommendation_item`    ADD FOREIGN KEY (`strategy_id`)        REFERENCES `strategy_recommendation` (`strategy_id`);
ALTER TABLE `strategy_recommendation_item`    ADD FOREIGN KEY (`platform_id`)        REFERENCES `platform`        (`platform_id`);


-- ══════════════════════════════════════════════
-- INSERT seed data (최소 시드 — 플랫폼 마스터, customerCenter 기반 데이터만)
-- ══════════════════════════════════════════════

-- 플랫폼 마스터 (google 포함 5개)
INSERT IGNORE INTO platform (platform_id, platform_code, platform_name, brand_color, is_active)
VALUES
(1, 'instagram', '인스타그램', '#E1306C', TRUE),
(2, 'facebook',  '페이스북',   '#1877F2', TRUE),
(3, 'naver',     '네이버',     '#03C75A', TRUE),
(4, 'kakao',     '카카오채널', '#FEE500', TRUE),
(5, 'google',    '구글',       '#4285F4', TRUE);

-- ══════════════════════════════════════════════
-- customerCenter 시드 데이터
-- 순서: faq → notice
-- ══════════════════════════════════════════════

-- FAQ
INSERT INTO faq (category, question, answer) VALUES
('가입·연동', '소셜 계정 연동은 어떻게 하나요?',
 '마이페이지 > SNS 연동 메뉴에서 연동할 소셜 계정을 선택하고 로그인 절차를 진행하시면 됩니다.'),
('가입·연동', '연동한 계정을 해제하려면 어떻게 하나요?',
 '마이페이지 > SNS 연동 메뉴에서 연동된 계정 옆의 해제 버튼을 클릭하시면 됩니다.'),
('콘텐츠 생성', 'AI 콘텐츠 자동 생성은 어떻게 사용하나요?',
 '콘텐츠 생성 메뉴에서 업종·톤·길이 등을 설정한 후 생성 버튼을 클릭하시면 AI가 자동으로 콘텐츠를 작성해 드립니다.'),
('콘텐츠 생성', '생성된 콘텐츠를 수정할 수 있나요?',
 '네, 생성 후 편집 화면에서 내용을 자유롭게 수정하실 수 있습니다.'),
('시스템오류', '게시물 업로드 중 오류가 발생할 경우 어떻게 하나요?',
 '잠시 후 재시도하시거나, 문제가 지속될 경우 고객센터 문의하기를 통해 오류 화면 캡처와 함께 접수해 주세요.'),
('계정', '회원 탈퇴는 어떻게 하나요?',
 '마이페이지 > 회원 탈퇴 메뉴에서 진행하실 수 있습니다. 탈퇴 후 데이터는 복구되지 않으니 신중히 결정해 주세요.'),
('계정', '이메일 주소를 변경하고 싶어요.',
 '현재 소셜 로그인 기반으로 운영되어 이메일 직접 변경은 지원되지 않습니다. 소셜 계정의 이메일을 변경하시면 자동으로 반영됩니다.');

-- 공지사항
INSERT INTO notice (category, title, content) VALUES
('공지',    '[공지] 소셜다모아 서비스 오픈 안내',
 '안녕하세요, 소셜다모아입니다.\n소셜다모아 정식 서비스가 오픈되었습니다. 많은 이용 부탁드립니다.\n문의사항은 고객센터를 통해 접수해 주세요.'),
('점검',    '[점검] 4월 정기 시스템 점검 안내 (04/20 02:00~04:00)',
 '정기 시스템 점검이 진행될 예정입니다.\n- 일시: 2026년 4월 20일(일) 02:00 ~ 04:00\n- 영향: 전 서비스 일시 중단\n점검 중에는 서비스 이용이 불가합니다.'),
('업데이트', '[업데이트] AI 콘텐츠 생성 기능 개선',
 'AI 콘텐츠 생성 품질이 향상되었습니다.\n- 업종별 맞춤 톤 적용 개선\n- 해시태그 추천 정확도 향상\n- 이미지 첨부 안정성 개선\n더 나은 서비스로 찾아뵙겠습니다.');