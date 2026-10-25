package ypfunding.server.persistence.dao;

import ypfunding.common.dto.ReviewDTO;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLDelete;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLInsert;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLSelect;
import ypfunding.server.persistence.dao.dynamicsql.condition.BasicCondition;
import ypfunding.server.persistence.dao.dynamicsql.op.ArithmeticConditionOperator;
import ypfunding.server.persistence.dao.dynamicsql.table.DirectReferenceTable;
import ypfunding.server.persistence.dao.dynamicsql.table.JoinType;
import ypfunding.server.persistence.dao.dynamicsql.table.JoinedTable;
import ypfunding.server.persistence.dao.dynamicsql.token.ColumnValueToken;
import ypfunding.server.persistence.dao.dynamicsql.token.TableRefColumnToken;
import ypfunding.server.persistence.dao.dynamicsql.token.UserValueToken;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReviewsDAO extends DAO {
    public enum Columns implements ColumnsEnum {
        FUND_ID("fund_id"),
        STAR("star"),
        CONTENT("content"),
        DATE("date");

        private final String name;

        Columns(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public void fillDataFromResultSet(ReviewDTO dto, ResultSet rs) {
        //TODO
    }

    // =================================================================
    // 1. CREATE SECTION
    // =================================================================

    public void insert(Connection conn, ReviewDTO reviewDTO) throws SQLException {
        new SQLInsert(Tables.REVIEWS)
                .addTarget(Columns.FUND_ID, reviewDTO.getFundId())
                .addTarget(Columns.STAR, reviewDTO.getStar())
                .addTarget(Columns.CONTENT, reviewDTO.getContent())
                .addTarget(Columns.DATE, reviewDTO.getDate())
                .execute(conn);
    }

    // =================================================================
    // 2. READ SECTION
    // =================================================================

    /**
     * 특정 프로젝트에 작성된 모든 리뷰를 최신순(작성 시각 내림차순)으로 조회한다. (평가표 5번)
     * REVIEWS와 FUNDS를 fund_id로 INNER JOIN하여 해당 project_id의 리뷰만 가져옴
     *
     * @param conn      커넥션
     * @param projectId 조회할 프로젝트 ID
     * @return 최신순으로 정렬된 리뷰 DTO 목록
     * @throws SQLException DB 오류 발생 시
     */
    public List<ReviewDTO> findAll(Connection conn, long projectId) throws SQLException {
        final String FIND_BY_PROJECT_SQL = """
                SELECT r.fund_id, r.star, r.content, r.date
                  FROM reviews r
                  INNER JOIN funds f ON r.fund_id = f.fund_id
                 WHERE f.project_id = ?
                 ORDER BY r.date DESC, r.fund_id DESC
                """;
        List<ReviewDTO> result = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_PROJECT_SQL)) {
            ps.setLong(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(toDTO(rs));
                }
            }
        }
        return result;
    }
    /**
     * 특정 후원(fund_id)에 대한 리뷰가 존재하는지 단건 조회한다.
     *
     * @param conn   커넥션
     * @param fundId 후원 ID
     * @return 리뷰 DTO (없으면 null)
     * @throws SQLException DB 오류 발생 시
     */
    public ReviewDTO findByFundId(Connection conn, long fundId) throws SQLException {
        try (ResultSet rs = SQLSelect.allColumns(new DirectReferenceTable(Tables.REVIEWS))
                .addCondition(new BasicCondition(
                        new ColumnValueToken(Columns.FUND_ID),
                        ArithmeticConditionOperator.EQ,
                        new UserValueToken(fundId)))
                .execute(conn)) {
            if (rs.next()) {
                return toDTO(rs);
            }
            return null;
        }
    }

    private ReviewDTO toDTO(ResultSet rs) throws SQLException {
        ReviewDTO reviewDTO = new ReviewDTO();
        reviewDTO.setFundId(rs.getLong(Columns.FUND_ID.name));
        reviewDTO.setStar(rs.getInt(Columns.STAR.name));
        reviewDTO.setContent(rs.getString(Columns.CONTENT.name));
        reviewDTO.setDate(rs.getObject(Columns.DATE.name, LocalDateTime.class));
        return reviewDTO;
    }

    // =================================================================
    // 3. UPDATE SECTION
    // =================================================================

//
//    //Update(수정) 기능
//    public void update(Connection conn, ReviewDTO reviewDTO) throws SQLException {
//        final String UPDATE_SQL = "UPDATE review SET star = ?, contents = ? WHERE user_id = ? AND project_id = ?";
//
//        try(PreparedStatement psmt = conn.prepareStatement(UPDATE_SQL)){
//
//            psmt.setLong(1, reviewDTO.getStar());
//            psmt.setString(2, reviewDTO.getContent());
//            psmt.setLong(3, reviewDTO.getUserId());
//            psmt.setLong(4, reviewDTO.getProjectId());
//
//            psmt.executeUpdate();
//        }
//    }

    // =================================================================
    // 4. DELETE SECTION
    // =================================================================

    public void delete(Connection conn, long fundId) throws SQLException {
        new SQLDelete(Tables.REVIEWS)
                .addCondition(
                        new BasicCondition(
                                new ColumnValueToken(Columns.FUND_ID),
                                ArithmeticConditionOperator.EQ,
                                new UserValueToken(fundId)
                        )
                ).execute(conn);

    }
}
