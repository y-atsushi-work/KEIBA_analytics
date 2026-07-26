package com.keiba.analytics.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keiba.analytics.entity.Race;
import com.keiba.analytics.entity.ScrapingFailureLog;
import com.keiba.analytics.repository.RaceRepository;
import com.keiba.analytics.repository.ScrapingFailureLogRepository;
import com.keiba.analytics.util.KeibaUtils;

@Service
public class ScrapingRetryService {

	private final ScrapingFailureLogRepository failureLogRepository;
	private final KeibaScraper keibaScraper;
	private final RaceRepository raceRepository;

	// ★定数: 最大リトライ許容回数（これを超えたらゾンビ化を防ぐため自動リトライを停止する）
	private static final int MAX_RETRY_COUNT = 5;

	public ScrapingRetryService(
			ScrapingFailureLogRepository failureLogRepository,
			KeibaScraper keibaScraper,
			RaceRepository raceRepository) {
		this.failureLogRepository = failureLogRepository;
		this.keibaScraper = keibaScraper;
		this.raceRepository = raceRepository;
	}

	@Transactional
	public void retryFailedScraping() throws InterruptedException {
		List<ScrapingFailureLog> unresolvedLogs = failureLogRepository.findByResolvedFalse();

		if (unresolvedLogs.isEmpty()) {
			System.out.println("[✨リトライバッチ] 未解決の失敗ログはありません。");
			return;
		}

		System.out.println("[🔄リトライバッチ開始] 未解決の失敗ログ件数: " + unresolvedLogs.size());

		for (ScrapingFailureLog log : unresolvedLogs) {

			// ★ガード条件: すでに最大リトライ回数に達している場合は自動リトライ対象外としてスキップ
			if (log.getRetryCount() >= MAX_RETRY_COUNT) {
				System.out.println("[⚠️スキップ] 対象ID: " + log.getTargetId() + " はリトライ上限回数(" + MAX_RETRY_COUNT
						+ "回)に達したため自動リトライを停止します（要手動確認）。");
				continue;
			}

			// ★リトライ回数を1回増やす
			log.setRetryCount(log.getRetryCount() + 1);

			String targetId = log.getTargetId();
			boolean success = false;

			try {
				// パターンA: 12桁のレースIDの場合
				if (targetId.matches("\\d{12}")) {
					System.out.println("[🔄再試行:レース詳細] 対象ID: " + targetId + " (試行回数: " + log.getRetryCount() + "回目)");
					LocalDate date = LocalDate.parse(targetId.substring(0, 8), DateTimeFormatter.ofPattern("yyyyMMdd"));

					Race race = keibaScraper.parseRaceResultPagePublic(targetId, date);
					if (race != null && !race.getRaceResults().isEmpty()) {
						raceRepository.save(race);
						success = true;
					}
				}
				// パターンB: 日付の場合 (8桁)
				else if (targetId.matches("\\d{8}")) {
					System.out
							.println("[🔄再試行:日付別レースID一覧] 対象日付: " + targetId + " (試行回数: " + log.getRetryCount() + "回目)");
					LocalDate date = LocalDate.parse(targetId, DateTimeFormatter.ofPattern("yyyyMMdd"));
					List<String> raceIds = keibaScraper.fetchRaceIdsOfDate(date);
					if (!raceIds.isEmpty()) {
						success = true;
					}
				}

				if (success) {
					log.setResolved(true);
					System.out.println("[✅リトライ成功] ログを解決済みに更新しました: " + targetId);
				}

			} catch (Exception e) {
				System.err.println("[❌リトライ失敗] 対象ID " + targetId + " エラー: " + e.getMessage());
			}

			// 変更したリトライ回数（および解決ステータス）をDBに保存
			failureLogRepository.save(log);

			KeibaUtils.randomSleep();
		}

		System.out.println("[🏁リトライバッチ終了]");
	}
}