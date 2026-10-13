package ypfunding.server.persistence.dao.dynamicsql.token;

public abstract sealed class SetValueToken extends ValueToken permits RawSetValueToken {
    protected SetValueToken() {

    }
}
