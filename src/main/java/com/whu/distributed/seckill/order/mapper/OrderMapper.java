package com.whu.distributed.seckill.order.mapper;

import com.whu.distributed.seckill.order.entity.Order;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface OrderMapper {

    @Insert("""
            INSERT INTO orders(order_no, user_id, product_id, quantity, amount, status, created_at, updated_at)
            VALUES(#{orderNo}, #{userId}, #{productId}, #{quantity}, #{amount}, #{status}, NOW(), NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);

    @Select("""
            SELECT id, order_no, user_id, product_id, quantity, amount, status, created_at, updated_at
            FROM orders
            WHERE order_no = #{orderNo}
            LIMIT 1
            """)
    Order findByOrderNo(@Param("orderNo") String orderNo);

    @Select("""
            SELECT id, order_no, user_id, product_id, quantity, amount, status, created_at, updated_at
            FROM orders
            WHERE user_id = #{userId}
            ORDER BY id DESC
            """)
    List<Order> findByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT id, order_no, user_id, product_id, quantity, amount, status, created_at, updated_at
            FROM orders
            WHERE user_id = #{userId}
              AND product_id = #{productId}
            LIMIT 1
            """)
    Order findByUserAndProduct(@Param("userId") Long userId, @Param("productId") Long productId);

    @Update("""
            UPDATE orders
            SET status = #{status},
                updated_at = NOW()
            WHERE order_no = #{orderNo}
            """)
    int updateStatus(@Param("orderNo") String orderNo, @Param("status") String status);
}
