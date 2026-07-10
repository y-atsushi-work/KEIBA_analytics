package com.keiba.analytics.service; // このクラスが所属するパッケージ（フォルダ構造）を定義

import java.io.IOException; // ページの取得失敗などの通信エラーを扱うための例外クラスをインポート
import java.time.LocalDate; // Javaで日付（年月日）を便利に扱うためのクラスをインポート
import java.time.format.DateTimeFormatter; // 日付を特定の文字形式（例: yyyyMMdd）に変換するためのクラスをインポート
import java.util.ArrayList; // 可変長のリスト（動的配列）を扱うためのクラスをインポート
import java.util.List; // 複数のデータをまとめて管理するコレクションインターフェースをインポート
import java.util.regex.Matcher; // 正規表現の判定結果を扱うためのクラス
import java.util.regex.Pattern; // 正規表現の検索パターンを定義するためのクラス

import org.jsoup.Jsoup; // Webスクレイピングライブラリ「Jsoup」の接続管理クラスをインポート
import org.jsoup.nodes.Document; // 取得したHTML全体を解析・保持するためのクラスをインポート
import org.springframework.scheduling.annotation.Scheduled; // 定期実行（タイマー機能）を設定するためのアノテーションをインポート
import org.springframework.stereotype.Service; // このクラスをSpring Bootの「ビジネスロジック担当部品」として登録するアノテーションをインポート

@Service // Spring BootにこのクラスをServiceとして管理させ、Controller等へ自動注入できるようにする
public class KeibaShutubaBatchService { // 競馬の出馬表（出走予定）を自動取得するバッチ処理用のサービスサクラスを定義

	/**
	 * 💡 【1時間毎の自動実行】毎時0分0秒に Spring Boot が自動でこのメソッドを起動します
	 * ※ テスト時は必要に応じて「@Scheduled(fixedRate = 10000, initialDelay = 0)」などに書き換えてください。
	 */
	@Scheduled(cron = "0 0 * * * *") // 毎時0分0秒（1:00, 2:00...）に自動で作動させるためのスケジュール（Cron）を設定
	public void executeHourlyScheduled() { // 定期実行タイマーの入り口となるメソッドを定義
		System.out.println("[⏰定期実行] 毎時の自動チェック処理を起動します。"); // タイマーが作動したことをターミナルにログ出力
		executeWeeklyShutubaCrawl(); // 下に書かれている実際のスクレイピングメイン処理を呼び出す
	} // メソッドの終了

	/**
	 * 💡 【メイン処理】今日から1週間先までの出馬表（レースID）を自動巡回して一括取得する
	 */
	public void executeWeeklyShutubaCrawl() {
		LocalDate today = LocalDate.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

		System.out.println("[🚀バッチ開始] 出走予定チェックを起動。");

		for (int i = 0; i < 7; i++) {
			LocalDate targetDate = today.plusDays(i);
			String dateStr = targetDate.format(formatter);

			// 💡 1. URLのパスを「race_list.html」に設定
			String topUrl = "https://race.netkeiba.com/top/race_list.html?kaisai_date=" + dateStr;

			try {
				Thread.sleep(1500); // 相手サーバーの負荷軽減のためにウェイトを1.5秒空ける

				// 💡 2. 接続設定を強化（403エラーを回避するためのヘッダー追加）
				Document doc = Jsoup.connect(topUrl)
						.userAgent(
								"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
						.header("Accept-Language", "ja,en-US;q=0.9,en;q=0.8")
						.header("Referer", "https://race.netkeiba.com/")
						.ignoreHttpErrors(false) // エラーが起きたらキャッチへ飛ばす
						.get();

				// デバッグ用：取得したHTMLの文字数をログに出す
				System.out.println("[📄データ取得] " + dateStr + " のHTML文字数: " + doc.html().length());

				// 💡 3. 【仕様修正】JavaScript動的生成対策
				// aタグ単体を探すのをやめ、HTMLソース内のJavaScriptデータ（JSON等）に含まれる12桁のレースIDを一網打尽にする
				String htmlContent = doc.html();
				
				// 「race_id」という文字の後、数字以外の文字（= や ":" など）を挟んで「12桁の数字」が続くパターンを定義
				Pattern raceIdPattern = Pattern.compile("race_id[^0-9]{1,5}([0-9]{12})");
				Matcher matcher = raceIdPattern.matcher(htmlContent);

				List<String> raceIds = new ArrayList<>();
				while (matcher.find()) {
					String raceId = matcher.group(1); // カッコ()で囲んだ12桁の数字部分だけを抽出
					if (!raceIds.contains(raceId)) { // 重複を防ぐ
						raceIds.add(raceId);
					}
				}

				if (raceIds.isEmpty()) {
					// 完全にデータが存在しない日の場合はスキップ
					System.out.println("[ℹ️スキップ] " + targetDate + " は出走予定のレースIDが見つかりません。");
					continue;
				}

				// 💡 デバッグ用：取得できたIDの一覧と総数をコンソールに表示
				System.out.println(" ➡️ " + targetDate + " の抽出成功したレースID: " + raceIds);
				System.out.println(" ➡️ " + targetDate + " の総レース数: " + raceIds.size() + " 件");

			} catch (org.jsoup.HttpStatusException e) {
				// 403や404などのHTTPステータスエラーをここで確実にキャッチする
				System.err.println("[❌HTTPエラー] ステータスコード: " + e.getStatusCode() + " / URL: " + e.getUrl());
			} catch (IOException e) {
				System.err.println("[❌通信エラー] " + dateStr + ": " + e.getMessage());
			} catch (InterruptedException e) {
				System.err.println("[❌割り込みエラー]: " + e.getMessage());
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