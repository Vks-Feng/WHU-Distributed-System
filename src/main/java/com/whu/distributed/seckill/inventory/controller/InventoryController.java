package com.whu.distributed.seckill.inventory.controller;

import com.whu.distributed.seckill.common.ApiResponse;
import com.whu.distributed.seckill.inventory.dto.InventoryStockResponse;
import com.whu.distributed.seckill.inventory.service.InventoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventories")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{productId}")
    public ApiResponse<InventoryStockResponse> getByProductId(@PathVariable Long productId) {
        return ApiResponse.ok(inventoryService.getByProductId(productId));
    }
}
