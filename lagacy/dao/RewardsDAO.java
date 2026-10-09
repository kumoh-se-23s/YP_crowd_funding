package kr.yp_crowdfunding.persistence.dao;

import javax.sql.DataSource;

public class RewardsDAO extends DAO{
    public enum Columns{
        PROJECT_ID("project_id"),
        NAME("name"),
        DESCRIPTION("description"),
        PRICE("price"),
        COUNT("count");

        private final String name;
        Columns(String name){
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public RewardsDAO(DataSource dataSource) {
        super(dataSource);
    }
}
