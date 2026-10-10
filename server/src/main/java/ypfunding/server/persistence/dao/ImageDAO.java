package ypfunding.server.persistence.dao;

import ypfunding.common.dto.ImageDTO;

import java.sql.ResultSet;

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

    public void fillDataFromResultSet(ImageDTO dto, ResultSet rs){
        //TODO
    }
}
