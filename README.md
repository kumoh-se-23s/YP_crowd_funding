# YP 크라우드펀딩

TCP/IP + MySQL 기반 크라우드펀딩 시스템 (융합프로젝트 텀프로젝트)

| 모듈 | 내용 |
|---|---|
| `common` | 서버·클라이언트 공용: enum, 상수(DomainRules), DTO, 순수 함수(Date). **DB 라이브러리 사용 금지** |
| `server` | 서버: 설정, 커넥션 풀, 트랜잭션, DAO, Service |
| `client` | (2차) 콘솔 클라이언트 |

---

## 1. 처음 세팅 (각자 PC에서 한 번)

준비물: JDK 25, MySQL 8, IntelliJ (프로젝트를 열면 Gradle로 자동 인식. **New Module 마법사는 쓰지 않는다**)

1. **테이블**: `sql/schema.sql` 맨 위 주석의 실행법대로 스키마를 만들고 실행한다
2. **설정 파일**: 아래 두 `.example` 파일의 주석대로 복사해서 작성한다
    - `server/src/main/resources/config/application.properties.example`
    - `server/src/test/resources/config/application-test.properties.example`
3. **확인**: MySQL을 켠 상태에서 `.\gradlew build` → 모든 테스트가 PASSED면 완료

> mysql 콘솔(명령줄)로 SQL 파일을 실행할 때는 `--default-character-set=utf8mb4`를 꼭 붙인다 (안 붙이면 한글 깨짐, 에러 1406)

---

## 2. 폴더 구조

```
sql/schema.sql                         테이블 정의 (구조 변경은 Workbench가 아니라 이 파일을 고쳐서 커밋)
common/src/main/java/ypfunding/common/
  ├─ type/        UserType, ApprovalStatus, Category, FundingResult, SupporterSortType, AdminSortType
  ├─ constant/    DomainRules (서버·클라 공용 규칙 상수)
  ├─ util/        DateTimeUtil (KST, 날짜 표시 형식)
  ├─ dto/         ~DTO (데이터 전달 객체)
  └─ protocol/    (2차) Command, Request, Response
server/src/main/java/ypfunding/server/
  ├─ bootstrap/   AppContext(조립), DataSourceFactory(커넥션 풀), MyBatisConfig
  ├─ config/      AppProperties (설정 파일 읽기)
  ├─ auth/        LoginUser
  ├─ exception/   BusinessException, NotAllowedException, DataAccessException
  ├─ persistence/ TransactionManager, SqlWork, SqlVoidWork
  │   ├─ dao/     ~DAO (테이블별 순수 JDBC)
  │   └─ mapper/  동적 쿼리 매퍼
  ├─ service/     ~Service
  ├─ constant/    서버 전용 상수
  ├─ util/        서버 전용 순수 함수
  ├─ network/     (2차) 접속 처리, 요청 분배
  └─ controller/  (2차) 요청 → Service → 응답
server/src/test/java/ypfunding/server/
  └─ support/     DbTestBase, TestData, TestClock, Concurrent, TestSchema (테스트 도구)
```

패키지는 모두 `ypfunding.`으로 시작한다. 모듈 X의 코드는 `ypfunding.X.*` 아래에만 둔다.
각 패키지의 역할과 참조 규칙은 그 패키지의 `package-info.java`에 적혀 있다.

---

## 3. 꼭 지킬 규칙

**커넥션·트랜잭션**
- 커넥션은 `TransactionManager`만 빌린다. Service는 `tx.execute(conn -> ...)` 안에서 받은 `conn`을 DAO에 넘긴다
- DAO는 커넥션을 **얻지도, 닫지도 않는다**. `PreparedStatement`, `ResultSet`만 try-with-resources로 닫는다
- 단순 조회도 `tx.execute()`로 실행한다
- `tx.execute()` 안에서 `tx.execute()`를 다시 부르지 않는다 (중첩 금지. 부르면 예외)
- Service가 다른 Service를 호출하지 않는다. 같은 데이터가 필요하면 같은 DAO 메서드를 각자 호출한다
- 비밀번호 해싱처럼 DB와 무관한 작업은 트랜잭션 밖에서 한다

**SQL**
- 모든 SQL은 `PreparedStatement` + `?`. 문자열 연결로 값을 넣지 않는다
- SQL에 `NOW()`를 쓰지 않는다. 현재 시각은 Service가 `Clock`에서 얻어 파라미터로 넘긴다
- 날짜 컬럼에는 기본값이 없다. INSERT 때 날짜를 빠뜨리면 에러 1364가 난다
- `end_date`는 생성 컬럼이라 INSERT/UPDATE에 넣지 않는다 (에러 3105)
- 날짜는 `ps.setObject(i, localDateTime)` / `rs.getObject("col", LocalDateTime.class)`. `getTimestamp()` 금지
- NULL이 가능한 컬럼은 래퍼 타입(`Integer`, `Long`)으로 받고 `rs.getObject("col", Integer.class)`로 읽는다.
  **`rs.getInt()`는 NULL을 0으로 읽어서 무제한(NULL) 리워드가 품절(0)로 보인다**

**동시성 (데드락 방지)**
- 한 트랜잭션 안에서 **부모 행 UPDATE를 먼저, 자식 행 INSERT/DELETE를 나중에**
    - 후원 신청: ① rewards 재고 차감 UPDATE → ② funds INSERT
    - 후원 취소: ① rewards 재고 복구 UPDATE → ② canceled_funds INSERT → ③ funds DELETE
- 재고는 SELECT 후 Java에서 비교하지 않는다. 조건부 UPDATE 한 문장 + 영향 행 수로 판단한다
- 중복(1인 1후원, 아이디 중복)은 DB의 UNIQUE가 막는다. 에러 1062를 DAO가 업무 예외로 바꾼다

**객체**
- Service·DAO는 상태가 없는 객체다. 필드에 요청별 값을 저장하지 않는다 (모든 클라이언트 스레드가 공유)
- 서버 수명 동안 하나인 것(TransactionManager, DAO, Clock)은 생성자로, 요청마다 다른 것(Connection, LoginUser, 요청 값)은 메서드 인자로 받는다
- Service·DAO의 `new`는 `AppContext`에서만 한다 (테스트 제외)
- Spring을 쓰지 않는다 (`@Autowired`, `@Transactional`, `@Service` 등 없음)

---

## 4. 새 기능을 만드는 순서

1. DTO (`common/dto`) → 2. DAO → 3. Service → 4. `AppContext`에 등록 → 5. 테스트 (`DbTestBase` 상속)

아래는 기능과 상관없이 변하지 않는 틀이다. `Xxx`를 기능 이름으로 바꿔서 쓴다.

### 4.1 DTO

```java
@Getter
@Setter
public class XxxDTO implements Serializable {   // 클라이언트로 전송되므로 Serializable
    private Long xxxId;
    private Integer nullableValue;              // NULL 가능 컬럼은 래퍼 타입 (int 금지)
    private LocalDateTime date;
}
```

### 4.2 DAO

```java
/** xxx 테이블 접근. 커넥션은 Service가 넘겨주며, DAO는 커넥션을 얻거나 닫지 않는다. */
public class XxxDAO {

    // 모든 메서드: 첫 인자 Connection, throws SQLException
    public Optional<XxxDTO> findById(Connection conn, long xxxId) throws SQLException {
        String sql = "SELECT ... FROM xxx WHERE xxx_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {   // 닫는 것은 PreparedStatement, ResultSet뿐
            ps.setLong(1, xxxId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(toDTO(rs)) : Optional.empty();
            }
        }
    }

    // UPDATE / DELETE: 바뀐 행 수를 돌려준다. 0행의 의미(재고 부족, 상태 불일치 등)는 Service가 판단
    public int updateXxx(Connection conn, ...) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ...
            return ps.executeUpdate();
        }
    }

    // 의미 있는 에러 코드를 업무 예외로 바꿀 때만 catch 한다
    public long insert(Connection conn, ...) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ...
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                throw new DuplicateXxxException(e);   // 바꿀 때는 e를 담는다
            }
            throw e;                                  // 나머지는 그대로 던진다 (감싸지 않는다)
        }
    }

    // ResultSet 한 행 → DTO
    private XxxDTO toDTO(ResultSet rs) throws SQLException {
        XxxDTO dto = new XxxDTO();
        dto.setXxxId(rs.getLong("xxx_id"));
        dto.setNullableValue(rs.getObject("nullable_value", Integer.class));   // NULL 가능: getInt 금지
        dto.setDate(rs.getObject("date", LocalDateTime.class));                // 날짜: getTimestamp 금지
        return dto;
    }
}
```

### 4.3 예외

```java
/** 사용자에게 보여줄 규칙 위반. BusinessException을 상속한다. */
public class XxxException extends BusinessException {
    public XxxException() {
        super("사용자에게 보여줄 문장");           // 코드가 위반을 직접 발견했을 때
    }

    public XxxException(Throwable cause) {
        super("사용자에게 보여줄 문장", cause);    // 다른 예외를 잡아서 바꿀 때
    }
}
```

**e(cause)를 담는 기준**

| 상황 | 던지는 법 | e |
|---|---|---|
| 코드가 위반을 직접 발견 (0행 변경, 범위 밖 입력, 권한 없음) | `throw new XxxException()` | 없음 |
| 다른 예외를 잡아서 업무 예외로 바꿈 (1062 등) | `throw new XxxException(e)` | **반드시 담는다** (원인이 로그에 남도록) |
| 그 밖의 SQLException | 잡지 않거나 `throw e` | TransactionManager가 e를 담아 `DataAccessException`으로 감싼다 |
| 잡고 버리기 (`catch (...) {}`, `printStackTrace()`만) | **금지** | |

- 메시지는 사용자 화면에 그대로 나가므로 읽을 수 있는 문장으로 쓴다. SQL, 테이블명, 내부 번호는 넣지 않는다
- 권한 위반은 `NotAllowedException`, DB 오류용 `DataAccessException`은 직접 만들지 않는다

### 4.4 Service

```java
public class XxxService {

    // 서버 수명 동안 하나인 것만 필드로 (AppContext가 생성자로 넣어 준다)
    private final TransactionManager tx;
    private final XxxDAO xxxDAO;
    private final Clock clock;

    public XxxService(TransactionManager tx, XxxDAO xxxDAO, Clock clock) {
        this.tx = tx;
        this.xxxDAO = xxxDAO;
        this.clock = clock;
    }

    public Result doXxx(LoginUser user, ...) {         // 사용자를 대신해 동작하면 첫 인자는 LoginUser
        // 1) 권한 검사: user.userType()으로. 위반 → NotAllowedException
        // 2) 입력 검사: DB 없이 가능한 것(범위·길이 → DomainRules). 위반 → BusinessException
        // 3) 현재 시각: 여기서 한 번만 얻어 끝까지 쓴다
        LocalDateTime now = LocalDateTime.now(clock);

        // 4) DB 작업: 트랜잭션 하나
        return tx.execute(conn -> {
            // DAO에 conn을 넘겨 호출한다
            // 규칙 위반을 발견하면 throw → 이 람다 안에서 실행한 SQL이 전부 롤백된다
            // 같은 행을 다루면 부모 UPDATE 먼저, 자식 INSERT/DELETE 나중
            return result;
        });
    }
}
```

**람다 사용법**
- 결과가 있으면 `T result = tx.execute(conn -> { ...; return 값; });`
- 결과가 없으면 `tx.executeWithoutResult(conn -> { ... });`
- 람다 안에서: DAO 호출 O, `throw` O / `tx.execute` 다시 호출 X, `conn` 닫기·commit X, `conn`을 밖에 저장 X
- 오래 걸리는 작업(비밀번호 해싱 등)은 람다에 넣지 않고 1)~3) 단계에서 한다 (그동안 커넥션과 잠금을 쥐고 있게 된다)
- 람다 안에서 쓰는 바깥 지역 변수(`now`, `user` 등)는 값을 다시 대입하면 컴파일 에러가 난다 (effectively final)

### 4.5 AppContext에 등록

`server/.../bootstrap/AppContext.java`에 아래 순서로 추가한다.

```java
// 1) 필드: DAO에는 getter를 붙이지 않는다 (Service 안에서만 사용). Service에는 @Getter
private final XxxDAO xxxDAO;
@Getter
private final XxxService xxxService;

// 2) 생성자의 try 블록 안: DAO 먼저, Service 나중
this.xxxDAO = new XxxDAO();
this.xxxService = new XxxService(transactionManager, xxxDAO, clock);
```

### 4.6 MyBatis 동적 쿼리 (조건·정렬에 따라 SQL이 바뀌는 목록 조회 전용)

- 고정 SQL은 일반 DAO(순수 JDBC). 매퍼는 XML이 아니라 `@SelectProvider` + SQL 빌더
- 매퍼를 만들면 `MyBatisConfig`의 `config.addMapper(...)` 주석을 풀어 등록한다
- 정렬 컬럼은 enum(`SupporterSortType`, `AdminSortType`)의 `switch`로만 정하고, 마지막에 보조 정렬 키(`p.project_id DESC`)를 붙인다

```java
public class ProjectQueryDAO {
    private final SqlSessionFactory factory;              // AppContext가 생성자로 넣어 준다

    public List<XxxDTO> search(Connection conn, XxxCondition cond) {
        try (SqlSession session = factory.openSession(conn)) {   // 반드시 conn을 넘긴다
            return session.getMapper(ProjectQueryMapper.class).search(cond);
        }                                                         // 세션만 닫히고 커넥션은 그대로
    }
}
```
`openSession()`을 인자 없이 부르면 TransactionManager 밖에서 커넥션을 빌리고 반납도 안 된다 (누수).

---

## 5. 테스트 작성법

DB를 쓰는 테스트는 `DbTestBase`를 상속한다. 아래를 자동으로 해 준다.
- 테스트 DB(`crowdfunding_test`)에 연결, 테이블이 없으면 `schema.sql`로 생성
- **매 테스트 전에 모든 테이블을 비운다** (테스트끼리 데이터가 섞이지 않음)
- 매 테스트 후 커넥션 누수 검사 (반납 안 된 커넥션이 있으면 실패)
- 접속한 DB 이름이 `_test`로 끝나지 않으면 아무것도 지우지 않고 멈춘다 (개발 DB 보호)

| 도구 | 쓰는 법 |
|---|---|
| `context` | 테스트용 AppContext. `context.getXxxService()` |
| `tx` | 데이터 준비·확인용. `tx.execute(conn -> ...)` |
| `clock` | 테스트 시계. 시작은 항상 `BASE_TIME`(2026-12-07 10:00). `clock.set(...)`, `clock.plus(Duration.ofDays(31))`, `clock.now()` |
| `TestData` | 준비 데이터를 SQL로 직접 넣는다. `insertUser`, `insertApprovedProject`, `insertPendingProject`, `insertReward`. 필요한 것은 각자 추가 |
| `Concurrent` | 동시성 테스트. `runAtOnce(스레드 수, i -> 작업)`, `successCount(결과)`, `countOf(결과, 예외.class)` |

```java
class XxxServiceTest extends DbTestBase {

    @Test
    @DisplayName("무엇을 하면 어떻게 된다")
    void xxx() {
        // 준비: TestData로 필요한 데이터만 넣는다 (돌려받은 id를 쓴다)
        long userId = tx.execute(conn -> TestData.insertUser(conn, "user1", UserType.SUPPORTER, clock.now()));
        LoginUser user = new LoginUser(userId, UserType.SUPPORTER);   // 테스트에서는 직접 만들어도 된다

        // 실행: context에서 Service를 꺼내 호출한다
        context.getXxxService().doXxx(user, ...);

        // 검증: 돌려받은 값, 또는 tx.execute로 DB를 직접 조회해서 확인한다
    }
}
```

동시성 테스트는 실행과 검증만 바뀐다.
```java
List<Throwable> results = Concurrent.runAtOnce(20, i -> context.getXxxService().doXxx(users.get(i), ...));
assertEquals(1, Concurrent.successCount(results));                  // 성공한 스레드 수
assertEquals(19, Concurrent.countOf(results, XxxException.class));  // 특정 예외로 실패한 스레드 수
```

- id를 1, 2 같은 숫자로 가정하지 않는다. `TestData`가 돌려준 id를 쓴다
- 테스트 시계는 초 단위 값만 쓴다. DATETIME은 초까지만 저장하고 밀리초는 반올림된다
- JUnit 병렬 실행은 켜지 않는다 (모든 테스트가 같은 테이블을 쓴다)

---

## 6. 자주 만나는 에러

| 메시지 / 코드 | 원인 | 해결 |
|---|---|---|
| `gradlew` 인식 안 됨 (PowerShell) | 현재 폴더 실행 파일은 `.\`가 필요 | `.\gradlew build` |
| `설정 파일을 찾을 수 없음` | properties 파일이 없음 | 1장 2번: `.example` 주석대로 작성 |
| `db 접속 불가` | MySQL 꺼짐 / 스키마 없음 / 계정·비밀번호 틀림 | 예외의 cause(Caused by)에 정확한 원인이 있다 |
| `테스트가 테스트용이 아닌 DB에 연결되었습니다` | `application-test.properties`의 url이 `crowdfunding`을 가리킴 | url을 `crowdfunding_test`로 |
| `중첩 트랜잭션 감지` | `tx.execute` 안에서 `tx.execute` 호출 (Service가 다른 Service 호출 포함) | 같은 람다 안에서 DAO를 직접 호출 |
| `반납되지 않은 커넥션이 있습니다` | 커넥션을 직접 빌렸거나 람다 밖으로 꺼냄 | 커넥션은 `tx.execute`가 준 것만 사용 |
| 1062 Duplicate entry | UNIQUE 위반 (중복 후원, 아이디 중복) | DAO에서 업무 예외로 변환 |
| 1364 Field doesn't have a default value | INSERT에 날짜 등 필수 값 누락 | `ps.setObject(i, now)`로 명시 |
| 3105 generated column | `end_date`를 INSERT/UPDATE에 넣음 | `end_date` 빼기 |
| 3819 Check constraint | 재고 음수, 별점 범위 밖, 수량 0 이하 | 입력값 검사 |
| 1451 foreign key constraint fails | 자식 행이 있는 부모 행 삭제 | 자식 → 부모 순으로 삭제 |
| 1406 Data too long / 한글 깨짐 (콘솔 실행 시) | mysql 콘솔 문자셋이 utf8mb4가 아님 | `--default-character-set=utf8mb4` |