package ypfunding.server.persistence.dao.dynamicsql;

public abstract sealed class SetValueToken extends ValueToken permits RawSetValueToken {
    protected SetValueToken() {

    }
}
