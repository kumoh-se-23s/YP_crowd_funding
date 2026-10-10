package ypfunding.server.persistence.dao;

import ypfunding.common.dto.RewardDTO;

import java.sql.ResultSet;

public class RewardsDAO extends DAO{
    public enum Columns implements ColumnsEnum{
        REWARD_ID("reward_id"),
        PROJECT_ID("project_id"),
        NAME("name"),
        PRICE("price"),
        DESCRIPTION("description"),
        STOCK("stock");

        private final String name;
        Columns(String name){
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }


    public void fillDataFromResultSet(RewardDTO dto, ResultSet rs){
        //TODO
    }

}
