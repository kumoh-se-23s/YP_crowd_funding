-- 1번
-- maker1 프로젝트별 총 펀딩 금액 / 달성률 / 상태 + 리워드별 총 수량 / 신청 수량 / 잔여 수량 (stock 0 = 무제한)
SELECT p.project_id, p.title, p.approval_status, p.goal, t.total, ROUND(t.total / p.goal * 100, 1) AS progress, CASE WHEN p.approval_status <> 'APPROVED' THEN p.approval_status WHEN p.end_date > NOW() THEN 'ONGOING' WHEN t.total >= p.goal THEN 'SUCCESS' ELSE 'FAILED' END AS status, r.name AS reward_name, IF(r.stock = 0, '무제한', r.stock) AS stock, (SELECT COALESCE(SUM(f.quantity), 0) FROM funds f WHERE f.reward_id = r.reward_id) AS applied, IF(r.stock = 0, '무제한', r.stock - (SELECT COALESCE(SUM(f.quantity), 0) FROM funds f WHERE f.reward_id = r.reward_id)) AS remaining FROM projects p JOIN users mu ON mu.user_id = p.user_id JOIN (SELECT p2.project_id, COALESCE(SUM(r2.price * f2.quantity), 0) AS total FROM projects p2 LEFT JOIN funds f2 ON f2.project_id = p2.project_id LEFT JOIN rewards r2 ON r2.reward_id = f2.reward_id GROUP BY p2.project_id) t ON t.project_id = p.project_id LEFT JOIN rewards r ON r.project_id = p.project_id WHERE mu.login_id = 'maker1' ORDER BY p.project_id, r.reward_id;
-- maker1 프로젝트의 서포터 및 배송지
SELECT p.project_id, p.title, su.name AS supporter, r.name AS reward_name, f.quantity, r.price * f.quantity AS amount, su.address FROM funds f JOIN projects p ON p.project_id = f.project_id JOIN users mu ON mu.user_id = p.user_id JOIN users su ON su.user_id = f.user_id JOIN rewards r ON r.reward_id = f.reward_id WHERE mu.login_id = 'maker1' ORDER BY p.project_id, f.date DESC, f.fund_id DESC;


-- 2번
-- 등록된 프로젝트 정보
SELECT project_id, user_id, title, description, goal, duration, start_date, end_date, approval_status FROM projects ORDER BY project_id DESC;
-- 등록된 프로젝트 카테고리
SELECT c.category_id, c.project_id, p.title, c.category_name FROM category c JOIN projects p ON c.project_id = p.project_id ORDER BY c.project_id DESC, c.category_id ASC;

-- 3번
-- 5번 프로젝트에 등록된 리워드 조회, 연계 시연시 pk에 맞게 수정
SELECT r.reward_id, r.project_id, p.title, p.approval_status, r.name, r.price, r.description, r.stock FROM rewards r JOIN projects p ON r.project_id = p.project_id WHERE r.project_id = 5 ORDER BY r.reward_id ASC;


-- 4번
-- 승인 대상(6번) 프로젝트 상태, 실행 전후로 함께 실행 (대상 바꾸면 pk 수정)
SELECT p.project_id, p.title, u.login_id AS maker, p.approval_status, p.duration, p.start_date, p.end_date FROM projects p JOIN users u ON u.user_id = p.user_id WHERE p.project_id = 6;

-- 5번
-- 정보 변경 대상 유저 정보 확인
SELECT user_id, login_id, name, address, password, salt FROM users WHERE login_id = 'profileSupporter';

-- 6번
-- 승인된 프로젝트 전체 (진행 중 + 마감, 최신순) 기한 / 달성률 / 상태
SELECT p.project_id, p.title, p.start_date, p.end_date, ROUND(t.total / p.goal * 100, 1) AS progress, CASE WHEN p.end_date > NOW() THEN 'ONGOING' WHEN t.total >= p.goal THEN 'SUCCESS' ELSE 'FAILED' END AS status FROM projects p JOIN (SELECT p2.project_id, COALESCE(SUM(r2.price * f2.quantity), 0) AS total FROM projects p2 LEFT JOIN funds f2 ON f2.project_id = p2.project_id LEFT JOIN rewards r2 ON r2.reward_id = f2.reward_id GROUP BY p2.project_id) t ON t.project_id = p.project_id WHERE p.approval_status = 'APPROVED' ORDER BY p.start_date DESC;


-- 7번
-- 필터 대상 카테고리의 승인된 프로젝트 전체와 상태 (진행 중 / 마감 필터 결과를 이 표와 비교), 카테고리 바꾸면 IN 안도 수정
SELECT c.category_name, p.project_id, p.title, p.end_date, ROUND(t.total / p.goal * 100, 1) AS progress, CASE WHEN p.end_date > NOW() THEN 'ONGOING' WHEN t.total >= p.goal THEN 'SUCCESS' ELSE 'FAILED' END AS status FROM projects p JOIN category c ON c.project_id = p.project_id JOIN (SELECT p2.project_id, COALESCE(SUM(r2.price * f2.quantity), 0) AS total FROM projects p2 LEFT JOIN funds f2 ON f2.project_id = p2.project_id LEFT JOIN rewards r2 ON r2.reward_id = f2.reward_id GROUP BY p2.project_id) t ON t.project_id = p.project_id WHERE p.approval_status = 'APPROVED' AND c.category_name IN ('TECH_HOME_APPLIANCES', 'SOCIAL') ORDER BY c.category_name, p.project_id;

-- 8번
-- 이하 실행 전 메소드 구조 통해 성공 실패 시도 및 결과 보여주기
-- 아래 셋 모두 전후로 함께 실행
-- 1번 리워드 수량 확인
SELECT r.reward_id, r.name, r.stock, r.stock - (SELECT COALESCE(SUM(f.quantity), 0) FROM funds f WHERE f.reward_id = r.reward_id) AS remaining FROM rewards r WHERE r.reward_id = 1;
-- supporter1의 펀딩 기록
SELECT f.fund_id, u.login_id, f.project_id, f.reward_id, f.quantity, f.date FROM funds f JOIN users u ON u.user_id = f.user_id WHERE u.login_id = 'supporter1' ORDER BY f.fund_id DESC;
-- 메이커 입장에서 자기 프로젝트 기록 조회
SELECT p.project_id, p.title, su.login_id AS supporter, su.name, r.name AS reward_name, f.quantity, r.price * f.quantity AS amount, f.date FROM funds f JOIN projects p ON p.project_id = f.project_id JOIN users mu ON mu.user_id = p.user_id JOIN users su ON su.user_id = f.user_id JOIN rewards r ON r.reward_id = f.reward_id WHERE mu.login_id = 'maker1' ORDER BY f.date DESC;

-- 9번
-- supporter1이 후원한 프로젝트들의 현재 상태 (실행 전 1회만 확인해도 무방)
SELECT f.fund_id, p.title, p.goal, (SELECT COALESCE(SUM(r2.price * f2.quantity), 0) FROM funds f2 JOIN rewards r2 ON r2.reward_id = f2.reward_id WHERE f2.project_id = p.project_id) AS total, CASE WHEN p.end_date > NOW() THEN 'ONGOING' WHEN (SELECT COALESCE(SUM(r2.price * f2.quantity), 0) FROM funds f2 JOIN rewards r2 ON r2.reward_id = f2.reward_id WHERE f2.project_id = p.project_id) >= p.goal THEN 'SUCCESS' ELSE 'FAILED' END AS status FROM funds f JOIN users u ON u.user_id = f.user_id JOIN projects p ON p.project_id = f.project_id WHERE u.login_id = 'supporter1';
-- 리뷰 목록 조회
SELECT u.login_id, p.title, r.name AS reward_name, rv.star, rv.content, rv.date FROM reviews rv JOIN funds f ON f.fund_id = rv.fund_id JOIN users u ON u.user_id = f.user_id JOIN projects p ON p.project_id = f.project_id JOIN rewards r ON r.reward_id = f.reward_id ORDER BY rv.date DESC;

-- 10번
-- 5번 리워드의 잔여 수량 조회
SELECT r.reward_id, r.name, r.stock, r.stock - (SELECT COALESCE(SUM(f.quantity), 0) FROM funds f WHERE f.reward_id = r.reward_id) AS remaining FROM rewards r WHERE r.reward_id = 5;
-- 5번 리워드 구매자 조회
SELECT f.fund_id, u.login_id, f.reward_id, f.quantity, f.date FROM funds f JOIN users u ON u.user_id = f.user_id WHERE f.reward_id = 5 ORDER BY f.date;