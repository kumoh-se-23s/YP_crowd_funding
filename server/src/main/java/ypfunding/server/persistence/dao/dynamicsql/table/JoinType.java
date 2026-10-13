package ypfunding.server.persistence.dao.dynamicsql.table;

public enum JoinType {
    LEFT("left join"),
    RIGHT("right join"),
    INNER("inner join");

    private final String name;
    JoinType(String name){
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
