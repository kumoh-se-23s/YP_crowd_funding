package ypfunding.server.service;

import ypfunding.server.persistence.dao.CategoriesDAO;

public class CategoryService extends Service<CategoriesDAO>{
    public CategoryService(CategoriesDAO dao) {
        super(dao);
    }
}
