package ypfunding.server.persistence.dao.dynamicsql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract sealed class Condition permits BasicCondition, CombinedCondition {

    List<Object> userParameters;

    protected Condition(List<Object> userParameters) {
        this.userParameters = List.copyOf(userParameters);
    }

    public static Condition and(Condition lhs, Condition rhs) {
        List<Object> combined = new ArrayList<>();
        combined.addAll(lhs.userParameters);
        combined.addAll(rhs.userParameters);

        return new CombinedCondition("(" + lhs + ") AND (" + rhs + ")", combined);
    }

    public static Condition or(Condition lhs, Condition rhs) {
        List<Object> combined = new ArrayList<>();
        combined.addAll(lhs.userParameters);
        combined.addAll(rhs.userParameters);

        return new CombinedCondition("(" + lhs + ") OR (" + rhs + ")", combined);
    }

    public static Condition not(Condition v) {
        return new CombinedCondition("NOT (" + v + ")", v.userParameters);
    }

    public List<Object> getUserParameters() {
        return Collections.unmodifiableList(userParameters);
    }

    public static String whereChain(List<Condition> conditions) {
        StringBuilder whereBuilder = new StringBuilder();
        for (Condition condition : conditions) {
            whereBuilder.append(" where ").append(condition);
        }
        return whereBuilder.toString();
    }
}
