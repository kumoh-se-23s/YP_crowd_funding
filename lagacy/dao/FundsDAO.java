package kr.yp_crowdfunding.persistence.dao;

import javax.sql.DataSource;

public class FundsDAO extends DAO{
    public enum Columns{
        USER_ID("user_id"),
        PROJECT_ID("project_id"),
        REWARD_NAME("reward_name"),
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

    public FundsDAO(DataSource dataSource) {
        super(dataSource);
    }
}
