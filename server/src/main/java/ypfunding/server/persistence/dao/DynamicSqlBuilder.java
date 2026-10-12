package ypfunding.server.persistence.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class DynamicSqlBuilder {
    private DynamicSqlBuilder() {
    }

    public abstract static sealed class ReferenceTable {

        protected ReferenceTable() {

        }

        public abstract List<Object> getUserParameters();
    }

    public abstract static sealed class ColumnSelector {

        public abstract List<Object> getUserParameters();

        public static String buildColumnParentheses(List<ColumnSelector> selectors) {
            return "(" + selectors.stream().map(Object::toString).collect(Collectors.joining(", ")) + ")";
        }

        public static String buildColumnParenthesesWithWildcard(List<ColumnSelector> selectors) {
            return buildColumnParentheses(selectors) +
                   " values (" +
                   selectors.stream()
                           .map(_ -> "?")
                           .collect(Collectors.joining(",")) + ")";
        }
    }

    public static final class DirectColumnSelector extends ColumnSelector {
        ColumnsEnum column;

        public DirectColumnSelector(ColumnsEnum column) {
            this.column = column;
        }

        @Override
        public String toString() {
            return column.toString();
        }

        @Override
        public List<Object> getUserParameters() {
            return List.of();
        }
    }

    public static final class ScalarSubqueryColumnSelector extends ColumnSelector {
        Select query;

        public ScalarSubqueryColumnSelector(Select query) {
            this.query = query;
        }

        @Override
        public String toString() {
            return query.toString();
        }

        @Override
        public List<Object> getUserParameters() {
            return query.getUserParameters();
        }
    }


    public static final class InlineViewReferenceTable extends ReferenceTable {
        Select query;

        public InlineViewReferenceTable(Select query) {
            this.query = query;
        }

        @Override
        public String toString() {
            return query.toString();
        }

        @Override
        public List<Object> getUserParameters() {
            return query.getUserParameters();
        }
    }

    public static final class DirectReferenceTable extends ReferenceTable {
        private final String tableName;

        public DirectReferenceTable(String table) {
            this.tableName = table;
        }

        @Override
        public String toString() {
            return tableName;
        }

        @Override
        public List<Object> getUserParameters() {
            return List.of();
        }
    }


    public abstract static sealed class ValueToken {
        protected ValueToken() {
        }

        public abstract List<Object> getUserParameters();
    }

    public static final class RawValueToken extends ValueToken {
        private final Number value;

        public RawValueToken(Number value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "?";
        }

        @Override
        public List<Object> getUserParameters() {
            // may contain user parameters
            List<Object> r = new ArrayList<>();
            r.add(value);
            return r;
        }
    }

    public abstract static sealed class SetValueToken extends ValueToken {
        protected SetValueToken() {

        }
    }

    public static final class RawSetValueToken extends SetValueToken {
        private final String[] set;

        public RawSetValueToken(String... set) {
            this.set = set;
        }

        @Override
        public String toString() {

            return "(" + Arrays.stream(set).map(_ -> "?").collect(Collectors.joining(",")) + ")";
        }

        @Override
        public List<Object> getUserParameters() {
            // containing user parameters.
            return new ArrayList<>(Arrays.asList(set));
        }
    }

    public static final class SubqueryToken extends ValueToken {
        private final Select selectQuery;

        public SubqueryToken(Select selectQuery) {
            this.selectQuery = selectQuery;
        }

        @Override
        public String toString() {
            return "(" + selectQuery + ")";
        }

        @Override
        public List<Object> getUserParameters() {
            return new ArrayList<>(selectQuery.getUserParameters());
        }
    }

    public static final class CorrelatedTableValueToken extends ValueToken {
        private final String varName;
        private final ColumnsEnum column;

        public CorrelatedTableValueToken(String varName, ColumnsEnum column) {
            this.varName = varName;
            this.column = column;
        }

        @Override
        public String toString() {
            return varName + "." + column;
        }

        @Override
        public List<Object> getUserParameters() {
            //direct reference of column name. no user parameters
            return List.of();
        }
    }

    public static final class UncorrelatedTableValueToken extends ValueToken {
        private final ColumnsEnum column;

        public UncorrelatedTableValueToken(ColumnsEnum column) {
            this.column = column;
        }

        @Override
        public String toString() {
            return column.toString();
        }

        @Override
        public List<Object> getUserParameters() {
            //direct reference of column name. no user parameters
            return List.of();
        }
    }


    public static final class ValueExpression extends ValueToken {
        private final ValueToken lhs;
        private final ArithmeticOperator operator;
        private final ValueToken rhs;

        public ValueExpression(ValueToken lhs, ArithmeticOperator operator, ValueToken rhs) {
            this.lhs = lhs;
            this.operator = operator;
            this.rhs = rhs;
        }

        @Override
        public String toString() {
            return lhs.toString() + operator + rhs;
        }

        @Override
        public List<Object> getUserParameters() {
            List<Object> userParams = new ArrayList<>();
            userParams.addAll(lhs.getUserParameters());
            userParams.addAll(rhs.getUserParameters());
            return Collections.unmodifiableList(userParams);
        }
    }


    private static class AccumulativePreparedStatement implements AutoCloseable {
        private final PreparedStatement statement;
        private final List<Object> params;

        public AccumulativePreparedStatement(Connection conn, String sql) throws SQLException {
            this.statement = conn.prepareStatement(sql);
            this.params = new ArrayList<>();
        }

        public void addParams(List<Object> params) {
            this.params.addAll(params);
        }

        public void addParam(Object param) {
            params.add(param);
        }

        public PreparedStatement applyParam() throws SQLException {
            for (int i = 0; i < params.size(); ++i) {
                statement.setObject(i + 1, params.get(i));
            }
            return statement;
        }

        @Override
        public void close() throws SQLException {
            statement.close();
        }
    }

    public enum ArithmeticOperator {
        ADD("+"),
        SUB("-"),
        MUL("*"),
        DIV("/");

        private final String str;

        ArithmeticOperator(String s) {
            str = s;
        }


        @Override
        public String toString() {
            return str;
        }
    }

    public interface ConditionOperator {

    }

    public enum ArithmeticConditionOperator implements ConditionOperator {
        LE("<="),
        EQ("="),
        GE(">="),
        GT(">"),
        LT("<"),
        NE("!=");
        private final String str;

        ArithmeticConditionOperator(String s) {
            str = s;
        }

        @Override
        public String toString() {
            return str;
        }

    }

    public enum SetConditionOperator implements ConditionOperator {
        NI("NOT IN"),
        IN("IN");

        private final String str;

        SetConditionOperator(String s) {
            str = s;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    public abstract static sealed class Condition {

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

    public static final class CombinedCondition extends Condition {
        String condition;

        CombinedCondition(String condition, List<Object> parameters) {
            super(parameters);
            this.condition = condition;
        }


        @Override
        public String toString() {
            return condition;
        }
    }


    public static final class BasicCondition extends Condition {
        private final ColumnsEnum column;
        private final ConditionOperator operator;
        private final ValueToken targetValue;

        BasicCondition(ColumnsEnum column, ConditionOperator operator, ValueToken targetValue) {
            super(new ArrayList<>());
            this.column = column;
            this.operator = operator;
            this.targetValue = targetValue;
        }

        @Override
        public String toString() {
            return column.toString() + operator + targetValue;
        }
    }


    private abstract static sealed class SqlBuilderImpl {

        protected final Connection conn;
        protected final List<Condition> conditions = new ArrayList<>();
        protected final List<Object> userParameters = new ArrayList<>();

        protected SqlBuilderImpl(Connection conn) {
            this.conn = conn;
        }

        protected void addConditionImpl(Condition condition) {
            conditions.add(condition);
            userParameters.addAll(condition.getUserParameters());
        }

        public List<Object> getUserParameters() {
            return userParameters;
        }

        public abstract void execute() throws SQLException;
    }

    public static final class Insert extends SqlBuilderImpl {

        private final ReferenceTable referenceTable;
        private final List<ColumnSelector> columns = new ArrayList<>();

        public Insert(Connection conn, ReferenceTable referenceTable) {
            super(conn);
            this.referenceTable = referenceTable;
            this.userParameters.addAll(referenceTable.getUserParameters());
        }


        public Insert addColumn(ColumnSelector selector) {
            columns.add(selector);
            userParameters.addAll(selector.getUserParameters());
            return this;
        }

        @Override
        public void execute() throws SQLException {
            String sql = "insert into " + referenceTable +
                         ColumnSelector.buildColumnParenthesesWithWildcard(columns);

            AccumulativePreparedStatement preparedStatement = new AccumulativePreparedStatement(conn, sql);
            preparedStatement.addParams(userParameters);
            preparedStatement.applyParam().executeQuery();
        }
    }


    public record UpdateSetter(ColumnsEnum column, ValueToken token) {

    }

    public static final class Update extends SqlBuilderImpl {

        private final ReferenceTable referenceTable;
        private final List<ColumnsEnum> columns = new ArrayList<>();

        public Update(Connection conn, ReferenceTable referenceTable, UpdateSetter... setters) {
            super(conn);

            this.referenceTable = referenceTable;
            this.userParameters.addAll(referenceTable.getUserParameters());

            columns.addAll(Arrays.stream(setters).map(UpdateSetter::column).toList());
            for (UpdateSetter setter : setters) {
                userParameters.addAll(setter.token().getUserParameters());
            }
        }

        public Update addCondition(Condition condition) {
            addConditionImpl(condition);
            return this;
        }


        @Override
        public void execute() throws SQLException {
            String setValueQuery = columns.stream().map(e -> e + "=?").collect(Collectors.joining(","));
            String sqlBuilder = "update " +
                                referenceTable +
                                " set " +
                                setValueQuery +
                                Condition.whereChain(conditions);

            AccumulativePreparedStatement preparedStatement = new AccumulativePreparedStatement(conn, sqlBuilder);
            preparedStatement.addParams(userParameters);
            preparedStatement.applyParam().executeUpdate();
        }
    }

    public static final class Delete extends SqlBuilderImpl {

        private final ReferenceTable referenceTable;

        public Delete(Connection conn, ReferenceTable referenceTable) {
            super(conn);
            this.referenceTable = referenceTable;
            this.userParameters.addAll(referenceTable.getUserParameters());
        }

        public Delete addCondition(Condition condition) {
            addConditionImpl(condition);
            return this;
        }

        @Override
        public void execute() throws SQLException {
            String sql = "delete " + referenceTable + Condition.whereChain(conditions);
            AccumulativePreparedStatement preparedStatement = new AccumulativePreparedStatement(conn, sql);
            preparedStatement.addParams(userParameters);
            preparedStatement.applyParam().executeUpdate();

        }
    }

    public static final class Select extends SqlBuilderImpl {
        private final List<ColumnSelector> columnSelectors;
        private final ReferenceTable referenceTable;

        public Select(Connection conn, List<ColumnSelector> columnSelectors, ReferenceTable referenceTable) {
            super(conn);
            this.columnSelectors = columnSelectors;
            this.userParameters.addAll(columnSelectors.stream().map(ColumnSelector::getUserParameters).toList());

            this.referenceTable = referenceTable;
            this.userParameters.addAll(referenceTable.getUserParameters());

        }

        public Select addCondition(Condition condition) {
            addConditionImpl(condition);
            return this;
        }


        @Override
        public void execute() throws SQLException {
            String sql = "select " + ColumnSelector.buildColumnParentheses(columnSelectors) +
                         " from " +
                         referenceTable + Condition.whereChain(conditions);
            AccumulativePreparedStatement preparedStatement = new AccumulativePreparedStatement(conn, sql);
            preparedStatement.addParams(userParameters);
            preparedStatement.applyParam().executeUpdate();
        }
    }

}
