package ypfunding.server.persistence.dao;

import ypfunding.common.dto.FundQueryDTO;
import ypfunding.common.dto.ProjectFunderDTO;
import ypfunding.common.type.ProjectFundingStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FundQueryDAO extends DAO{
    public enum Columns implements ColumnsEnum{
        REWARD_NAME("reward_name"),
        AMOUNT("amount"),
        STATUS("status"),
        REVIEWED("reviewed");

        private final String name;

        Columns(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    // =================================================================
    // READ SECTION
    // =================================================================

    //후원 내역
    public List<FundQueryDTO> findMyFunds(Connection conn, long userId, LocalDateTime now) throws SQLException {
        final String MY_FUNDS_SQL = """
                WITH project_totals AS (
                    SELECT f2.project_id,
                           SUM(r2.price * f2.quantity) AS total
                    FROM funds f2
                    JOIN rewards r2 ON r2.reward_id = f2.reward_id
                    GROUP BY f2.project_id
                )
                SELECT f.fund_id,
                       p.project_id,
                       p.title,
                       r.name AS reward_name,
                       f.quantity,
                       f.date,
                       r.price * f.quantity AS amount,
                       CASE
                           WHEN p.end_date > ? THEN 'ONGOING'
                           WHEN pt.total >= p.goal THEN 'SUCCESS'
                           ELSE 'FAILED'
                       END AS status,
                       EXISTS (SELECT 1 FROM reviews rv WHERE rv.fund_id = f.fund_id) AS reviewed
                FROM funds f
                JOIN rewards r ON r.reward_id = f.reward_id
                JOIN projects p ON p.project_id = f.project_id
                JOIN project_totals pt ON pt.project_id = f.project_id
                WHERE f.user_id = ?
                """;

        List<FundQueryDTO> result = new ArrayList<>();

        try (PreparedStatement psmt = conn.prepareStatement(MY_FUNDS_SQL)) {
            psmt.setObject(1, now);
            psmt.setLong(2, userId);

            try (ResultSet rs = psmt.executeQuery()) {
                while (rs.next()) {
                    FundQueryDTO dto = new FundQueryDTO();
                    dto.setFundId(rs.getLong(FundsDAO.Columns.FUND_ID.toString()));
                    dto.setProjectId(rs.getLong(ProjectsDAO.Columns.PROJECT_ID.toString()));
                    dto.setProjectTitle(rs.getString(ProjectsDAO.Columns.TITLE.toString()));
                    dto.setRewardName(rs.getString(Columns.REWARD_NAME.toString()));
                    dto.setQuantity(rs.getLong(FundsDAO.Columns.QUANTITY.toString()));
                    dto.setAmount(rs.getLong(Columns.AMOUNT.toString()));
                    dto.setDate(rs.getObject(FundsDAO.Columns.DATE.toString(), LocalDateTime.class));
                    dto.setCanceled(false);
                    dto.setStatus(ProjectFundingStatus.valueOf(rs.getString(Columns.STATUS.toString())));
                    dto.setReviewed(rs.getBoolean(Columns.REVIEWED.toString()));
                    result.add(dto);
                }
            }
        }
        return result;
    }

    //취소한 후원 내역
    public List<FundQueryDTO> findMyCanceledFunds(Connection conn, long userId) throws SQLException{
        final String MY_CANCELED_SQL = """
                SELECT c.canceled_funds_id,
                       p.project_id,
                       p.title,
                       r.name AS reward_name,
                       c.quantity,
                       r.price * c.quantity AS amount,
                       c.content,
                       c.cancel_date
                FROM canceled_funds c
                JOIN rewards  r ON r.reward_id  = c.reward_id
                JOIN projects p ON p.project_id = c.project_id
                WHERE c.user_id = ?
                """;

        List<FundQueryDTO> result = new ArrayList<>();

        try (PreparedStatement psmt = conn.prepareStatement(MY_CANCELED_SQL)) {
            psmt.setLong(1, userId);

            try (ResultSet rs = psmt.executeQuery()) {
                while (rs.next()) {
                    FundQueryDTO dto = new FundQueryDTO();
                    dto.setCanceledFundId(rs.getLong(CanceledFundsDAO.Columns.CANCELED_FUND_ID.toString()));
                    dto.setProjectId(rs.getLong(ProjectsDAO.Columns.PROJECT_ID.toString()));
                    dto.setProjectTitle(rs.getString(ProjectsDAO.Columns.TITLE.toString()));
                    dto.setRewardName(rs.getString(Columns.REWARD_NAME.toString()));
                    dto.setQuantity(rs.getLong(CanceledFundsDAO.Columns.QUANTITY.toString()));
                    dto.setAmount(rs.getLong(Columns.AMOUNT.toString()));
                    dto.setDate(rs.getObject(CanceledFundsDAO.Columns.CANCELED_DATE.toString(), LocalDateTime.class));
                    dto.setCanceled(true);
                    dto.setCancelReason(rs.getString(CanceledFundsDAO.Columns.CONTENT.toString()));
                    result.add(dto);
                }
            }
        }
        return result;
    }

    //프로젝트 성공 여부 판단
    public ProjectFundingStatus findProjectStatus(Connection conn, long projectId, LocalDateTime now) throws SQLException {
        final String STATUS_SQL = """
            SELECT CASE
                       WHEN p.end_date > ? THEN 'ONGOING'
                       WHEN COALESCE((SELECT SUM(r.price * f.quantity)
                                        FROM funds f
                                        JOIN rewards r ON r.reward_id = f.reward_id
                                       WHERE f.project_id = p.project_id), 0) >= p.goal THEN 'SUCCESS'
                       ELSE 'FAILED'
                   END AS status
            FROM projects p
            WHERE p.project_id = ?
            """;

        try (PreparedStatement psmt = conn.prepareStatement(STATUS_SQL)) {
            psmt.setObject(1, now);
            psmt.setLong(2, projectId);

            try (ResultSet rs = psmt.executeQuery()) {
                return rs.next() ? ProjectFundingStatus.valueOf(rs.getString(Columns.STATUS.toString())) : null;
            }
        }
    }

    //메이커가 자신의 프로젝트에 후원한 후원자 목록 조회
    public List<ProjectFunderDTO> findFundersByProject(Connection conn, long projectId) throws SQLException {
        final String FUNDERS_SQL = """
                SELECT f.fund_id,
                       u.login_id,
                       u.name,
                       r.name AS reward_name,
                       f.quantity,
                       r.price * f.quantity AS amount,
                       f.date
                FROM funds f
                JOIN users   u ON u.user_id   = f.user_id
                JOIN rewards r ON r.reward_id = f.reward_id
                WHERE f.project_id = ?
                ORDER BY f.date DESC
                """;

        List<ProjectFunderDTO> result = new ArrayList<>();

        try (PreparedStatement psmt = conn.prepareStatement(FUNDERS_SQL)) {
            psmt.setLong(1, projectId);

            try (ResultSet rs = psmt.executeQuery()) {
                while (rs.next()) {
                    ProjectFunderDTO dto = new ProjectFunderDTO();
                    dto.setFundId(rs.getLong(FundsDAO.Columns.FUND_ID.toString()));
                    dto.setLoginId(rs.getString(UsersDAO.Columns.LOGIN_ID.toString()));
                    dto.setName(rs.getString(UsersDAO.Columns.NAME.toString()));
                    dto.setRewardName(rs.getString(Columns.REWARD_NAME.toString()));
                    dto.setQuantity(rs.getLong(FundsDAO.Columns.QUANTITY.toString()));
                    dto.setAmount(rs.getLong(Columns.AMOUNT.toString()));
                    dto.setDate(rs.getObject(FundsDAO.Columns.DATE.toString(), LocalDateTime.class));
                    result.add(dto);
                }
            }
        }
        return result;
    }
}
