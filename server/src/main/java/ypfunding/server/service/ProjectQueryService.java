package ypfunding.server.service;

import ypfunding.server.persistence.TransactionManager;

public class ProjectQueryService extends Service{

    public ProjectQueryService(TransactionManager transactionManager) {
        super(transactionManager);
    }

}
