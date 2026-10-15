package ypfunding.server.persistence.dao;

import ypfunding.server.persistence.dao.dynamicsql.table.DirectReferenceTables;

public enum Tables implements DirectReferenceTables {
    USERS("users"),
    FUNDS("funds"),
    CATEGORY("category"),
    CANCELED_FUNDS("canceled_funds"),
    IMAGE("image"),
    LIKES("likes"),
    PROJECTS("projects"),
    REJECT("reject"),
    REVIEWS("reviews"),
    REWARDS("rewards"),
    ;

    private final String name;

    Tables(String name){
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
