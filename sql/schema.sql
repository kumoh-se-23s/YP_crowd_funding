-- 테이블 세팅

-- 실행법
-- 1. 각자 MySQL에 빈 스키마를 만듦 (처음 한 번만): Workbench에서 아래 두 줄 query에 넣고 실행
--        CREATE DATABASE crowdfunding      CHARACTER SET utf8mb4
--        CREATE DATABASE crowdfunding_test CHARACTER SET utf8mb4
-- 2. Workbench에서 crowdfunding 스키마를 선택한 상태로 이 파일 실행
--    - 좌하단 Schemas 누르고 crowdfunding 더블클릭해서 이름 볼드체 되면 Ctrl+Shift+O 눌러서 이 파일 선택 후 실행하면 됨
--    - crowdfunding_test 스키마는 자체적으로 실행하게 짤거니 안 돌려도 됨
-- 3. 이후 필요에 따라 seed.sql 실행

-- 유의사항
-- 기존 테이블을 지우고 다시 만드는 것이므로, 재실행 시 기존 데이터가 사라짐에 유의
-- 테이블 구조를 바꿀 일이 생긴다면 팀 전체에 알린 후에 Workbench가 아니라 이 파일을 고쳐서 커밋하는 식으로 수정
-- 윈도우에선 MySQL이 모든 내용을 소문자로 바꿔 저장하는데, Linux에선 대소문자를 구분하므로 소문자로 통일
-- 이 파일 수정하면서 주석 쓸 때 세미콜론 붙이면 안됨 에러남

-- 컨벤션
-- 제약조건 이름은 fk_테이블_대상, uk_테이블_컬럼, ck_테이블_컬럼 등으로 통일

-- 0. 기존 테이블 삭제 (데이터 초기화)
DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS canceled_funds;
DROP TABLE IF EXISTS funds;
DROP TABLE IF EXISTS likes;
DROP TABLE IF EXISTS image;
DROP TABLE IF EXISTS rewards;
DROP TABLE IF EXISTS reject;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS projects;
DROP TABLE IF EXISTS users;


-- ---------------------------------------------------------------------
-- 1. users : 사용자 (메이커, 서포터, 관리자)
--    메이커와 관리자 계정은 seed.sql로만 (회원가입은 서포터만 가능)
--    password에는 비밀번호 원문이 아니라 해시값을 저장
-- ---------------------------------------------------------------------
CREATE TABLE users (
    user_id     INT           NOT NULL AUTO_INCREMENT,
    login_id    VARCHAR(30)   NOT NULL,                         -- 로그인 아이디
    password    CHAR(64)      NOT NULL,                         -- 비밀번호 해시
    salt        CHAR(32)      NOT NULL,                         -- 해시용 salt (Java가 생성)
    name        VARCHAR(30)   NOT NULL,
    address     VARCHAR(200)  NOT NULL,                         -- 메이커 펀딩 현황의 배송지로 사용
    usertype    ENUM('ADMIN', 'MAKER', 'SUPPORTER') NOT NULL,   -- Java enum UserType과 이름 일치
    regdate     DATETIME      NOT NULL,                         -- 가입일

    PRIMARY KEY (user_id),
    CONSTRAINT uk_users_login_id UNIQUE (login_id)              -- 같은 아이디로 중복가입 방지
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4; -- DB 엔진 및 문자 저장 방식 고정



-- 2. projects : 펀딩 프로젝트
CREATE TABLE projects (
    project_id       INT           NOT NULL AUTO_INCREMENT,     -- 값이 클수록 최근 등록
    user_id          INT           NOT NULL,                    -- 작성한 메이커
    title            VARCHAR(100)  NOT NULL,                    -- 프로젝트명 (검색 대상)
    description      TEXT          NOT NULL,
    goal             INT           NOT NULL,                    -- 목표 금액
    duration         INT           NOT NULL,                    -- 펀딩 기간 (일)
    start_date       DATETIME      NULL,                        -- 승인 시각 = 펀딩 시작 시각
    end_date         DATETIME      GENERATED ALWAYS AS (start_date + INTERVAL duration DAY) STORED,
    approval_status  ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL,  -- 신청, 승인, 반려

    PRIMARY KEY (project_id),
    CONSTRAINT fk_projects_users FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


-- ---------------------------------------------------------------------
-- 3. category : 프로젝트별 카테고리 (프로젝트 하나에 1~2개)
--    프로젝트당 2개라는 규칙은 DB로 표현할 수 없으므로 Service에서 검사
--    enum 수정할 때 TYPE 파일이랑 맞춰야 됨 (대소문자까지)
-- ---------------------------------------------------------------------
CREATE TABLE category (
    category_id    INT  NOT NULL AUTO_INCREMENT,
    project_id     INT  NOT NULL,
    category_name  ENUM(
        'TECH_HOME_APPLIANCES', 'HOME_LIVING', 'BEAUTY', 'FASHION', 'FOOD',
        'BOOK', 'KIDS', 'SPORTS', 'GOODS', 'TRAVEL', 'FANDOM',
        'PET', 'DESIGN', 'ART', 'CAR', 'GAME', 'MOVIE', 'MUSIC',
        'PHOTO', 'WEBTOON', 'MEMBERSHIP', 'SOCIAL'
    ) NOT NULL,

    PRIMARY KEY (category_id),
    CONSTRAINT uk_category_project_name UNIQUE (project_id, category_name), -- 같은 프로젝트에 같은 카테고리를 두 번 넣기 X
    CONSTRAINT fk_category_projects FOREIGN KEY (project_id) REFERENCES projects (project_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


-- ---------------------------------------------------------------------
-- 4. rewards : 리워드
--    stock(잔여 수량)이 NULL이면 무제한을 의미
--    후원 금액은 따로 저장하지 않고 price x 수량으로 계산: 때문에 승인된 프로젝트의 리워드는 가격을 바꾸면 안 됨
-- ---------------------------------------------------------------------
CREATE TABLE rewards (
    reward_id    INT          NOT NULL AUTO_INCREMENT,
    project_id   INT          NOT NULL,
    name         VARCHAR(50)  NOT NULL,
    price        INT          NOT NULL,                         -- 1개당 후원 금액
    description  TEXT         NOT NULL,                         -- 제공 내용
    stock        INT          NULL,                             -- 잔여 수량 (NULL = 무제한)

    PRIMARY KEY (reward_id),

    CONSTRAINT ck_rewards_stock CHECK (stock >= 0), -- 재고 음수 X
    CONSTRAINT fk_rewards_projects FOREIGN KEY (project_id) REFERENCES projects (project_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


-- ---------------------------------------------------------------------
-- 5. image : 리워드 이미지 (리워드당 0장 또는 1장)
--    image_data를 SELECT하는 곳은 RewardImageDAO의 이미지 조회 메서드뿐이어야 함
--    목록이나 상세 조회에서 이미지까지 읽으면 매번 큰 데이터가 오가게 되므로 주의
-- ---------------------------------------------------------------------
CREATE TABLE image (
    image_id    INT           NOT NULL AUTO_INCREMENT,
    reward_id   INT           NOT NULL,
    name        VARCHAR(255)  NOT NULL,                         -- 원본 파일명 (경로 부분 제거 후 저장)
    type        VARCHAR(50)   NOT NULL,                         -- MIME 타입 (서버가 파일 내용으로 판별)
    image_data  MEDIUMBLOB    NOT NULL,

    PRIMARY KEY (image_id),
    CONSTRAINT uk_image_reward UNIQUE (reward_id),              -- 리워드 하나에 이미지는 최대 1장
    CONSTRAINT fk_image_rewards FOREIGN KEY (reward_id) REFERENCES rewards (reward_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


-- ---------------------------------------------------------------------
-- 6. reject : 관리자의 프로젝트 거절 사유
--    반려와 재신청이 반복될 수 있으므로 이력으로 쌓임
-- ---------------------------------------------------------------------
CREATE TABLE reject (
    rejectreason_id  INT       NOT NULL AUTO_INCREMENT,
    project_id       INT       NOT NULL,
    reason           TEXT      NOT NULL,
    created_at       DATETIME  NOT NULL,

    PRIMARY KEY (rejectreason_id),
    CONSTRAINT fk_reject_projects FOREIGN KEY (project_id) REFERENCES projects (project_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


-- ---------------------------------------------------------------------
-- 7. likes : 좋아요
--    좋아요 취소는 행 삭제로 구현
-- ---------------------------------------------------------------------
CREATE TABLE likes (
    user_id     INT  NOT NULL,
    project_id  INT  NOT NULL,

    PRIMARY KEY (user_id, project_id),   -- 한 사용자는 한 프로젝트에 좋아요 1번
    CONSTRAINT fk_likes_users    FOREIGN KEY (user_id)    REFERENCES users (user_id),
    CONSTRAINT fk_likes_projects FOREIGN KEY (project_id) REFERENCES projects (project_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


-- ---------------------------------------------------------------------
-- 8. funds : 유효한 후원
--    금액은 rewards.price x quantity로 계산
--    사용자가 직접 취소하면 이 테이블에서 지우고 canceled_funds로 옮김
--    펀딩에 실패한 프로젝트의 후원도 여기에 그대로 남는다 (실패 여부는 조회할 때 계산)
--
--    [데드락 방지] 후원 신청 트랜잭션은 반드시
--      1) rewards 재고 차감 UPDATE  2) funds INSERT  순서로 실행
--      funds를 먼저 INSERT하면 FK 검사 때문에 rewards 행에 잠금이 걸려 데드락 발생
-- ---------------------------------------------------------------------
CREATE TABLE funds (
    fund_id     INT       NOT NULL AUTO_INCREMENT,
    user_id     INT       NOT NULL,                             -- 후원한 서포터
    project_id  INT       NOT NULL,
    reward_id   INT       NOT NULL,                             -- 선택한 리워드
    quantity    INT       NOT NULL,                             -- 구매 수량 (인당 최대값은 Java 상수로 검사)
    date        DATETIME  NOT NULL,   -- 후원 시각 = 신청일

    PRIMARY KEY (fund_id),
    CONSTRAINT uk_funds_user_project UNIQUE (user_id, project_id), -- 한 서포터는 한 프로젝트에 한번만 후원
    CONSTRAINT ck_funds_quantity CHECK (quantity >= 1), -- 최소 1개 이상 구매
    CONSTRAINT fk_funds_users    FOREIGN KEY (user_id)    REFERENCES users (user_id),
    CONSTRAINT fk_funds_projects FOREIGN KEY (project_id) REFERENCES projects (project_id),
    CONSTRAINT fk_funds_rewards  FOREIGN KEY (reward_id)  REFERENCES rewards (reward_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


-- ---------------------------------------------------------------------
-- 9. canceled_funds : 사용자가 직접 취소한 후원 (MyPage에서 사유와 함께 보여줌)
--    취소는 진행 중인 프로젝트에서만 가능
--
--    [데드락 방지] 후원 취소 트랜잭션은 반드시
--      1) rewards 재고 복구 UPDATE  2) canceled_funds INSERT  3) funds DELETE  순서로 실행
-- ---------------------------------------------------------------------
CREATE TABLE canceled_funds (
    canceled_funds_id  INT       NOT NULL AUTO_INCREMENT,
    user_id            INT       NOT NULL,
    project_id         INT       NOT NULL,
    reward_id          INT       NOT NULL,
    quantity           INT       NOT NULL,                      -- funds의 수량을 그대로 복사
    content            TEXT      NOT NULL,                      -- 취소 사유
    cancel_date        DATETIME  NOT NULL,  -- 취소 시각

    PRIMARY KEY (canceled_funds_id),
    CONSTRAINT fk_canceled_funds_users    FOREIGN KEY (user_id)    REFERENCES users (user_id),
    CONSTRAINT fk_canceled_funds_projects FOREIGN KEY (project_id) REFERENCES projects (project_id),
    CONSTRAINT fk_canceled_funds_rewards  FOREIGN KEY (reward_id)  REFERENCES rewards (reward_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


-- ---------------------------------------------------------------------
-- 10. reviews : 리뷰와 별점
--     작성 조건(본인 후원, 프로젝트 마감, 펀딩 성공)은 Service에서 검사
-- ---------------------------------------------------------------------
CREATE TABLE reviews (
                         fund_id  INT       NOT NULL,
                         star     TINYINT   NOT NULL,                                -- 별점 1~5
                         content  TEXT      NULL,                                    -- 리뷰 내용 (선택)
                         date     DATETIME  NOT NULL,      -- 작성 시각

                         PRIMARY KEY (fund_id), -- 후원 하나에 리뷰는 하나만
                         CONSTRAINT ck_reviews_star CHECK (star BETWEEN 1 AND 5),
                         CONSTRAINT fk_reviews_funds FOREIGN KEY (fund_id) REFERENCES funds (fund_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;