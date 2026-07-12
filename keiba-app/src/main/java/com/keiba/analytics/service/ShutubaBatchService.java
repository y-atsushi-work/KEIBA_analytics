package com.keiba.analytics.service; // このクラスが所属するパッケージ（フォルダ構造）を定義

import java.time.LocalDate; // Javaで日付（年月日）を便利に扱うためのクラスをインポート
import java.time.format.DateTimeFormatter; // 日付を特定の文字形式（例: yyyyMMdd）に変換するためのクラスをインポート
import java.util.ArrayList; // 可変長のリスト（動的配列）を扱うためのクラスをインポート
import java.util.List; // 複数のデータをまとめて管理するコレクションインターフェースをインポート
import java.util.regex.Matcher; // 正規表現の判定結果を扱うためのクラス
import java.util.regex.Pattern; // 正規表現の検索パターンを定義するためのクラス

import org.jsoup.Jsoup; // Webスクレイピングライブラリ「Jsoup」の接続管理クラスをインポート
import org.jsoup.nodes.Document; // 取得したHTML全体を解析・保持するためのクラスをインポート
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service; // このクラスをSpring Bootの「ビジネスロジック担当部品」として登録するアノテーションをインポート

@Service // Spring BootにこのクラスをServiceとして管理させ、Controller等へ自動注入できるようにする
public class ShutubaBatchService {
	@Autowired
	private ShutubaService shutubaService;

	// 1. 【自動実行】30分毎のトリガー（既存を置き換え）
	@Scheduled(cron = "0 0/30 9-16 * * *")
	public void scheduledRun() {
		System.out.println("[⏰定時実行] レース当日のデータ更新を開始します。");
		// 当日のレースIDのみを特定して更新するロジックを呼ぶ
		updateCurrentDayRaces();
	}

	// 2. 【任意実行】手動呼び出し用（新規追加）
	public void manualRunFullCrawl() {
		System.out.println("[⚡手動実行] 7日分の一括更新を開始します。");
		executeWeeklyShutubaCrawl();
	}

	// 3. 【更新ロジック】最終更新〜現在までの差分更新（新規追加）
	public void updateCurrentDayRaces() {
		// TODO: DBから「今日」のレースIDを取得し、必要なものだけ再クロールする処理
		// または、既存のロジックで「今日」のURLのみを対象にループする
	}

	/**
	 * 💡 【メイン処理】今日から1週間先までの出馬表（レースID）を自動巡回して一括取得する
	 */
	public void executeWeeklyShutubaCrawl() {
		LocalDate today = LocalDate.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

		System.out.println("[🚀バッチ開始] 出走予定チェックを起動。");

		for (int i = 0; i < 2; i++) {
			LocalDate targetDate = today.plusDays(i);
			String dateStr = targetDate.format(formatter);
			String topUrl = "https://race.netkeiba.com/top/race_list_sub.html?kaisai_date=" + dateStr;

			try {
				Thread.sleep(1500); // サーバー負荷軽減

				// 接続実行
				Document doc = Jsoup.connect(topUrl)
						.userAgent(
								"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
						.header("Accept-Language", "ja,en-US;q=0.9,en;q=0.8")
						.header("Referer", "https://race.netkeiba.com/")
						.ignoreHttpErrors(false)
						.get();

				String htmlContent = doc.html();
				Pattern raceIdPattern = Pattern.compile("(?i)race_id[^0-9]{1,5}([0-9]{12})");
				Matcher matcher = raceIdPattern.matcher(htmlContent);

				List<String> raceIds = new ArrayList<>();
				while (matcher.find()) {
					String raceId = matcher.group(1);
					if (!raceIds.contains(raceId)) { // 重複防止
						raceIds.add(raceId);
					}
				}

				if (raceIds.isEmpty()) {
					System.out.println("[ℹ️スキップ] " + targetDate + " は出走予定のレースIDが見つかりません。");
					continue;
				}
				// 取得できたIDをコンソールに出力
				System.out.println(" ➡️ " + targetDate + " の抽出成功したレースID: " + raceIds);
				System.out.println(" ➡️ " + targetDate + " の総レース数: " + raceIds.size() + " 件");

				// 詳細取得処理を呼び出し
				for (String raceId : raceIds) {
					try {
						Thread.sleep(1000); // 1秒待機
						shutubaService.scrapeAndSaveShutuba(raceId);
					} catch (Exception e) {
						System.err.println("[❌詳細取得エラー] ID: " + raceId + " - " + e.getMessage());
					}
				}

			} catch (Exception e) {
				System.err.println("[❌エラー] " + dateStr + " の取得失敗: " + e.getMessage());
			}
		}
		System.out.println("[🏁バッチ完了] 出走予定チェックが終了しました。");
	}

	/**
	 * 💡 【ヘルパーメソッド】URL文字列の中から race_id= に続く12桁の数字を抜き出す
	 */
	private String extractRaceId(String href) { // URL文字列を引数に受け取り、解析したレースIDを返すプライベートメソッドを定義
		Pattern pattern = Pattern.compile("race_id=([0-9]{12})"); // 「race_id= の後ろに数字が12桁続く」という正規表現の検索パターンを定義
		Matcher matcher = pattern.matcher(href); // 引数で渡されたURL文字列に対して、上記の正規表現パターンが一致するか検証するチェッカーを作成
		if (matcher.find()) { // もしURL文字列の中にパターンに合致する部分（12桁の数字）が見つかった場合
			return matcher.group(1); // 正規表現のカッコ()で囲んだ最初の部分（＝12桁の数字そのもの）だけを切り出して呼び出し元に返す
		} // if文の終了
		return null; // パターンがどこにも見つからなかった（不正なURLリンクだった）場合は、何も返さず null を返す
	} // extractRaceId メソッドの終了
} // クラス全体の終了