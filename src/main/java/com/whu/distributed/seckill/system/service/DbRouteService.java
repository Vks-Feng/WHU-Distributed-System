package com.whu.distributed.seckill.system.service;

import com.whu.distributed.seckill.system.dto.DbNodeInfo;
import com.whu.distributed.seckill.system.mapper.DbNodeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DbRouteService {

    private final DbNodeMapper dbNodeMapper;

    public DbRouteService(DbNodeMapper dbNodeMapper) {
        this.dbNodeMapper = dbNodeMapper;
    }

    public DbNodeInfo writeNode() {
        return dbNodeMapper.currentNode();
    }

    @Transactional(readOnly = true)
    public DbNodeInfo readNode() {
        return dbNodeMapper.currentNode();
    }
}
