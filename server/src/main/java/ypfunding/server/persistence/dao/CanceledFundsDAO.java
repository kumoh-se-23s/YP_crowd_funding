package ypfunding.server.persistence.dao;

import ypfunding.common.dto.CanceledFundDTO;

import java.sql.ResultSet;

public class CanceledFundsDAO extends DAO{
    public enum Columns implements ColumnsEnum{
        CANCELED_FUND_ID("canceled_fund_id"),
        USER_ID("user_id"),
        PROJECT_ID("project_id"),
        REWARD_NAME("reward_id"),
        QUANTITY("quantity"),
        CONTENT("content"),
        CANCELED_DATE("canceled_date");

        private final String name;
        Columns(String name){
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public void fillDataFromResultSet(CanceledFundDTO dto, ResultSet rs){
        //TODO
    }

}
