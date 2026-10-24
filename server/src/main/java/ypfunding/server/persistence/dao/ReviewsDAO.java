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
                .addTarget(Columns.FUND_ID, reviewDTO.getFundID())
                .addTarget(Columns.STAR, reviewDTO.getStar())
                .addTarget(Columns.CONTENT, reviewDTO.getContent())
                .addTarget(Columns.DATE, reviewDTO.getDate())
                .execute(conn);
    }

    // =================================================================
    // 2. READ SECTION
    // =================================================================

    //Read(조회) 리뷰는 해당 상품에 대해 전체 조회가 기본이니 전체 조회로 만들겟음
    public List<ReviewDTO> findAll(Connection conn, long projectId) throws SQLException {

        var reviewsTableReference = new DirectReferenceTable(Tables.REVIEWS);
        var fundsTableReference = new DirectReferenceTable(Tables.FUNDS);
        List<ReviewDTO> result = new ArrayList<>();

        try (ResultSet rs = SQLSelect.allColumns(
                        new JoinedTable(reviewsTableReference, JoinType.LEFT, fundsTableReference,
                                new BasicCondition(
                                        new TableRefColumnToken(fundsTableReference, FundsDAO.Columns.PROJECT_ID),
                                        ArithmeticConditionOperator.EQ,
                                        new UserValueToken(projectId)
                                )
                        )
                ).execute(conn)) {
            while (rs.next()) {
                ReviewDTO reviewDTO = new ReviewDTO();
                reviewDTO.setFundID(rs.getLong(Columns.FUND_ID.toString()));
                reviewDTO.setStar(rs.getInt(Columns.STAR.toString()));
                reviewDTO.setContent(rs.getString(Columns.CONTENT.toString()));
                reviewDTO.setDate(rs.getObject(Columns.DATE.toString(), LocalDateTime.class));
                result.add(reviewDTO);
            }

        }


        return result;
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
//            psmt.setLong(3, reviewDTO.getUserID());
//            psmt.setLong(4, reviewDTO.getProjectID());
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
