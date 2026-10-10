package ypfunding.server.persistence.dao;

import ypfunding.common.dto.RejectDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RejectDAO extends DAO{

    public enum Columns implements ColumnsEnum{
        REJECTREASON_ID("rejectreason_id"),
        PROJECT_ID("project_id"),
        REASON("reason"),
        CREATED_AT("created_at");

        private final String name;
        Columns(String name){
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public void fillDataFromResultSet(RejectDTO dto, ResultSet rs){
        //TODO
    }


    //Create(생성) 기능
    public void insert(Connection conn, RejectDTO rejectDTO) throws SQLException {
        final String INSERT_SQL =
                "INSERT INTO failReason (project_id, reason, date) VALUES (?, ?, ?)";

        try(PreparedStatement psmt = conn.prepareStatement(INSERT_SQL)){

            psmt.setLong(1, rejectDTO.getProjectID());
            psmt.setString(2, rejectDTO.getReason());
            psmt.setTimestamp(3,  new java.sql.Timestamp(rejectDTO.getCreatedAt().getTime()));

            psmt.executeUpdate();
        }
    }

    //Read(조회) 기능
    //얘는 일반적으로 프로젝트에서 가져와야하는거 같으니 조회기준을 projectId로 전체 조회
    //단일 조회는 필요하려나?
    public List<RejectDTO> getFailReasons(Connection conn, long projectId) throws SQLException {
        final String READ_SQL = "SELECT * FROM failReason WHERE project_id = ?";
        List<RejectDTO> resultList = new ArrayList<>();

        try(PreparedStatement psmt = conn.prepareStatement(READ_SQL)){
            psmt.setLong(1, projectId);

            try (ResultSet rs = psmt.executeQuery()){
                while (rs.next()){
                    RejectDTO dto = new RejectDTO();

                    dto.setRejectReasonId(rs.getLong(Columns.REJECTREASON_ID.name));
                    dto.setProjectID(rs.getLong(Columns.PROJECT_ID.name));
                    dto.setReason(rs.getString(Columns.REASON.name));
                    dto.setCreatedAt(rs.getTimestamp(Columns.CREATED_AT.name));

                    resultList.add(dto);
                }
            }

        }
        return resultList;
    }
    //Update는 오타 대비로 만들어둠
    public void update(Connection conn, RejectDTO rejectDTO) throws SQLException {
        //reason은 내용 수정, id는 수정할 대상 지정
        final String UPDATE_SQL = "UPDATE failReason SET reason = ? WHERE id = ?";

        try(PreparedStatement psmt = conn.prepareStatement(UPDATE_SQL)){

            psmt.setString(1, rejectDTO.getReason());
            psmt.setLong(2, rejectDTO.getRejectReasonId());

            psmt.executeUpdate();
        }
    }

    //Delete 펀딩이 종료 되었을 때 자원관리를 위해 밀어주는 용도로 필요할 듯
    public void delete(Connection conn, long projectId) throws SQLException {
        final String DELETE_SQL = "DELETE FROM failReason WHERE project_id = ?";

        try(PreparedStatement psmt = conn.prepareStatement(DELETE_SQL)){
            psmt.setLong(1, projectId);
            psmt.executeUpdate();
        }
    }
}
