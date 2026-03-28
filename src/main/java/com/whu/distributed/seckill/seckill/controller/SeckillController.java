package com.whu.distributed.seckill.seckill.controller;

import com.whu.distributed.seckill.common.ApiResponse;
import com.whu.distributed.seckill.seckill.dto.SeckillRequest;
import com.whu.distributed.seckill.seckill.dto.SeckillSubmitResponse;
import com.whu.distributed.seckill.seckill.service.SeckillService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seckill")
public class SeckillController {

    private final SeckillService seckillService;

    public SeckillController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    @PostMapping("/orders")
    public ApiResponse<SeckillSubmitResponse> submit(@RequestBody SeckillRequest request) {
        return ApiResponse.ok(seckillService.submit(request));
    }
}
