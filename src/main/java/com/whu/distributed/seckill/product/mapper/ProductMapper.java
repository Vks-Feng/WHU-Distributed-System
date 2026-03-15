package com.whu.distributed.seckill.product.mapper;

import com.whu.distributed.seckill.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductMapper {

    @Select("SELECT id, name, price, status, created_at, updated_at FROM products WHERE id = #{id} LIMIT 1")
    Product findById(@Param("id") Long id);

    @Select("""
            SELECT id, name, price, status, created_at, updated_at
            FROM products
            ORDER BY id ASC
            LIMIT #{size} OFFSET #{offset}
            """)
    List<Product> findPage(@Param("offset") int offset, @Param("size") int size);
}
