package com.keiba.analytics.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.keiba.analytics.entity.ScrapingFailureLog;
import com.keiba.analytics.repository.RaceRepository;
import com.keiba.analytics.repository.ScrapingFailureLogRepository;

@Component
public class RaceSyncManager implements CommandLineRunner {

	private final KeibaScraper keibaScraper;
	private final RaceService raceService;
	private final ScrapingFailureLogRepository failureLogRepository;
	private final RaceRepository raceRepository;

	// コンストラクタインジェクションに RaceRepository を追加
	public RaceSyncManager(
			KeibaScraper keibaScraper,
			RaceService raceService,
			ScrapingFailureLogRepository failureLogRepository,
			RaceRepository raceRepository) {
		this.keibaScraper = keibaScraper;
		this.raceService = raceService;
		this.failureLogRepository = failureLogRepository;
		this.raceRepository = raceRepository;
	}

	@Override
	public void run(String... args) throws Exception {
		System.out.println("[🚀同期システム] レースデータ自動同期バッチを開始します...");

		// =====================================================================================
		// 【変更点】固定の2019年スタートではなく、データベースの最新日付から続きを自動で再開する設定
		// =====================================================================================

		// 1. データベースにすでに保存されているレースの中で、最も新しい開催日を検索する
		Optional<LocalDate> latestDateOpt = raceRepository.findLatestRaceDate();

		LocalDate startDate;
		if (latestDateOpt.isPresent()) {
			// データがすでに存在する場合は、「最後に保存された日」を起点にする
			// （※もし完全な重複日を避けたければ plusDays(1) をつけて翌日からにすることも可能です）
			startDate = latestDateOpt.get();
			System.out.println("[⚙️同期システム] 既存のデータを検出しました。前回の続き（最新日: " + startDate + "）から同期を再開します。");
		} else {
			// データが1件もない完全な初回起動時は、従来通り 2019年1月1日 からスタートする
			startDate = LocalDate.of(2019, 1, 1);
			System.out.println("[⚙️同期システム] 既存データが見つからないため、初期設定として " + startDate + " から一括取得を開始します。");
		}

		// 2. 終了日は本日の日付
		LocalDate endDate = LocalDate.now();

		System.out.println("[⚙️同期システム] 【同期モード】 期間: " + startDate + " から " + endDate + " まで調査します。");

		// 3. 起点となる日から今日まで1日ずつループを回す
		LocalDate currentDate = startDate;
		while (!currentDate.isAfter(endDate)) {

			System.out.println("\n--------------------------------------------------");
			System.out.println("[⚙️同期システム] 調査中: " + currentDate);

			try {
				// 4. その日のレースID一覧をネットケイバから取得
				List<String> raceIds = keibaScraper.fetchRaceIdsOfDate(currentDate);

				if (raceIds == null || raceIds.isEmpty()) {
					System.out.println("[⚙️スキップ] " + currentDate + " はレース開催がありませんでした。");
				} else {
					System.out.println("[⚙️開催日発見] " + currentDate + " に " + raceIds.size() + " 件のレースを検出しました。");

					// 5. 検出したレースを1件ずつ個別に同期・保存処理を実行
					for (String raceId : raceIds) {
						try {
							System.out.println("[💾同期中] レースID: " + raceId);
							raceService.syncRaceResult(raceId);

							// サーバー負荷を軽減するため、レース取得ごとに3秒間スリープ
							Thread.sleep(3000);

						} catch (Exception e) {
							System.err.println("[❌エラー] レースID: " + raceId + " の同期に失敗しました: " + e.getMessage());
							// 失敗したレースIDは失敗ログテーブルに保存し、後からリトライバッチで回収できるようにする
							failureLogRepository.save(new ScrapingFailureLog(raceId, e.getMessage()));
						}
					}
				}
			} catch (Exception e) {
				String dateStr = currentDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
				System.err.println("[❌エラー] 日付別一覧取得失敗 (" + currentDate + "): " + e.getMessage());
				// 日付単位で失敗した場合もログに保存
				failureLogRepository.save(new ScrapingFailureLog(dateStr, e.getMessage()));
			}

			// 6. 日付が変わるごとの安全スリープ（サーバーへの優しさ）
			System.out.println("[⚙️同期システム] サーバー負荷軽減のため、3秒間待機して次の日に進みます...");
			Thread.sleep(3000);

			// 7. 次の日に進める
			currentDate = currentDate.plusDays(1);
		}

		System.out.println("\n[🏁同期システム] すべての同期処理が完了しました！");
	}
}