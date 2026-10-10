package ypfunding.server.persistence.dao;

import ypfunding.common.dto.FailReasonDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FailReasonsDAO extends DAO{

    public enum Columns{
        ID("id"),
        PROJECT_ID("project_id"),
        REASON("reason"),
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


    //Create(생성) 기능
    public void insert(Connection conn, FailReasonDTO failReasonDTO) throws SQLException {
        final String INSERT_SQL =
                "INSERT INTO failReason (project_id, reason, date) VALUES (?, ?, ?)";

        try(PreparedStatement psmt = conn.prepareStatement(INSERT_SQL)){

            psmt.setLong(1,failReasonDTO.getProjectID());
            psmt.setString(2,failReasonDTO.getReason());
            psmt.setTimestamp(3,  new java.sql.Timestamp(failReasonDTO.getDate().getTime()));

            psmt.executeUpdate();
        }
    }

    //Read(조회) 기능
    //얘는 일반적으로 프로젝트에서 가져와야하는거 같으니 조회기준을 projectId로 전체 조회
    //단일 조회는 필요하려나?
    public List<FailReasonDTO> getFailReasons(Connection conn, long projectId) throws SQLException {
        final String READ_SQL = "SELECT * FROM failReason WHERE project_id = ?";
        List<FailReasonDTO> resultList = new ArrayList<>();

        try(PreparedStatement psmt = conn.prepareStatement(READ_SQL)){
            psmt.setLong(1, projectId);

            try (ResultSet rs = psmt.executeQuery()){
                while (rs.next()){
                    FailReasonDTO dto = new FailReasonDTO();

                    dto.setId(rs.getLong(Columns.ID.name));
                    dto.setProjectID(rs.getLong(Columns.PROJECT_ID.name));
                    dto.setReason(rs.getString(Columns.REASON.name));
                    dto.setDate(rs.getTimestamp(Columns.DATE.name));

                    resultList.add(dto);
                }
            }

        }
        return resultList;
    }
    //Update는 오타 대비로 만들어둠
    public void update(Connection conn, FailReasonDTO failReasonDTO) throws SQLException {
        //reason은 내용 수정, id는 수정할 대상 지정
        final String UPDATE_SQL = "UPDATE failReason SET reason = ? WHERE id = ?";

        try(PreparedStatement psmt = conn.prepareStatement(UPDATE_SQL)){

            psmt.setString(1, failReasonDTO.getReason());
            psmt.setLong(2, failReasonDTO.getId());

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
