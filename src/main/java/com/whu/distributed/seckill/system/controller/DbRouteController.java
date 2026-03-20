package com.whu.distributed.seckill.system.controller;

import com.whu.distributed.seckill.common.ApiResponse;
import com.whu.distributed.seckill.system.dto.DbNodeInfo;
import com.whu.distributed.seckill.system.service.DbRouteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system/db-route")
public class DbRouteController {

    private final DbRouteService dbRouteService;

    public DbRouteController(DbRouteService dbRouteService) {
        this.dbRouteService = dbRouteService;
    }

    @GetMapping("/write")
    public ApiResponse<DbNodeInfo> writeNode() {
        return ApiResponse.ok(dbRouteService.writeNode());
    }

    @GetMapping("/read")
    public ApiResponse<DbNodeInfo> readNode() {
        return ApiResponse.ok(dbRouteService.readNode());
    }
}
