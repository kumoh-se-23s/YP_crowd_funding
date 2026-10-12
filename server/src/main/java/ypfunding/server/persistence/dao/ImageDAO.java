package ypfunding.server.persistence.dao;

import ypfunding.common.dto.ImageDTO;
import ypfunding.server.util.DynamicSQLUtil;

import java.sql.*;

public class ImageDAO extends DAO{

    public enum Columns implements ColumnsEnum{
        IMAGE_ID("image_id"),
        REWARD_ID("reward_id"),
        NAME("name"),
        TYPE("type"),
        IMAGE_DATA("image_data");

        private final String name;
        Columns(String name){
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private void fillDataFromResultSet(ImageDTO dto, ResultSet rs){
        //TODO
    }
    // =================================================================
    // 1. CREATE SECTION
    // =================================================================

    public void insert(Connection conn, ImageDTO imageDTO) throws SQLException {
        String sql = DynamicSQLUtil.generate("INSERT INTO image ({}) VALUES ({})",
                Columns.REWARD_ID, Columns.NAME, Columns.TYPE, Columns.IMAGE_DATA);

        try (PreparedStatement psmt = conn.prepareStatement(sql)) {
            setInsertParameters(psmt, imageDTO);
            psmt.executeUpdate();
        }
    }

    private void setInsertParameters(PreparedStatement psmt, ImageDTO imageDTO) throws SQLException {
        psmt.setLong(1, imageDTO.getRewardId());
        psmt.setString(2, imageDTO.getName());
        psmt.setString(3, imageDTO.getType());

        psmt.setBlob(4, imageDTO.getImageData());
    }
    // =================================================================
    // 2. READ SECTION
    // =================================================================

    // =================================================================
    // 3. UPDATE SECTION
    // =================================================================


    // =================================================================
    // 4. DELETE SECTION
    // =================================================================

}
