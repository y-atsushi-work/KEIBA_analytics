package com.keiba.analytics.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import com.keiba.analytics.entity.Horse;
import com.keiba.analytics.entity.Race;
import com.keiba.analytics.entity.RaceResult;

@Component
public class KeibaScraper {

	/**
	 * 指定された年月のカレンダーページから、開催日（レースリストのURL日付）を正しく抽出する
	 * 対象URL例: https://db.netkeiba.com/race/list/20200105/
	 */
	public List<LocalDate> fetchRaceDatesOfMonth(int year, int month) {
		List<LocalDate> dates = new ArrayList<>(); // 見つかった日付を格納するための空のリストを用意
		// 引数の年・月を 4桁・2桁（例: 2026, 6 -> "202606"）に整形し、カレンダーページのURLを作成
		String url = String.format("https://db.netkeiba.com/race/calendar/%04d%02d/", year, month);

		try {
			Thread.sleep(1000); // 相手サーバーに負荷をかけないよう、処理を1秒間一時停止する

			// Java 21対応：URI経由でURLに変換し、User-Agent（ブラウザのふり）を設定してストリームを開く
			java.net.URLConnection connection = java.net.URI.create(url).toURL().openConnection();
			connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)");

			// 指定したURLのHTMLを、文字化けを防ぐために「EUC-JP」という文字コードを指定して読み込む
			Document doc = Jsoup.parse(
					connection.getInputStream(),
					"EUC-JP",
					"https://db.netkeiba.com");

			// HTMLの中から「href属性が /race/list/ で始まる aタグ（リンク）」をすべて抽出する
			Elements links = doc.select("a[href*=/race/list/]");

			// 文字列（"20260628" など）を Javaの LocalDate 型に変換するための型形式を定義
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
			for (Element link : links) { // 抽出したリンクを1つずつループ処理
				String href = link.attr("href"); // リンクのURL部分（例: "/race/list/20200105/"）を取得

				String dateStr = href.replaceAll("[^0-9]", ""); // URLから数字以外の文字をすべて消し、数字だけに変換
				if (dateStr.length() == 8) { // もし文字数がピッタリ8桁（日付データ）であれば
					LocalDate date = LocalDate.parse(dateStr, formatter); // 文字列を日付型に変換
					if (!dates.contains(date)) { // リストにまだ同じ日付が入っていなければ（重複除去）
						dates.add(date); // リストに追加
					}
				}
			}

			// ログを出力して何日分の日付が取得できたかを表示
			System.out.println("[⚙️完了] " + year + "年" + month + "月のカレンダーから " + dates.size() + " 日分のレースリストURLを検出しました。");

		} catch (Exception e) { // エラー（通信失敗など）が発生した場合の処理
			System.err.println("[❌エラー] カレンダーパース失敗 (" + year + "/" + month + "): " + e.getMessage());
		}
		return dates; // 収集した日付リストを呼び出し元に返す
	}

	/**
	 * 【新設】指定された日付の全レースID（netkeiba_race_id）の一括一覧を取得する
	 * 起点URL: https://db.netkeiba.com/race/list/YYYYMMDD/
	 */
	public List<String> fetchRaceIdsOfDate(LocalDate date) {
		List<String> raceIds = new ArrayList<>(); // 見つかったレースIDを格納する空のリストを用意
		String dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd")); // 日付を "20260628" 形式の文字列に変換
		String url = "https://db.netkeiba.com/race/list/" + dateStr + "/"; // その日のレース一覧ページのURLを作成

		try {
			Thread.sleep(1000); // サーバー負荷軽減のために1秒待機
			// 指定したURLに擬似的なブラウザ情報（userAgent）をセットして通信を要求・実行する
			org.jsoup.Connection.Response response = Jsoup.connect(url)
					.userAgent(
							"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
					.execute();
			Document doc = response.charset("EUC-JP").parse(); // レスポンスをEUC-JPとしてHTMLパース（解析）する

			// HTMLから「href属性が /race/20 で始まる aタグ（各レース詳細へのリンク）」をすべて抽出する
			Elements links = doc.select("a[href^=/race/20]");

			for (Element link : links) { // 抽出したリンクを1つずつループ処理
				String href = link.attr("href"); // リンクのURL部分（例: "/race/202406040811/"）を取得
				// 正規表現を使い、URLの中からレースIDとなる数字の部分だけを切り出す
				String raceId = href.replaceAll(".*/race/([0-9]+).*", "$1");
				// レースIDが正しい桁数（12桁）であり、かつリストにまだ登録されていなければ
				if (raceId.length() == 12 && !raceIds.contains(raceId)) {
					raceIds.add(raceId); // リストに追加
				}
			}
		} catch (Exception e) { // 例外エラー時の処理
			System.err.println("[❌エラー] レース一覧取得失敗 (" + dateStr + "): " + e.getMessage());
		}
		return raceIds; // 収集したレースIDリストを返す
	}

	/**
	 * 【修正】アクセス修飾子を public にし、メソッド名を RaceService の要求に合わせました
	 */
	public Race parseRaceResultPagePublic(String raceId, LocalDate date) {
		String resultUrl = "https://db.netkeiba.com/race/" + raceId + "/"; // レース結果詳細ページのURLを作成
		System.out.println("[🕷️スクレイピング] レース詳細ページを解析中: " + resultUrl);

		try {
			// ブラウザ（MacのChrome）のふりをして、相手サイトにアクセスを実行
			org.jsoup.Connection.Response response = Jsoup.connect(resultUrl)
					.userAgent(
							"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
					.referrer("https://db.netkeiba.com/") // どこからアクセスしてきたかの情報（リファラ）を付与
					.execute();

			Document doc = response.charset("EUC-JP").parse(); // 文字コードをEUC-JPにしてHTMLを解析

			// データを詰め込むためのRaceエンティティオブジェクトを新しく作成
			Race race = new Race();
			race.setNetkeibaRaceId(raceId); // レースIDをセット
			race.setRaceDate(date); // 開催日付をセット

			// === 1. レース基本情報の詳細抽出 ===
			// HTMLからレース名が書かれている場所（クラス名など）を探して、最初の1つを取得
			Element raceNameEl = doc.selectFirst(".data_intro h1, .race_head_inner h1");
			// レース名が見つかればそのテキストを取得、なければ「不明なレース」とする（前後の余白もカット）
			String raceName = (raceNameEl != null) ? raceNameEl.text() : "不明なレース";
			race.setRaceName(raceName.trim());

			// レース番号（「11R」など）が書かれているテキストを取得
			String rNoText = doc.select(".race_num, .RaceNum").text();
			int raceNum = 11; // 変換失敗時のためのデフォルト値
			try {
				// 文字列から数字以外を排除して数値（11など）に変換
				raceNum = Integer.parseInt(rNoText.replaceAll("[^0-9]", ""));
			} catch (Exception e) {
			}
			race.setRaceNumber(raceNum); // レース番号をセット

			// 天気、馬場、距離などがまとめて書かれている文字列を取得
			String raceData01 = doc.select(".RaceData01, .data_intro p").text();

			// 文字列に「芝」や「ダ」が含まれているかでコース種別を判定
			String trackType = "不明";
			if (raceData01.contains("芝")) {
				trackType = "芝";
			} else if (raceData01.contains("ダ") || raceData01.contains("砂")) {
				trackType = "ダート";
			} else if (raceData01.contains("障")) {
				trackType = "障害";
			}
			race.setTrackType(trackType); // コース種別（芝・ダートなど）をセット

			int distance = 2000; // デフォルト値
			if (raceData01.contains("m")) { // もし文字列に「m（メートル）」があれば
				try {
					// 「m」の直前4文字を切り出し、数字だけを取り出して距離（2400など）として数値化
					String distStr = raceData01.substring(raceData01.indexOf("m") - 4, raceData01.indexOf("m"))
							.replaceAll("[^0-9]", "");
					distance = Integer.parseInt(distStr);
				} catch (Exception e) {
				}
			}
			race.setDistance(distance); // 距離をセット

			// 「天候:」または「天気 : 」の文字の後ろにある単語を切り出して天気を特定
			String weather = "晴";
			if (raceData01.contains("天候:")) {
				weather = raceData01.split("天候:")[1].trim().split(" ")[0];
			} else if (raceData01.contains("天気 : ")) {
				weather = raceData01.split("天気 : ")[1].trim().split(" ")[0];
			}
			race.setWeather(weather); // 天候をセット

			// 「馬場:」または「馬場 : 」の文字の後ろにある単語を切り出して馬場状態を特定
			String condition = "良";
			if (raceData01.contains("馬場:")) {
				condition = raceData01.split("馬場:")[1].trim().split(" ")[0];
			} else if (raceData01.contains("馬場 : ")) {
				condition = raceData01.split("馬場 : ")[1].trim().split(" ")[0];
			}
			race.setTrackCondition(condition); // 馬場状態（良、稍重など）をセット

			// レースIDの4〜5文字目の2桁（競馬場コード）を切り出す
			String locCode = raceId.substring(4, 6);
			// 競馬場コードを、実際の競馬場名に変換（Javaの新しいswitch構文）
			String location = switch (locCode) {
			// === 中央競馬 (JRA) ===
			case "01" -> "札幌";
			case "02" -> "函館";
			case "03" -> "福島";
			case "04" -> "新潟";
			case "05" -> "東京";
			case "06" -> "中山";
			case "07" -> "中京";
			case "08" -> "京都";
			case "09" -> "阪神";
			case "10" -> "小倉";
			// === 地方競馬 (NAR) ===
			case "30" -> "門別";
			case "35" -> "盛岡";
			case "36" -> "水沢";
			case "42" -> "船橋";
			case "43" -> "大井";
			case "44" -> "川崎";
			case "45" -> "浦和";
			case "46" -> "金沢";
			case "51" -> "笠松";
			case "54" -> "名古屋";
			case "65" -> "園田";
			case "66" -> "姫路";
			case "73" -> "高知";
			case "86" -> "佐賀";
			// 海外競馬やイレギュラー
			default -> "その他";
			};
			race.setLocation(location); // 競馬場名をセット
			// === 2. 各競走馬・着順データの詳細抽出 ===
			List<RaceResult> results = new ArrayList<>(); // 馬ごとの着順結果を格納するリストを用意

			// レース結果が載っているテーブル（表）の「行（trタグ）」をすべて取得
			Elements rows = doc.select("table.race_table_01 tbody tr");
			if (rows.isEmpty()) { // もし上記で見つからなければ、別のID名を持つテーブルから取得
				rows = doc.select("table#All_Result_Table tbody tr");
			}
			System.out.println("[🕷️スクレイピング] -> 全 " + rows.size() + " 行のデータを解析します。");

			for (Element row : rows) { // 表の1行（＝馬1頭ずつのデータ）ごとにループ処理
				Elements tds = row.select("td"); // 行の中にある「マス（tdタグ）」をすべて取得

				if (tds.size() < 10) { // マスの数が10未満なら、ヘッダー行や不完全なデータとみなしてスキップ
					continue;
				}

				String rowOrderStr = tds.get(0).text().trim(); // 1番目のマスから「着順」を取得
				String wakuStr = tds.get(1).text().trim(); // 2番目のマスから「枠番」を取得
				String umabanStr = tds.get(2).text().trim(); // 3番目のマスから「馬番」を取得

				Element horseLink = tds.get(3).selectFirst("a"); // 4番目のマス（馬名）にあるリンクタグを取得
				if (horseLink == null) { // リンクがなければデータ不正としてスキップ
					continue;
				}

				String horseName = horseLink.text().trim(); // 馬の名前を取得
				String horseHref = horseLink.attr("href"); // 馬の個別ページのURLを取得
				// URL（例: /horse/2021104567/）から馬の固有ID（英数字）だけを正規表現で抽出
				String horseId = horseHref.replaceAll(".*/horse/([0-9a-zA-Z]+).*", "$1");

				String seireiStr = tds.get(4).text().trim(); // 5番目のマスから「性齢（例：牡3）」を取得
				String sex = "不明";
				Integer age = null;
				if (seireiStr.length() >= 2) {
					sex = seireiStr.substring(0, 1); // 最初の1文字（「牡」「牝」「騸」など）を性にセット
					try {
						// 2文字目以降の数字を取り出して年齢に変換
						age = Integer.parseInt(seireiStr.substring(1).replaceAll("[^0-9]", ""));
					} catch (Exception e) {
					}
				}

				String kinryoStr = tds.get(5).text().trim(); // 6番目のマスから「斤量（背負う重量）」を取得
				Double weight = 55.0; // デフォルト値
				try {
					weight = Double.parseDouble(kinryoStr); // 小数点を含む数値（例: 56.5）に変換
				} catch (Exception e) {
				}

				String jockeyName = tds.get(6).text().trim(); // 7番目のマスから「騎手名」を取得
				String raceTime = tds.get(7).text().trim(); // 8番目のマスから「走破タイム」を取得

				// 19番目のマス（インデックスは0から始まるので18）から「馬体重（例: 480(+2)）」の文字列を取得
				String rawWeightStr = tds.get(18).text().trim();
				Integer horseWeight = null;
				Integer weightChange = null;

				// 体重データが空でなく、かつ「計不（計測不能）」でもなければ解析
				if (!rawWeightStr.isEmpty() && !rawWeightStr.contains("計不")) {
					try {
						if (rawWeightStr.contains("(")) { // カッコ「(」が含まれている場合（増減があるとき）
							String[] parts = rawWeightStr.split("\\("); // カッコで前後を2つに分割
							horseWeight = Integer.parseInt(parts[0].trim()); // 前半部分を馬体重（480）とする
							String changeStr = parts[1].replaceAll("\\)", "").trim(); // 後半のカッコ「)」を消す
							weightChange = Integer.parseInt(changeStr); // 残った数字を体重増減（+2）とする
						} else { // カッコがない場合（増減なし、または未記載）
							horseWeight = Integer.parseInt(rawWeightStr.replaceAll("[^0-9]", "")); // 数字だけを取り出す
							weightChange = 0; // 増減は0とする
						}
					} catch (Exception e) {
					}
				}

				// --- 抽出したデータを各エンティティにセットする ---
				Horse horse = new Horse(); // Horseオブジェクトを新しく作成
				horse.setNetkeibaHorseId(horseId);
				horse.setHorseName(horseName);
				horse.setSex(sex);
				horse.setAge(age);

				RaceResult result = new RaceResult(); // RaceResult（着順結果）オブジェクトを新しく作成
				result.setRace(race); // この結果がどのレースのものかを紐付け
				result.setHorse(horse); // どの馬のものかを紐付け
				result.setJockeyName(jockeyName);
				result.setRaceTime(raceTime.isEmpty() ? "-" : raceTime); // タイムが空ならハイフンにする
				result.setWeight(weight);
				result.setHorseWeight(horseWeight);
				result.setWeightChange(weightChange);

				try {
					result.setRowOrder(Integer.parseInt(rowOrderStr)); // 着順を数値に変換してセット
				} catch (Exception e) {
					result.setRowOrder(99); // 「取消」や「失格」などで数値にできない場合は 99（仮）にする
				}

				try {
					if (!wakuStr.isEmpty()) { // 枠番の文字が入っていれば
						result.setBracketNumber(Integer.parseInt(wakuStr)); // そのまま数値に変換
					} else { // 空欄の場合、HTMLタグの背景色クラス名（例：waku6）から枠番を推測して抽出する
						String wakuClass = tds.get(1).className();
						String wakuNum = wakuClass.replaceAll("[^0-9]", "");
						if (!wakuNum.isEmpty()) {
							result.setBracketNumber(Integer.parseInt(wakuNum));
						}
					}
				} catch (Exception e) {
				}

				try {
					result.setHorseNumber(Integer.parseInt(umabanStr)); // 馬番を数値に変換してセット
				} catch (Exception e) {
				}

				results.add(result); // 完成した1頭分の結果をリストに追加
			}

			race.setRaceResults(results); // 全頭分のリストを、親であるレースオブジェクトにセット
			return race; // すべての情報が詰まったレースオブジェクトを呼び出し元に返す

		} catch (IOException e) { // 通信やパースに致命的なエラーが起きた場合の処理
			System.err.println("[❌エラー] レース詳細の解析に失敗しました: " + e.getMessage());
			return null; // 失敗時は null を返す
		}
	}

}