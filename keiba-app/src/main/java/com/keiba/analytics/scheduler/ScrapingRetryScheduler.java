package com.keiba.analytics.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.keiba.analytics.service.ScrapingRetryService;

@Component
public class ScrapingRetryScheduler {

    private final ScrapingRetryService retryService;

    public ScrapingRetryScheduler(ScrapingRetryService retryService) {
        this.retryService = retryService;
    }

    /**
     * 週1回のリトライバッチ実行
     * Cron式: "秒 分 時 日 月 曜日"
     * 例: 毎週月曜日の午前3時0分0秒に実行 -> "0 0 3 ? * MON"
     */
    @Scheduled(cron = "0 0 3 ? * MON")
    public void scheduleWeeklyRetry() {
        System.out.println("[⏰定期実行] 失敗データの週次リカバリバッチが起動しました。");
        try {
			retryService.retryFailedScraping();
		} catch (InterruptedException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
    }
}