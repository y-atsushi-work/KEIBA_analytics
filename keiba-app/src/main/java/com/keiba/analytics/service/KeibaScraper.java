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
	 * 【新設】指定された年月の開催日（LocalDate）の一覧を取得する
	 * 起点URL: https://db.netkeiba.com/race/calendar/YYYYMM/
	 */
//	public List<LocalDate> fetchRaceDatesOfMonth(int year, int month) {
//		List<LocalDate> dates = new ArrayList<>();
//		String url = String.format("https://db.netkeiba.com/race/calendar/%04d%02d/", year, month);
//		
//		try {
//			Thread.sleep(1000); // サーバー負荷軽減用のウェイト
//			org.jsoup.Connection.Response response = Jsoup.connect(url)
//					.userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
//					.execute();
//			Document doc = response.charset("EUC-JP").parse();
//			
//			// カレンダー内の日付リンク（例: /race/list/20260628/）を抽出
//			Elements links = doc.select("a[href*=/race/list/]");
//			
//			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
//			for (Element link : links) {
//				String href = link.attr("href"); // 例: "/race/list/20260628/"
//				String dateStr = href.replaceAll("[^0-9]", ""); // "20260628"
//				if (dateStr.length() == 8) {
//					LocalDate date = LocalDate.parse(dateStr, formatter);
//					if (!dates.contains(date)) {
//						dates.add(date);
//					}
//				}
//			}
//		} catch (Exception e) {
//			System.err.println("[❌エラー] カレンダー取得失敗 (" + year + "/" + month + "): " + e.getMessage());
//		}
//		return dates;
//	}
	/**
	 * 指定された年月のカレンダーページから、開催日（レースリストのURL日付）を正しく抽出する
	 * 対象URL例: https://db.netkeiba.com/race/list/20200105/
	 */
	public List<LocalDate> fetchRaceDatesOfMonth(int year, int month) {
		List<LocalDate> dates = new ArrayList<>();
		String url = String.format("https://db.netkeiba.com/race/calendar/%04d%02d/", year, month);
		
		try {
			Thread.sleep(1000); // サーバー負荷軽減
			
			// 文字化けを完全に防ぐため、InputStreamから明示的にEUC-JPでパースする
			Document doc = Jsoup.parse(
				new java.net.URL(url).openStream(), 
				"EUC-JP", 
				"https://db.netkeiba.com"
			);
			
			// あなたの仰る通り、/race/list/ で始まるリンクを正確に取得
			Elements links = doc.select("a[href^=/race/list/]");
			
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
			for (Element link : links) {
				String href = link.attr("href"); // 例: "/race/list/20200105/"
				
				// URLから日付の8桁数字だけを抽出
				String dateStr = href.replaceAll("[^0-9]", ""); 
				if (dateStr.length() == 8) {
					LocalDate date = LocalDate.parse(dateStr, formatter);
					if (!dates.contains(date)) {
						dates.add(date);
					}
				}
			}
			
			System.out.println("[⚙️完了] " + year + "年" + month + "月のカレンダーから " + dates.size() + " 日分のレースリストURLを検出しました。");
			
		} catch (Exception e) {
			System.err.println("[❌エラー] カレンダーパース失敗 (" + year + "/" + month + "): " + e.getMessage());
		}
		return dates;
	}

	/**
	 * 【新設】指定された日付の全レースID（netkeiba_race_id）の一括一覧を取得する
	 * 起点URL: https://db.netkeiba.com/race/list/YYYYMMDD/
	 */
	public List<String> fetchRaceIdsOfDate(LocalDate date) {
		List<String> raceIds = new ArrayList<>();
		String dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		String url = "https://db.netkeiba.com/race/list/" + dateStr + "/";
		
		try {
			Thread.sleep(1000); // サーバー負荷軽減用のウェイト
			org.jsoup.Connection.Response response = Jsoup.connect(url)
					.userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
					.execute();
			Document doc = response.charset("EUC-JP").parse();
			
			// レース結果詳細へのリンク（例: /race/202406040811/）を抽出
			Elements links = doc.select("a[href^=/race/20]");
			
			for (Element link : links) {
				String href = link.attr("href"); // 例: "/race/202406040811/"
				String raceId = href.replaceAll(".*/race/([0-9]+).*", "$1");
				if (raceId.length() == 12 && !raceIds.contains(raceId)) {
					raceIds.add(raceId);
				}
			}
		} catch (Exception e) {
			System.err.println("[❌エラー] レース一覧取得失敗 (" + dateStr + "): " + e.getMessage());
		}
		return raceIds;
	}
//不要処理なので、後々削除する
//	public List<Race> fetchRacesByDate(LocalDate date) {
//		List<Race> fetchedRaces = new ArrayList<>();
//
//		// テスト用ハック：2024年有馬記念のレースID（202406040811）を直接狙い撃ち
//		String targetRaceId = "202406040811";
//		System.out.println("[🕷️テスト] 有馬記念のレースIDを直接詳細解析します: " + targetRaceId);
//
//		Race race = parseRaceResultPagePublic(targetRaceId, date);
//		if (race != null) {
//			fetchedRaces.add(race);
//		}
//
//		return fetchedRaces;
//	}

	/**
	 * 【修正】アクセス修飾子を public にし、メソッド名を RaceService の要求に合わせました
	 */
	public Race parseRaceResultPagePublic(String raceId, LocalDate date) {
		String resultUrl = "https://db.netkeiba.com/race/" + raceId + "/";
		System.out.println("[🕷️スクレイピング] レース詳細ページを解析中: " + resultUrl);

		try {
			// 1. .execute() で一度だけ通信し、レスポンスをまるごと受け取る
			org.jsoup.Connection.Response response = Jsoup.connect(resultUrl)
					.userAgent(
							"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
					.referrer("https://db.netkeiba.com/")
					.execute();

			// 2. 受け取ったレスポンスの文字コードを「EUC-JP」に上書きしてパースする
			Document doc = response.charset("EUC-JP").parse();

			Race race = new Race();
			race.setNetkeibaRaceId(raceId);
			race.setRaceDate(date);

			// === 1. レース基本情報の詳細抽出 ===
			Element raceNameEl = doc.selectFirst(".data_intro h1, .race_head_inner h1");
			String raceName = (raceNameEl != null) ? raceNameEl.text() : "不明なレース";
			race.setRaceName(raceName.trim());

			// レース番号（例: "11R" から数字の 11 を抽出）
			String rNoText = doc.select(".race_num, .RaceNum").text();
			int raceNum = 11; // デフォルト
			try {
				raceNum = Integer.parseInt(rNoText.replaceAll("[^0-9]", ""));
			} catch (Exception e) {
			}
			race.setRaceNumber(raceNum);

			// 天気・馬場状態・距離
			String raceData01 = doc.select(".RaceData01, .data_intro p").text();

			String trackType = "不明";
			if (raceData01.contains("芝")) {
				trackType = "芝";
			} else if (raceData01.contains("ダ") || raceData01.contains("砂")) {
				trackType = "ダート";
			} else if (raceData01.contains("障")) {
				trackType = "障害";
			}
			race.setTrackType(trackType);

			int distance = 2000;
			if (raceData01.contains("m")) {
				try {
					String distStr = raceData01.substring(raceData01.indexOf("m") - 4, raceData01.indexOf("m"))
							.replaceAll("[^0-9]", "");
					distance = Integer.parseInt(distStr);
				} catch (Exception e) {
				}
			}
			race.setDistance(distance);

			String weather = "晴";
			if (raceData01.contains("天候:")) {
				weather = raceData01.split("天候:")[1].trim().split(" ")[0];
			} else if (raceData01.contains("天気 : ")) {
				weather = raceData01.split("天気 : ")[1].trim().split(" ")[0];
			}
			race.setWeather(weather);

			String condition = "良";
			if (raceData01.contains("馬場:")) {
				condition = raceData01.split("馬場:")[1].trim().split(" ")[0];
			} else if (raceData01.contains("馬場 : ")) {
				condition = raceData01.split("馬場 : ")[1].trim().split(" ")[0];
			}
			race.setTrackCondition(condition);

			// 競馬場名
			String locCode = raceId.substring(4, 6);
			String location = switch (locCode) {
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
			default -> "その他";
			};
			race.setLocation(location);

			// === 2. 各競走馬・着順データの詳細抽出 ===
			List<RaceResult> results = new ArrayList<>();

			Elements rows = doc.select("table.race_table_01 tbody tr");
			if (rows.isEmpty()) {
				rows = doc.select("table#All_Result_Table tbody tr");
			}
			System.out.println("[🕷️スクレイピング] -> 全 " + rows.size() + " 行のデータを解析します。");

			for (Element row : rows) {
				Elements tds = row.select("td");

				if (tds.size() < 10) {
					continue;
				}

				String rowOrderStr = tds.get(0).text().trim();
				String wakuStr = tds.get(1).text().trim();
				String umabanStr = tds.get(2).text().trim();

				Element horseLink = tds.get(3).selectFirst("a");
				if (horseLink == null) {
					continue;
				}

				String horseName = horseLink.text().trim();
				String horseHref = horseLink.attr("href");
				String horseId = horseHref.replaceAll(".*/horse/([0-9a-zA-Z]+).*", "$1");

				String seireiStr = tds.get(4).text().trim();
				String sex = "不明";
				Integer age = null;
				if (seireiStr.length() >= 2) {
					sex = seireiStr.substring(0, 1);
					try {
						age = Integer.parseInt(seireiStr.substring(1).replaceAll("[^0-9]", ""));
					} catch (Exception e) {
					}
				}

				String kinryoStr = tds.get(5).text().trim();
				Double weight = 55.0;
				try {
					weight = Double.parseDouble(kinryoStr);
				} catch (Exception e) {
				}

				String jockeyName = tds.get(6).text().trim();
				String raceTime = tds.get(7).text().trim();

				// 馬体重と増減の解析（19列目：インデックス18）
				String rawWeightStr = tds.get(18).text().trim();
				Integer horseWeight = null;
				Integer weightChange = null;

				if (!rawWeightStr.isEmpty() && !rawWeightStr.contains("計不")) {
					try {
						if (rawWeightStr.contains("(")) {
							String[] parts = rawWeightStr.split("\\(");
							horseWeight = Integer.parseInt(parts[0].trim());
							String changeStr = parts[1].replaceAll("\\)", "").trim();
							weightChange = Integer.parseInt(changeStr);
						} else {
							horseWeight = Integer.parseInt(rawWeightStr.replaceAll("[^0-9]", ""));
							weightChange = 0;
						}
					} catch (Exception e) {
					}
				}

				Horse horse = new Horse();
				horse.setNetkeibaHorseId(horseId);
				horse.setHorseName(horseName);
				horse.setSex(sex);
				horse.setAge(age);

				RaceResult result = new RaceResult();
				result.setRace(race);
				result.setHorse(horse);
				result.setJockeyName(jockeyName);
				result.setRaceTime(raceTime.isEmpty() ? "-" : raceTime);
				result.setWeight(weight);
				result.setHorseWeight(horseWeight);
				result.setWeightChange(weightChange);

				try {
					result.setRowOrder(Integer.parseInt(rowOrderStr));
				} catch (Exception e) {
					result.setRowOrder(99);
				}

				try {
					if (!wakuStr.isEmpty()) {
						result.setBracketNumber(Integer.parseInt(wakuStr));
					} else {
						String wakuClass = tds.get(1).className();
						String wakuNum = wakuClass.replaceAll("[^0-9]", "");
						if (!wakuNum.isEmpty()) {
							result.setBracketNumber(Integer.parseInt(wakuNum));
						}
					}
				} catch (Exception e) {
				}

				try {
					result.setHorseNumber(Integer.parseInt(umabanStr));
				} catch (Exception e) {
				}

				results.add(result);
			}

			race.setRaceResults(results);
			return race;

		} catch (IOException e) {
			System.err.println("[❌エラー] レース詳細の解析に失敗しました: " + e.getMessage());
			return null;
		}
	}
}