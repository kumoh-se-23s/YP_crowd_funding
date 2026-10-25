package ypfunding.server.persistence.dao.dynamicsql.token;

import java.util.List;

public final class WildcardValueToken extends ValueToken {

    private static final WildcardValueToken INSTANCE = new WildcardValueToken();
    public WildcardValueToken(){
    }

    public WildcardValueToken instance(){
        return INSTANCE;
    }


    @Override
    public String toString() {
        return "*";
    }

    @Override
    public List<Object> getUserParameters(){
        return List.of();
    }
}
