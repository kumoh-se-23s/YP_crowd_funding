package ypfunding.server.persistence.dao;

import ypfunding.common.dto.ImageDTO;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLDelete;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLInsert;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLSelect;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLUpdate;
import ypfunding.server.persistence.dao.dynamicsql.col.ColumnSelector;
import ypfunding.server.persistence.dao.dynamicsql.col.DirectColumnSelector;
import ypfunding.server.persistence.dao.dynamicsql.col.UpdateSelector;
import ypfunding.server.persistence.dao.dynamicsql.condition.BasicCondition;
import ypfunding.server.persistence.dao.dynamicsql.op.ArithmeticConditionOperator;
import ypfunding.server.persistence.dao.dynamicsql.table.DirectReferenceTable;
import ypfunding.server.persistence.dao.dynamicsql.token.ColumnValueToken;
import ypfunding.server.persistence.dao.dynamicsql.token.UserValueToken;

import javax.sql.rowset.serial.SerialBlob;
import java.sql.*;

public class ImageDAO extends DAO {

    public enum Columns implements ColumnsEnum {
        IMAGE_ID("image_id"),
        REWARD_ID("reward_id"),
        NAME("name"),
        TYPE("type"),
        IMAGE_DATA("image_data");

        private final String name;

        Columns(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public void fillDataFromResultSet(ImageDTO dto, ResultSet rs) {
        //TODO
    }
    // =================================================================
    // 1. CREATE SECTION
    // =================================================================

    /**
     * 리워드 이미지를 등록
     *
     * @param conn     커넥션
     * @param imageDTO 등록할 이미지 정보 (rewardId, name, type, imageData 등등)
     * @throws SQLException DB 오류 발생 시
     */
    public void insert(Connection conn, ImageDTO imageDTO) throws SQLException {
        new SQLInsert(Tables.IMAGE)
                .addTarget(Columns.REWARD_ID, imageDTO.getRewardId())
                .addTarget(Columns.NAME, imageDTO.getName())
                .addTarget(Columns.TYPE, imageDTO.getType())
                .addTarget(Columns.IMAGE_DATA, imageDTO.getImageData())
                .execute(conn);
    }

    // =================================================================
    // 2. READ SECTION
    // =================================================================

    /**
     * 특정 리워드에 이미지가 존재하는지 여부만 확인 기능
     * 프로젝트 상세 보기에서 각 리워드의 이미지 첨부 여부를 표시할 때 사용한다.
     *
     * @param conn     커넥션
     * @param rewardId 리워드 ID
     * @return 이미지가 존재하면 true, 없으면 false
     * @throws SQLException DB 오류 발생 시
     */
    public boolean isExistsByRewardId(Connection conn, long rewardId) throws SQLException {
        try (ResultSet rs = new SQLSelect(
                new DirectColumnSelector(Columns.IMAGE_ID),
                new DirectReferenceTable(Tables.IMAGE))
                .addCondition(new BasicCondition(
                        new ColumnValueToken(Columns.REWARD_ID),
                        ArithmeticConditionOperator.EQ,
                        new UserValueToken(rewardId)))
                .execute(conn)) {
            return rs.next();
        }
    }

    /**
     * 특정 리워드의 이미지 메타데이터(ID, 파일명, MIME 타입)만 조회한다 (IMAGE_DATA 제외).
     *
     * @param conn     커넥션
     * @param rewardId 리워드 ID
     * @return IMAGE_DATA가 비어있는 ImageDTO (없으면 null)
     * @throws SQLException DB 오류 발생 시
     */
    public ImageDTO findMetadataByRewardId(Connection conn, long rewardId) throws SQLException {
        ColumnSelector[] metaColumns = new ColumnSelector[]{
                new DirectColumnSelector(Columns.IMAGE_ID),
                new DirectColumnSelector(Columns.REWARD_ID),
                new DirectColumnSelector(Columns.NAME),
                new DirectColumnSelector(Columns.TYPE)
        };
        try (ResultSet rs = new SQLSelect(metaColumns, new DirectReferenceTable(Tables.IMAGE))
                .addCondition(new BasicCondition(
                        new ColumnValueToken(Columns.REWARD_ID),
                        ArithmeticConditionOperator.EQ,
                        new UserValueToken(rewardId)))
                .execute(conn)) {
            if (rs.next()) {
                ImageDTO dto = new ImageDTO();
                dto.setImageId(rs.getLong(Columns.IMAGE_ID.name));
                dto.setRewardId(rs.getLong(Columns.REWARD_ID.name));
                dto.setName(rs.getString(Columns.NAME.name));
                dto.setType(rs.getString(Columns.TYPE.name));
                return dto;
            }
            return null;
        }
    }

    /**
     * 특정 리워드의 이미지 전체(IMAGE_DATA BLOB 포함)를 조회한다.
     * 트랜잭션(Connection)이 닫힌 뒤에도 Service 밖에서 안전하게 바이트를 읽을 수 있도록
     * 메모리에 복사된 SerialBlob으로 변환하여 담아 반환한다.
     *
     * @param conn     트랜잭션 커넥션
     * @param rewardId 리워드 ID
     * @return 이미지 데이터가 포함된 ImageDTO (존재하지 않으면 null)
     * @throws SQLException DB 오류 발생 시
     */
    public ImageDTO findByRewardId(Connection conn, long rewardId) throws SQLException {
        try (ResultSet rs = SQLSelect.allColumns(new DirectReferenceTable(Tables.IMAGE))
                .addCondition(new BasicCondition(
                        new ColumnValueToken(Columns.REWARD_ID),
                        ArithmeticConditionOperator.EQ,
                        new UserValueToken(rewardId)))
                .execute(conn)) {
            if (rs.next()) {
                return toDTO(rs);
            }
            return null;
        }
    }

    private ImageDTO toDTO(ResultSet rs) throws SQLException {
        ImageDTO dto = new ImageDTO();
        dto.setImageId(rs.getLong(Columns.IMAGE_ID.name));
        dto.setRewardId(rs.getLong(Columns.REWARD_ID.name));
        dto.setName(rs.getString(Columns.NAME.name));
        dto.setType(rs.getString(Columns.TYPE.name));
        dto.setImageData(rs.getBytes(Columns.IMAGE_DATA.name)); // SerialBlob 없이 byte[] 그대로 저장!
        return dto;
    }

    // =================================================================
    // 3. UPDATE SECTION
    // =================================================================
    /**
     * 리워드의 이미지 수정 기능
     * @param conn     커넥션
     * @param imageDTO 수정할 이미지 정보
     * @return 영향받은 행 수
     * @throws SQLException DB 오류 발생 시
     */
    public int update(Connection conn, ImageDTO imageDTO) throws SQLException {
        return new SQLUpdate(Tables.IMAGE, new UpdateSelector[]{
                new UpdateSelector(Columns.NAME, new UserValueToken(imageDTO.getName())),
                new UpdateSelector(Columns.TYPE, new UserValueToken(imageDTO.getType())),
                new UpdateSelector(Columns.IMAGE_DATA, new UserValueToken(imageDTO.getImageData()))
        })
                .addCondition(new BasicCondition(
                        new ColumnValueToken(Columns.REWARD_ID),
                        ArithmeticConditionOperator.EQ,
                        new UserValueToken(imageDTO.getRewardId())))
                .execute(conn);
    }
    // =================================================================
    // 4. DELETE SECTION
    // =================================================================

    /**
     * 리워드 아래의 이미지(1개)를 삭제 기능
     * @param conn     커넥션
     * @param rewardId 리워드 ID
     * @return 삭제된 행 수
     * @throws SQLException DB 오류 발생 시
     */
    public int deleteByRewardId(Connection conn, long rewardId) throws SQLException {
        return new SQLDelete(Tables.IMAGE)
                .addCondition(new BasicCondition(
                        new ColumnValueToken(Columns.REWARD_ID),
                        ArithmeticConditionOperator.EQ,
                        new UserValueToken(rewardId)))
                .execute(conn);
    }
}
