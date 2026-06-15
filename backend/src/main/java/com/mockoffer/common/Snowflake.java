package com.mockoffer.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 雪花 ID 生成器：64 位 = 41 位毫秒时间戳 + 10 位 workerId + 12 位序列。
 * 非 users 表统一用它生成主键（分布式、不暴露业务量）。M1 单实例 worker 默认 1，多实例由配置区分。
 */
@Component
public class Snowflake {

    private static final long EPOCH = 1_700_000_000_000L; // 2023-11-15 起算
    private static final long WORKER_BITS = 10L;
    private static final long SEQ_BITS = 12L;
    private static final long MAX_WORKER = ~(-1L << WORKER_BITS);
    private static final long MAX_SEQ = ~(-1L << SEQ_BITS);

    private final long workerId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public Snowflake(@Value("${app.snowflake.worker-id:1}") long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER) {
            throw new IllegalArgumentException("workerId 超出范围 0.." + MAX_WORKER);
        }
        this.workerId = workerId;
    }

    public synchronized long nextId() {
        long ts = System.currentTimeMillis();
        if (ts < lastTimestamp) {
            ts = lastTimestamp; // 时钟回拨：不倒退，等下一序列
        }
        if (ts == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQ;
            if (sequence == 0) {
                while ((ts = System.currentTimeMillis()) <= lastTimestamp) {
                    // 自旋到下一毫秒
                }
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = ts;
        return ((ts - EPOCH) << (WORKER_BITS + SEQ_BITS)) | (workerId << SEQ_BITS) | sequence;
    }
}
