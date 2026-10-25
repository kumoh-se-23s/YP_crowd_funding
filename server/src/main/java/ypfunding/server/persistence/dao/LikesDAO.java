package ypfunding.server.persistence.dao;

import ypfunding.common.dto.LikeDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LikesDAO extends DAO {

    public enum Columns implements ColumnsEnum{
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

    public void fillDataFromResultSet(LikeDTO dto, ResultSet rs) throws SQLException{
        dto.setUserId(rs.getLong(Columns.USER_ID.toString()));
        dto.setProjectId(rs.getLong(Columns.PROJECT_ID.toString()));
    }

    // =================================================================
    // 1. CREATE SECTION
    // =================================================================

    //좋아요 추가
    public void insert(Connection conn, LikeDTO likeDTO) throws SQLException {
        final String INSERT_SQL =
                "INSERT INTO likes (user_id, project_id) VALUES (?, ?)";

        try (PreparedStatement psmt = conn.prepareStatement(INSERT_SQL)) {

            psmt.setLong(1, likeDTO.getUserId());
            psmt.setLong(2, likeDTO.getProjectId());

            psmt.executeUpdate();
        }
    }

    // =================================================================
    // 2. READ SECTION
    // =================================================================

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

    // =================================================================
    // 3. DELETE SECTION
    // =================================================================

    //좋아요 취소
    //취소할 줄을 찾아 취소했을시 1을 반환 아닌경우 0
    public int delete(Connection conn, long userId, long projectId) throws SQLException {
        final String DELETE_SQL = "DELETE FROM likes WHERE user_id = ? AND project_id = ?";

        try ( PreparedStatement psmt = conn.prepareStatement(DELETE_SQL)) {

            psmt.setLong(1, userId);
            psmt.setLong(2, projectId);

            return psmt.executeUpdate();
        }
    }
}
