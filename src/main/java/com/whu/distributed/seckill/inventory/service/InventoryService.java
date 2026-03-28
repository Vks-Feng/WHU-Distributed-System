package com.whu.distributed.seckill.inventory.service;

import com.whu.distributed.seckill.inventory.dto.InventoryStockResponse;
import com.whu.distributed.seckill.inventory.entity.Inventory;
import com.whu.distributed.seckill.inventory.mapper.InventoryMapper;
import com.whu.distributed.seckill.seckill.service.SeckillCacheService;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {

    private final InventoryMapper inventoryMapper;
    private final SeckillCacheService seckillCacheService;

    public InventoryService(InventoryMapper inventoryMapper, SeckillCacheService seckillCacheService) {
        this.inventoryMapper = inventoryMapper;
        this.seckillCacheService = seckillCacheService;
    }

    public InventoryStockResponse getByProductId(Long productId) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("invalid product id");
        }

        Inventory inventory = inventoryMapper.findByProductId(productId);
        if (inventory == null) {
            throw new IllegalArgumentException("inventory not found");
        }

        Integer redisAvailableStock = seckillCacheService.getCachedStock(productId);
        return new InventoryStockResponse(
                inventory.getProductId(),
                inventory.getTotalStock(),
                inventory.getAvailableStock(),
                inventory.getLockedStock(),
                redisAvailableStock,
                inventory.getUpdatedAt()
        );
    }
}
