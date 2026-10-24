package ypfunding.server.persistence.dao.dynamicsql.token;

import java.util.List;

public final class BuiltinFunctionToken extends ValueToken{
    private final BuiltinFunctions func;
    private final ValueToken[] parameters;

    public BuiltinFunctionToken(BuiltinFunctions func, ValueToken... parameters){
        this.func = func;
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return BuiltinFunctions.makeFunction(func, parameters).toString();
    }

    @Override
    public List<Object> getUserParameters() {
        return ValueToken.combineUserParameters(parameters);
    }
}
