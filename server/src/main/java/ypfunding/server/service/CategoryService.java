package ypfunding.server.service;

import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.CategoryDAO;

public class CategoryService extends Service{
    private final CategoryDAO categoryDAO;
    public CategoryService(TransactionManager transactionManager, CategoryDAO categoryDAO) {
        super(transactionManager);
        this.categoryDAO = categoryDAO;
    }
}
