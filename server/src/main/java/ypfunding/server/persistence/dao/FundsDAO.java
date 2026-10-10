package ypfunding.server.persistence.dao;

import ypfunding.common.dto.FundDTO;

import java.sql.ResultSet;

public class FundsDAO extends DAO{
    public enum Columns implements ColumnsEnum{
        FUND_ID("fund_id"),
        USER_ID("user_id"),
        PROJECT_ID("project_id"),
        REWARD_NAME("reward_id"),
        QUANTITY("quantity"),
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

    public void fillDataFromResultSet(FundDTO dto, ResultSet rs){
        //TODO
    }


}
