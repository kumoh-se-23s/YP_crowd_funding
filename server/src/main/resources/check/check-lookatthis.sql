-- 1번
--

-- 2번
-- 등록된 프로젝트 정보
SELECT project_id, user_id, title, description, goal, duration, start_date, end_date, approval_status FROM projects ORDER BY project_id DESC;
-- 등록된 프로젝트 카테고리
SELECT c.category_id, c.project_id, p.title, c.category_name FROM category c JOIN projects p ON c.project_id = p.project_id ORDER BY c.project_id DESC, c.category_id ASC;

-- 3번
-- 5번 프로젝트에 등록된 리워드 조회, 연계 시연시 pk에 맞게 수정
SELECT r.reward_id, r.project_id, p.title, p.approval_status, r.name, r.price, r.description, r.stock FROM rewards r JOIN projects p ON r.project_id = p.project_id WHERE r.project_id = 5 ORDER BY r.reward_id ASC;


-- 4번
--

-- 5번
-- 정보 변경 대상 유저 정보 확인
SELECT user_id, login_id, name, address, password, salt FROM users WHERE login_id = 'profileSupporter';

-- 6번
--

-- 7번
--

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