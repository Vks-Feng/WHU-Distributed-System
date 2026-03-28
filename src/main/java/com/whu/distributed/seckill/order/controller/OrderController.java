package com.whu.distributed.seckill.order.controller;

import com.whu.distributed.seckill.common.ApiResponse;
import com.whu.distributed.seckill.order.dto.OrderDetailResponse;
import com.whu.distributed.seckill.order.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderDetailResponse> getByOrderId(@PathVariable String orderId) {
        return ApiResponse.ok(orderService.getByOrderId(orderId));
    }

    @GetMapping
    public ApiResponse<List<OrderDetailResponse>> listByUserId(@RequestParam Long userId) {
        return ApiResponse.ok(orderService.listByUserId(userId));
    }
}
