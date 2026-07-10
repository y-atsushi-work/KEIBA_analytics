package com.keiba.analytics.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.keiba.analytics.repository.RaceRepository;

@Component
public class RaceSyncManager implements CommandLineRunner {

	private final RaceRepository raceRepository;
	private final KeibaScraper keibaScraper;
	private final RaceService raceService;

	public RaceSyncManager(RaceRepository raceRepository, KeibaScraper keibaScraper, RaceService raceService) {
		this.raceRepository = raceRepository;
		this.keibaScraper = keibaScraper;
		this.raceService = raceService;
	}

	@Override
	public void run(String... args) throws Exception {
		System.out.println("[⚙️同期システム] アプリ起動時のデータ同期チェックを開始します...");

		// テスト用の期間設定（2020年1月1日 〜 2020年1月31日までの総当たり）
		LocalDate startDate = LocalDate.of(2020, 1, 1);
		LocalDate endDate = LocalDate.of(2020, 1, 6);

		System.out.println("[⚙️同期システム] 【総当たりモード】" + startDate + " から " + endDate + " まで1日ずつ調査します。");

		// 開始日から終了日まで、1日ずつ実直に進めるループ
		LocalDate currentDate = startDate;
		while (!currentDate.isAfter(endDate)) {

			System.out.println("\n--------------------------------------------------");
			System.out.println("[⚙️同期システム] 調査中: " + currentDate);

			// 1. その日のレースID一覧を取得してみる
			List<String> raceIds = keibaScraper.fetchRaceIdsOfDate(currentDate);

			// 2. 空のページ（レースIDが0件）かどうかの判定
			if (raceIds == null || raceIds.isEmpty()) {
				// 何もないページなら「開催なし」と判定して楽にスキップ！
				System.out.println("[⚙️スキップ] " + currentDate + " はレース開催がありませんでした。");
			} else {
				// レースIDが存在する場合（開催日）はインポートを実行
				System.out.println("[⚙️開催日発見] " + currentDate + " に " + raceIds.size() + " 件のレースを検出しました。");

				for (String raceId : raceIds) {
					try {
						System.out.println("[💾同期中] レースID: " + raceId);
						raceService.syncRaceResult(raceId);

						// レース詳細の取得間隔（念のためここも1〜2秒あけるとより安全です）
						Thread.sleep(1500);

						//テストのため1回で終了するように記述。あとで消す
						break;

					} catch (Exception e) {
						System.err.println("[❌エラー] レースID: " + raceId + " の同期に失敗しました。");
						e.printStackTrace();
					}
				}
			}

			// 💡 ご提案の通り、日付を変えるタイミングで安全のために「5秒間隔」のウェイトを入れる
			System.out.println("[⚙️同期システム] サーバー負荷軽減のため、5秒間待機して次の日に進みます...");
			Thread.sleep(5000);

			// 次の日へ進める
			currentDate = currentDate.plusDays(1);

		}

		System.out.println("\n[⚙️同期システム] 指定期間の総当たりチェックがすべて完了しました！");
	}
}