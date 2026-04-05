package com.whu.distributed.seckill.inventory.mapper;

import com.whu.distributed.seckill.inventory.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InventoryMapper {

    @Select("""
            SELECT id, product_id, total_stock, available_stock, locked_stock, updated_at
            FROM inventories
            WHERE product_id = #{productId}
            LIMIT 1
            """)
    Inventory findByProductId(@Param("productId") Long productId);

    @Update("""
            UPDATE inventories
            SET available_stock = available_stock - #{quantity},
                updated_at = NOW()
            WHERE product_id = #{productId}
              AND available_stock >= #{quantity}
            """)
    int deductAvailableStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Update("""
            UPDATE inventories
            SET available_stock = available_stock - #{quantity},
                locked_stock = locked_stock + #{quantity},
                updated_at = NOW()
            WHERE product_id = #{productId}
              AND available_stock >= #{quantity}
            """)
    int reserveStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Update("""
            UPDATE inventories
            SET locked_stock = locked_stock - #{quantity},
                updated_at = NOW()
            WHERE product_id = #{productId}
              AND locked_stock >= #{quantity}
            """)
    int confirmReservedStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Update("""
            UPDATE inventories
            SET available_stock = available_stock + #{quantity},
                locked_stock = locked_stock - #{quantity},
                updated_at = NOW()
            WHERE product_id = #{productId}
              AND locked_stock >= #{quantity}
            """)
    int releaseReservedStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
