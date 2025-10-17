package com.nguyenanhquoc.example05.service;

import com.nguyenanhquoc.example05.entity.Category;
import com.nguyenanhquoc.example05.payloads.dto.CategoryDTO;
import com.nguyenanhquoc.example05.payloads.response.CategoryResponse;

public interface CategoryService {

    CategoryDTO createCategory(Category category);

    CategoryResponse getCategories(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    CategoryDTO updateCategory(Category category, Long categoryId);

    String deleteCategory(Long categoryId);
}