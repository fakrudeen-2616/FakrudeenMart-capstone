package com.fakrudeenmart.service;

import com.fakrudeenmart.dao.CategoryDAO;
import com.fakrudeenmart.model.Category;

import java.util.List;
import java.util.Optional;

public class CategoryService {
    private final CategoryDAO categoryDAO;
    public CategoryService() { this(new CategoryDAO()); }
    public CategoryService(CategoryDAO categoryDAO) { this.categoryDAO = categoryDAO; }
    public List<Category> getAllCategories() { return categoryDAO.findAll(); }
    public Optional<Category> getCategoryById(Long id) {
        if (id == null || id <= 0) throw new IllegalArgumentException("Category id must be positive.");
        return categoryDAO.findById(id);
    }
}
