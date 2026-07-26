package com.keiba.analytics.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.keiba.analytics.entity.ScrapingFailureLog;
import com.keiba.analytics.repository.ScrapingFailureLogRepository;

@Component
public class RaceSyncManager implements CommandLineRunner {

	private final KeibaScraper keibaScraper;
	private final RaceService raceService;
	private final ScrapingFailureLogRepository failureLogRepository;

	public RaceSyncManager(
			KeibaScraper keibaScraper, 
			RaceService raceService,
			ScrapingFailureLogRepository failureLogRepository) {
		this.keibaScraper = keibaScraper;
		this.raceService = raceService;
		this.failureLogRepository = failureLogRepository;
	}

	@Override
	public void run(String... args) throws Exception {
		System.out.println("[🚀同期システム] 過去7年分データ一括取得バッチを開始します...");

		// 過去7年分の設定（例: 2019年1月1日 〜 今日の日付）
		int startYear = 2019;
		int endYear = LocalDate.now().getYear(); // 現在の年（2026年など）

		LocalDate startDate = LocalDate.of(startYear, 1, 1);
		LocalDate endDate = LocalDate.now();

		System.out.println("[⚙️同期システム] 【過去7年分一括モード】 " + startDate + " から " + endDate + " まで1日ずつ調査します。");

		LocalDate currentDate = startDate;
		while (!currentDate.isAfter(endDate)) {

			System.out.println("\n--------------------------------------------------");
			System.out.println("[⚙️同期システム] 調査中: " + currentDate);

			try {
				// 1. その日のレースID一覧を取得
				List<String> raceIds = keibaScraper.fetchRaceIdsOfDate(currentDate);

				if (raceIds == null || raceIds.isEmpty()) {
					System.out.println("[⚙️スキップ] " + currentDate + " はレース開催がありませんでした。");
				} else {
					System.out.println("[⚙️開催日発見] " + currentDate + " に " + raceIds.size() + " 件のレースを検出しました。");

					for (String raceId : raceIds) {
						try {
							System.out.println("[💾同期中] レースID: " + raceId);
							raceService.syncRaceResult(raceId);

							// レース詳細の取得間隔（サーバー負荷軽減）
							Thread.sleep(3000);

						} catch (Exception e) {
							System.err.println("[❌エラー] レースID: " + raceId + " の同期に失敗しました: " + e.getMessage());
							// 失敗したレースIDは失敗ログに保存（後からリトライバッチが回収）
							failureLogRepository.save(new ScrapingFailureLog(raceId, e.getMessage()));
						}
					}
				}
			} catch (Exception e) {
				String dateStr = currentDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
				System.err.println("[❌エラー] 日付別一覧取得失敗 (" + currentDate + "): " + e.getMessage());
				// 日付単位の失敗もログに保存
				failureLogRepository.save(new ScrapingFailureLog(dateStr, e.getMessage()));
			}

			// 日付が変わるごとの安全スリープ
			System.out.println("[⚙️同期システム] サーバー負荷軽減のため、3秒間待機して次の日に進みます...");
			Thread.sleep(3000);

			// 次の日へ進める
			currentDate = currentDate.plusDays(1);
		}

		System.out.println("\n[🏁同期システム] 過去7年分のデータ一括チェックがすべて完了しました！失敗したデータは週次リトライバッチで自動回収されます。");
	}
}