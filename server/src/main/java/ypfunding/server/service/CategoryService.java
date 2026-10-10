package ypfunding.server.service;

import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.CategoriesDAO;

public class CategoryService extends Service<CategoriesDAO>{
    public CategoryService(TransactionManager transactionManager, CategoriesDAO dao) {
        super(transactionManager, dao);
    }
}
