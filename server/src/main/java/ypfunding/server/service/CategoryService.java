package ypfunding.server.service;

import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.CategoryDAO;

public class CategoryService extends Service<CategoryDAO>{
    public CategoryService(TransactionManager transactionManager, CategoryDAO dao) {
        super(transactionManager, dao);
    }
}
