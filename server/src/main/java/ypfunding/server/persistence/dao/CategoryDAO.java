package ypfunding.server.persistence.dao;


import ypfunding.common.dto.CategoryDTO;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLInsert;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLSelect;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLUpdate;
import ypfunding.server.persistence.dao.dynamicsql.col.UpdateSelector;
import ypfunding.server.persistence.dao.dynamicsql.condition.BasicCondition;
import ypfunding.server.persistence.dao.dynamicsql.op.ArithmeticConditionOperator;
import ypfunding.server.persistence.dao.dynamicsql.token.ColumnValueToken;
import ypfunding.server.persistence.dao.dynamicsql.token.UserValueToken;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO extends DAO {
    public enum Columns implements ColumnsEnum {
        CATEGORY_ID("category_id"),
        PROJECT_ID("project_id"),
        CATEGORY_NAME("category_name");
        private final String name;

        Columns(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public void fillDataFromResultSet(CategoryDTO dto, ResultSet rs) {
        //TODO
    }

    // =================================================================
    // 1. CREATE SECTION
    // =================================================================

    public void insert(Connection conn, CategoryDTO categoryDTO) throws SQLException {
        new SQLInsert(Tables.CATEGORY)
                .addTarget(Columns.CATEGORY_ID, categoryDTO.getCategoryId())
                .addTarget(Columns.PROJECT_ID, categoryDTO.getProjectId())
                .addTarget(Columns.CATEGORY_NAME, categoryDTO.getCategoryName())
                .execute(conn);

    }

    // =================================================================
    // 2. READ SECTION
    // =================================================================

    public List<CategoryDTO> getProjectsByCategory(Connection conn, CategoryDTO categoryDTO) throws SQLException {
        List<CategoryDTO> resultList = new ArrayList<>();

        try (ResultSet rs = SQLSelect.allColumns(Tables.CATEGORY)
                .addCondition(new BasicCondition(
                                new UserValueToken(categoryDTO.getCategoryId()),
                                ArithmeticConditionOperator.EQ,
                                new ColumnValueToken(Columns.CATEGORY_ID)
                        )
                ).execute(conn)) {
            //카테고리 내 모든 내역 반환
            while (rs.next()) {
                CategoryDTO dto = new CategoryDTO();

                dto.setProjectId(rs.getLong(Columns.PROJECT_ID.toString()));
                dto.setCategoryName(CategoryDTO.Category.valueOf(rs.getString(Columns.CATEGORY_NAME.toString())));

                resultList.add(dto);
            }
        }
        return resultList;
    }

    // =================================================================
    // 3. UPDATE SECTION
    // =================================================================

    //카테고리 수정 기능(카테고리 잘못 기입)
    public void update(Connection conn, CategoryDTO categoryDTO) throws SQLException {
        new SQLUpdate(Tables.CATEGORY,
                new UpdateSelector(Columns.CATEGORY_NAME, new UserValueToken(categoryDTO.getCategoryName()))         )
                .addCondition(new BasicCondition(
                        new UserValueToken(categoryDTO.getProjectId()),
                        ArithmeticConditionOperator.EQ,
                        new ColumnValueToken(Columns.CATEGORY_ID)
                ))
                .execute(conn);

    }

    // =================================================================
    // 4. DELETE SECTION
    // =================================================================

}
