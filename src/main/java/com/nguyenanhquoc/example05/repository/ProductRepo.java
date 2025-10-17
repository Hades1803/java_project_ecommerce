package com.nguyenanhquoc.example05.repository;

import com.nguyenanhquoc.example05.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ProductRepo extends JpaRepository<Product,Long> {
    Page<Product> findByProductNameContainingIgnoreCase(String keyword, Pageable pageable);

}
