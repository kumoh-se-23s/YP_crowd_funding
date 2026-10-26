package ypfunding.server.check;

import ypfunding.server.bootstrap.AppContext;

public class CheckBase {
    protected final AppContext context;
    protected final CheckUsers users;

    protected CheckBase(AppContext context, CheckUsers users) {
        this.context = context;
        this.users = users;
    }

}
