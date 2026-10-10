package ypfunding.server.persistence.dao;

import ypfunding.common.dto.ReviewDTO;
import ypfunding.server.util.DynamicSQLUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReviewsDAO extends DAO{
    public enum Columns implements ColumnsEnum{
        FUND_ID("fund_id"),
        STAR("star"),
        CONTENT("content"),
        DATE("date");

        private final String name;
        Columns(String name){
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public void fillDataFromResultSet(ReviewDTO dto, ResultSet rs){
        //TODO
    }

    public void insert(Connection conn, ReviewDTO reviewDTO) throws SQLException {
        String sql = DynamicSQLUtil.generate("INSERT INTO reviews ({}) VALUES ({})",
                Columns.FUND_ID, Columns.STAR, Columns.CONTENT, Columns.DATE);

        try(PreparedStatement psmt = conn.prepareStatement(sql)){

            psmt.setLong(1, reviewDTO.getFundID());
            psmt.setLong(2, reviewDTO.getStar());
            psmt.setString(3, reviewDTO.getContent());
            psmt.setTimestamp(4, new java.sql.Timestamp(reviewDTO.getDate().getTime()) );

            psmt.executeUpdate();
        }
    }
    //Read(조회) 리뷰는 해당 상품에 대해 전체 조회가 기본이니 전체 조회로 만들겟음
    public List<ReviewDTO> findAll(Connection conn, long projectId) throws SQLException {
        final String FIND_ALL_SQL = "SELECT * FROM review WHERE project_id = ?";
        List<ReviewDTO> result = new ArrayList<>();

        try(PreparedStatement statement = conn.prepareStatement(FIND_ALL_SQL)){
            statement.setLong(1, projectId);

            try(ResultSet rs = statement.executeQuery()){
                while (rs.next()){
                    ReviewDTO reviewDTO = new ReviewDTO();
                    reviewDTO.setFundID(rs.getLong(Columns.FUND_ID.toString()));
                    reviewDTO.setStar(rs.getInt(Columns.STAR.toString()));
                    reviewDTO.setContent(rs.getString(Columns.CONTENT.toString()));
                    reviewDTO.setDate(rs.getTimestamp(Columns.DATE.toString()));
                    result.add(reviewDTO);
                }
            }

        }
        return result;
    }
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

    //delete(삭제) 기능
    public void delete(Connection conn, long userID, long projectID) throws SQLException {
        final String DELETE_SQL = "DELETE FROM review WHERE user_id = ?  AND project_id = ?";

        try(PreparedStatement psmt = conn.prepareStatement(DELETE_SQL)){

            psmt.setLong(1, userID);
            psmt.setLong(2, projectID);

            psmt.executeUpdate();
        }
    }
}
