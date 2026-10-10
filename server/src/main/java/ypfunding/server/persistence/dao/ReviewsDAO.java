package ypfunding.server.persistence.dao;

import ypfunding.common.dto.ReviewDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReviewsDAO extends DAO{
    public enum Columns{
        USER_ID("user_id"),
        PROJECT_ID("project_id"),
        REWARD_NAME("reward_name"),
        STAR("star"),
        CONTENTS("contents"),
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
    public void insert(Connection conn, ReviewDTO reviewDTO) throws SQLException {
        final String INSERT_SQL =
                "INSERT INTO review (user_id, project_id, rewardName, star, contents, date) VALUES (?, ?, ?, ?, ?, ?)";

        try(PreparedStatement psmt = conn.prepareStatement(INSERT_SQL)){

            psmt.setLong(1, reviewDTO.getUserID());
            psmt.setLong(2, reviewDTO.getProjectID());
            psmt.setString(3, reviewDTO.getRewardName());
            psmt.setLong(4, reviewDTO.getStar());
            psmt.setString(5, reviewDTO.getContents());
            psmt.setTimestamp(6, new java.sql.Timestamp(reviewDTO.getDate().getTime()) );

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
                    reviewDTO.setUserID(rs.getLong(Columns.USER_ID.toString()));
                    reviewDTO.setProjectID(rs.getLong(Columns.PROJECT_ID.toString()));
                    reviewDTO.setRewardName(rs.getString(Columns.REWARD_NAME.toString()));
                    reviewDTO.setStar(rs.getInt(Columns.STAR.toString()));
                    reviewDTO.setContents(rs.getString(Columns.CONTENTS.toString()));
                    reviewDTO.setDate(rs.getTimestamp(Columns.DATE.toString()));
                    result.add(reviewDTO);
                }
            }

        }
        return result;
    }

    //Update(수정) 기능
    public void update(Connection conn, ReviewDTO reviewDTO) throws SQLException {
        final String UPDATE_SQL = "UPDATE review SET star = ?, contents = ? WHERE user_id = ? AND project_id = ?";

        try(PreparedStatement psmt = conn.prepareStatement(UPDATE_SQL)){

            psmt.setLong(1, reviewDTO.getStar());
            psmt.setString(2, reviewDTO.getContents());
            psmt.setLong(3, reviewDTO.getUserID());
            psmt.setLong(4, reviewDTO.getProjectID());

            psmt.executeUpdate();
        }
    }

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
