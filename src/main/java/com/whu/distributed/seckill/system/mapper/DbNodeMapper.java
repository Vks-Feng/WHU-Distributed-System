package com.whu.distributed.seckill.system.mapper;

import com.whu.distributed.seckill.system.dto.DbNodeInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DbNodeMapper {

    @Select("""
            SELECT
                @@hostname AS hostname,
                @@server_id AS serverId,
                @@read_only AS readOnly,
                DATABASE() AS databaseName,
                CURRENT_USER() AS currentUser
            """)
    DbNodeInfo currentNode();
}
