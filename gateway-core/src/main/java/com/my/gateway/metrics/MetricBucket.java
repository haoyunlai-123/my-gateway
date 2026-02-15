package com.my.gateway.metrics;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 每个时间窗口的指标桶，记录请求数、成功数、失败数、4xx数、5xx数、总RT、最大RT.
 * 以及一个简单的RT直方图用于近似分位计算。
 */
public class MetricBucket {

    public final LongAdder requests = new LongAdder();
    public final LongAdder success = new LongAdder();
    public final LongAdder fail = new LongAdder();
    public final LongAdder status4xx = new LongAdder();
    public final LongAdder status5xx = new LongAdder();

    public final LongAdder totalRtMs = new LongAdder();
    public final AtomicLong maxRtMs = new AtomicLong(0);

    // 近似分位：简单 log2 直方图（0~1ms,1~2ms,2~4ms...）
    private static final int HIST_SIZE = 32;
    public final LongAdder[] hist = new LongAdder[HIST_SIZE];

    public MetricBucket() {
        for (int i = 0; i < HIST_SIZE; i++) {
            hist[i] = new LongAdder();
        }
    }

    public void record(int statusCode, long rtMs) {
        requests.increment();
        totalRtMs.add(rtMs);
        maxRtMs.accumulateAndGet(rtMs, Math::max);

        if (statusCode >= 200 && statusCode < 400) {
            success.increment();
        } else {
            fail.increment();
        }

        if (statusCode >= 400 && statusCode < 500) {
            status4xx.increment();
        }
        if (statusCode >= 500) {
            status5xx.increment();
        }

        hist[bucket(rtMs)].increment();
    }

    private int bucket(long ms) {
        if (ms <= 0) {
            return 0;
        }
        int b = 63 - Long.numberOfLeadingZeros(ms); // log2
        if (b < 0) {
            b = 0;
        }
        if (b >= HIST_SIZE) {
            b = HIST_SIZE - 1;
        }
        return b;
    }

    public long avgRtMs() {
        long c = requests.sum();
        if (c == 0) {
            return 0;
        }
        return totalRtMs.sum() / c;
    }

    // 近似 pxx：在直方图上做累计
    public long percentile(double p) {
        long total = 0;
        for (LongAdder a : hist) {
            total += a.sum();
        }
        if (total == 0) {
            return 0;
        }

        long target = (long) Math.ceil(total * p);
        long cum = 0;
        for (int i = 0; i < HIST_SIZE; i++) {
            cum += hist[i].sum();
            if (cum >= target) {
                // 反推 bucket 上界：2^i
                return 1L << i;
            }
        }
        return 1L << (HIST_SIZE - 1);
    }
}
