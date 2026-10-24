package ypfunding.server.persistence.dao.dynamicsql.col;

import lombok.NonNull;
import ypfunding.server.persistence.dao.dynamicsql.token.ValueToken;

import java.util.List;

public record OrderBySelector(ValueToken target, boolean ascending) {
    @Override
    @NonNull
    public String toString() {
        return " order by " + target + (ascending ? " asc" : " desc");
    }

    public List<Object> getUserParameters() {
        return target.getUserParameters();
    }
}
