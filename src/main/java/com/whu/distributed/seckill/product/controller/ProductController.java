package com.whu.distributed.seckill.product.controller;

import com.whu.distributed.seckill.common.ApiResponse;
import com.whu.distributed.seckill.product.dto.ProductDetailResponse;
import com.whu.distributed.seckill.product.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductDetailResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(productService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<ProductDetailResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(productService.list(page, size));
    }
}
