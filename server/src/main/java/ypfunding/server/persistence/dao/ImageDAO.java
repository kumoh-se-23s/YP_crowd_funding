package ypfunding.server.persistence.dao;

import ypfunding.common.dto.ImageDTO;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLInsert;

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

    // =================================================================
    // 3. UPDATE SECTION
    // =================================================================


    // =================================================================
    // 4. DELETE SECTION
    // =================================================================

}
