package ypfunding.server.persistence.dao;


import ypfunding.common.dto.CategoryDTO;
import ypfunding.server.util.DynamicSQLUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO extends DAO{
    public enum Columns implements ColumnsEnum{
        CATEGORY_ID("category_id"),
        PROJECT_ID("project_id"),
        CATEGORY_NAME("category_name");
        private final String name;
        Columns(String name){
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public void fillDataFromResultSet(CategoryDTO dto, ResultSet rs){
        //TODO
    }


    //Create(생성) 기능
    public void insert(Connection conn, CategoryDTO categoryDTO) throws SQLException {
        final String INSERT_SQL = DynamicSQLUtil.generate("INSERT INTO category ({}) VALUES ({})",
                Columns.CATEGORY_ID, Columns.PROJECT_ID, Columns.CATEGORY_NAME);

        try(PreparedStatement psmt = conn.prepareStatement(INSERT_SQL)){

            psmt.setLong(1, categoryDTO.getCategoryId());
            psmt.setLong(2, categoryDTO.getProjectId());
            psmt.setString(3, categoryDTO.getCategoryName().name());
            psmt.executeUpdate();

        }
    }

    //Read(조회) 기능
    public List<CategoryDTO> getProjectsByCategory(Connection conn, CategoryDTO categoryDTO) throws SQLException {
        final String READ_SQL = DynamicSQLUtil.generate("SELECT * FROM category WHERE project_id = ({})",
                Columns.CATEGORY_ID);
        List<CategoryDTO> resultList = new ArrayList<>();

        try (PreparedStatement psmt = conn.prepareStatement(READ_SQL)) {

             psmt.setLong(1, categoryDTO.getCategoryId());


            try (ResultSet rs = psmt.executeQuery()) {
                //카테고리 내 모든 내역 반환
                while (rs.next()) {
                    CategoryDTO dto = new CategoryDTO();

                    dto.setProjectId(rs.getLong(Columns.PROJECT_ID.toString()));
                    dto.setCategoryName(CategoryDTO.Category.valueOf(rs.getString(Columns.CATEGORY_NAME.toString())));

                    resultList.add(dto);
                }
            }
        }
        return resultList;
    }

    //UPDATE 수정 기능(카테고리 잘못 기입)
    public void update(Connection conn, CategoryDTO categoryDTO) throws SQLException {
        final String UPDATE_SQL = DynamicSQLUtil.generate("UPDATE category SET ({}) WHERE project_id = ({})",
                Columns.CATEGORY_NAME, Columns.PROJECT_ID);

        try(PreparedStatement psmt = conn.prepareStatement(UPDATE_SQL)){
            psmt.setString(1, categoryDTO.getCategoryName().name());
            psmt.setLong(2, categoryDTO.getProjectId());

            psmt.executeUpdate();
        }
    }
}
