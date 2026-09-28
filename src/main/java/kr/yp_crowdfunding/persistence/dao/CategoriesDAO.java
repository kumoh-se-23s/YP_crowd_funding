package kr.yp_crowdfunding.persistence.dao;

import kr.yp_crowdfunding.persistence.dto.CategoryDTO;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoriesDAO extends DAO{
    public enum Columns{
        PROJECT_ID("project_id"),
        CATEGORY("category");
        private final String name;
        Columns(String name){
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
    public CategoriesDAO(DataSource dataSource) {
        super(dataSource);
    }

    //Create(생성) 기능
    public void insert(CategoryDTO categoryDTO) throws SQLException {
        final String INSERT_SQL =
                "INSERT INTO categories (categotry) VALUES (?)";

        try(Connection conn = dataSource.getConnection();
            PreparedStatement psmt = conn.prepareStatement(INSERT_SQL)){

            psmt.setString(1, categoryDTO.getCategory().name());
            psmt.executeUpdate();

        }
    }

    //Read(조회) 기능
    //얘는 추가 회의 후 구현해야할 듯
    //1. CategoryDTO에 projectId -> categoryId로 변경이 맞아보이는데 단독행동일수도잇으니 일단 이야기하는거로
    //2. Id(숫자) 기반 조회로 만들것인지, BOOK(문자) 기반 조회로 만들것인지
    //사실 둘다 만들어버려도 ok긴함 --> 대신 실행과정에서 어떻게 조회하고 싶은지
    //CLI면 숫자가 편할거고 GUI면 텍스트(어차피 클릭인데 정보는 텍스트 일거니)가 편하지 않을까? 라는 생각을 가지고 있긴함.
    public List<CategoryDTO> getProjectsByCategory(CategoryDTO.Category categoryType) throws SQLException {
        final String READ_SQL = "SELECT * FROM categories WHERE category = ?";
        List<CategoryDTO> resultList = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement psmt = conn.prepareStatement(READ_SQL)) {

             psmt.setString(1, categoryType.name());

            try (ResultSet rs = psmt.executeQuery()) {
                //카테고리 내 모든 내역 반환
                while (rs.next()) {
                    CategoryDTO dto = new CategoryDTO();

                    dto.setProjectID(rs.getLong(Columns.PROJECT_ID.toString()));
                    dto.setCategory(CategoryDTO.Category.valueOf(rs.getString(Columns.CATEGORY.toString())));

                    resultList.add(dto);
                }
            }
        }
        return resultList;
    }

    //UD는 필요할까 싶긴한데 상황에 따라 U는 필요할거 같으니 추후 구현
}
