package ypfunding.server.persistence.dao;


import ypfunding.common.dto.CategoryDTO;
import ypfunding.common.type.Category;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLDelete;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLInsert;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLSelect;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLUpdate;
import ypfunding.server.persistence.dao.dynamicsql.col.OrderBySelector;
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

    // =================================================================
    // 1. CREATE SECTION
    // =================================================================

    /**
     * 프로젝트의 카테고리를 등록 기능
     * @param conn 커넥션
     * @param categoryDTO 등록할 카테고리 정보
     * @throws SQLException DB 오류 발생 시 (중복 카테고리 시 에러 1062)
     */
    public void insert(Connection conn, CategoryDTO categoryDTO) throws SQLException {
        new SQLInsert(Tables.CATEGORY)
                .addTarget(Columns.PROJECT_ID, categoryDTO.getProjectId())
                .addTarget(Columns.CATEGORY_NAME, categoryDTO.getCategoryName().name())
                .execute(conn);
    }

    // =================================================================
    // 2. READ SECTION
    // =================================================================

    /**
     * 특정 프로젝트에 속한 카테고리 목록을 조회 기능 (프로젝트 상세 보기용)
     * @param conn 커넥션
     * @param projectId 조회할 프로젝트 ID
     * @return 해당 프로젝트의 카테고리 목록
     * @throws SQLException DB 오류 발생 시
     */
    public List<CategoryDTO> findByProjectId(Connection conn, long projectId) throws SQLException {
        List<CategoryDTO> resultList = new ArrayList<>();

        try (ResultSet rs = SQLSelect.allColumns(Tables.CATEGORY)
                .addCondition(new BasicCondition(
                        new ColumnValueToken(Columns.PROJECT_ID),
                        ArithmeticConditionOperator.EQ,
                        new UserValueToken(projectId)
                ))
                .orderBy(new OrderBySelector(new ColumnValueToken(Columns.CATEGORY_ID), true))
                .execute(conn)) {
            while (rs.next()) {
                resultList.add(toDTO(rs));
            }
        }
        return resultList;
    }

    /**
     * 특정 카테고리에 속한 모든 내역 조회 기능
     * @param conn 커넥션
     * @param categoryDTO (조회할 카테고리 조건)
     * @return 카테고리 DTO 목록
     * @throws SQLException DB 오류 발생 시
     */
    public List<CategoryDTO> getProjectsByCategory(Connection conn, CategoryDTO categoryDTO) throws SQLException {
        List<CategoryDTO> resultList = new ArrayList<>();

        try (ResultSet rs = SQLSelect.allColumns(Tables.CATEGORY)
                .addCondition(new BasicCondition(
                        new ColumnValueToken(Columns.CATEGORY_NAME),
                        ArithmeticConditionOperator.EQ,
                        new UserValueToken(categoryDTO.getCategoryName().name())
                ))
                .execute(conn)) {
            while (rs.next()) {
                resultList.add(toDTO(rs));
            }
        }
        return resultList;
    }

    private CategoryDTO toDTO(ResultSet rs) throws SQLException {
        CategoryDTO dto = new CategoryDTO();
        dto.setCategoryId(rs.getLong(Columns.CATEGORY_ID.name));
        dto.setProjectId(rs.getLong(Columns.PROJECT_ID.name));
        dto.setCategoryName(Category.valueOf(rs.getString(Columns.CATEGORY_NAME.name)));
        return dto;
    }

    // =================================================================
    // 3. UPDATE SECTION
    // =================================================================
    /**
     * 카테고리 수정 기능
     * @param conn 커넥션
     * @param categoryDTO 수정할 카테고리 정보 (categoryId, categoryName 필수)
     * @return 영향받은 행 수
     * @throws SQLException DB 오류 발생 시
     */
    public int update(Connection conn, CategoryDTO categoryDTO) throws SQLException {
        return new SQLUpdate(Tables.CATEGORY,
                new UpdateSelector[]{
                        new UpdateSelector(Columns.CATEGORY_NAME, new UserValueToken(categoryDTO.getCategoryName().name()))
                })
                .addCondition(new BasicCondition(
                        new ColumnValueToken(Columns.CATEGORY_ID),
                        ArithmeticConditionOperator.EQ,
                        new UserValueToken(categoryDTO.getCategoryId())
                ))
                .execute(conn);
    }

    // =================================================================
    // 4. DELETE SECTION
    // =================================================================
    /**
     * 특정 프로젝트의 카테고리 삭제 기능
     * @param conn 커넥션
     * @param projectId 프로젝트ID
     * @return 삭제된 행 수
     * @throws SQLException DB 오류 발생 시
     */
    public int deleteByProjectId(Connection conn, long projectId) throws SQLException {
        return new SQLDelete(Tables.CATEGORY)
                .addCondition(new BasicCondition(
                        new ColumnValueToken(Columns.PROJECT_ID),
                        ArithmeticConditionOperator.EQ,
                        new UserValueToken(projectId)
                ))
                .execute(conn);
    }
}
