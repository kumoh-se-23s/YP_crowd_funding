package ypfunding.server.persistence.dao;

import ypfunding.common.dto.LikeDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LikesDAO extends DAO {

    public enum Columns {
        USER_ID("user_id"),
        PROJECT_ID("project_id");
        private final String name;

        Columns(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public void fillDataFromResultSet(LikeDTO dto, ResultSet rs){
        //TODO
    }


    //Create(생성) 좋아요 추가
    public void insert(Connection conn, LikeDTO likeDTO) throws SQLException {
        final String INSERT_SQL =
                "INSERT INTO likes (user_id, project_id) VALUES (?, ?)";

        try (PreparedStatement psmt = conn.prepareStatement(INSERT_SQL)) {

            psmt.setLong(1, likeDTO.getUserID());
            psmt.setLong(2, likeDTO.getProjectID());

            psmt.executeUpdate();
        }
    }

    //Delete 좋아요 취소
    public void delete(Connection conn, long userId, long projectId) throws SQLException {
        final String DELETE_SQL = "DELETE FROM likes WHERE user_id = ? AND project_id = ?";

        try ( PreparedStatement psmt = conn.prepareStatement(DELETE_SQL)) {

            psmt.setLong(1, userId);
            psmt.setLong(2, projectId);

            psmt.executeUpdate();
        }
    }

    //Read, 전체 좋아요 개수 조회 기능
    public int getLikeCount(Connection conn, long projectId) throws SQLException {
        final String GET_COUNT_SQL = "SELECT COUNT(*) FROM likes WHERE project_id = ?";

        try (PreparedStatement psmt = conn.prepareStatement(GET_COUNT_SQL)) {
            psmt.setLong(1, projectId);

            try (ResultSet rs = psmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    //단일 인원 좋아요 기능 필요??
    //update는 필요 X로 판단.
}
