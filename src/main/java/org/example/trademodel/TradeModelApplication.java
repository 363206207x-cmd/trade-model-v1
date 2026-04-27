package org.example.trademodel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // ← 第四轮关键：开启定时任务
public class TradeModelApplication {

    public static void main(String[] args) {
        long startupBegin = System.currentTimeMillis();
        SpringApplication.run(TradeModelApplication.class, args);
        long startupCostMs = System.currentTimeMillis() - startupBegin;
        System.out.println("[PERF] startup=" + startupCostMs + " ms");
        System.out.println("🚀 多源证据驱动的交易决策闭环系统 V1 已启动！端口: 8081");
    }
}
