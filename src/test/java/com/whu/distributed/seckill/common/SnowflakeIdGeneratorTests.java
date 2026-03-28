package com.whu.distributed.seckill.common;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnowflakeIdGeneratorTests {

    @Test
    void shouldGenerateUniqueIncreasingIds() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator("test-instance");
        Set<Long> ids = new HashSet<>();
        long previous = -1L;

        for (int i = 0; i < 1000; i++) {
            long current = generator.nextId();
            ids.add(current);
            assertTrue(current > previous);
            previous = current;
        }

        assertEquals(1000, ids.size());
    }
}
