package com.keiba.analytics.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import com.keiba.analytics.entity.Horse;
import com.keiba.analytics.entity.Race;
import com.keiba.analytics.entity.RaceResult;
import com.keiba.analytics.entity.ScrapingFailureLog;
import com.keiba.analytics.repository.ScrapingFailureLogRepository;
import com.keiba.analytics.util.KeibaUtils;

@Component
public class KeibaScraper {

	// 失敗ログをデータベースに保存するためのリポジトリ
	private final ScrapingFailureLogRepository failureLogRepository;

	public KeibaScraper(ScrapingFailureLogRepository failureLogRepository) {
		this.failureLogRepository = failureLogRepository;
	}

	/**
	 * 【月別カレンダー取得】
	 */
	public List<LocalDate> fetchRaceDatesOfMonth(int year, int month) {
		List<LocalDate> dates = new ArrayList<>();
		String url = String.format("https://db.netkeiba.com/race/calendar/%04d%02d/", year, month);

		try {
			KeibaUtils.randomSleep(); 

			Document doc = executeWithRetry(() -> {
				try {
					java.net.URLConnection connection = java.net.URI.create(url).toURL().openConnection();
					connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)");

					return Jsoup.parse(
							connection.getInputStream(),
							"EUC-JP",
							"https://db.netkeiba.com");
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}, 3);

			Elements links = doc.select("a[href*=/race/list/]");
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
			for (Element link : links) {
				String href = link.attr("href");
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
			// 失敗ログの記録（ターゲットIDとして年月を文字列で保存）
			saveFailureLog(year + "年" + month + "月", e.getMessage());
		}
		return dates;
	}

	/**
	 * 【日付別レースID一覧取得】
	 */
	public List<String> fetchRaceIdsOfDate(LocalDate date) {
		List<String> raceIds = new ArrayList<>();
		String dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		String url = "https://db.netkeiba.com/race/list/" + dateStr + "/";

		try {
			KeibaUtils.randomSleep(); 

			Document doc = executeWithRetry(() -> {
				try {
					org.jsoup.Connection.Response response = Jsoup.connect(url)
							.userAgent(
									"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
							.execute();
					return response.charset("EUC-JP").parse();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}, 3);

			Elements links = doc.select("a[href^=/race/20]");

			for (Element link : links) {
				String href = link.attr("href");
				String raceId = href.replaceAll(".*/race/([0-9]+).*", "$1");
				if (raceId.length() == 12 && !raceIds.contains(raceId)) {
					raceIds.add(raceId);
				}
			}
		} catch (Exception e) {
			System.err.println("[❌エラー] レース一覧取得失敗 (" + dateStr + "): " + e.getMessage());
			// 失敗ログの記録（対象日を保存）
			saveFailureLog(dateStr, e.getMessage());
		}
		return raceIds;
	}

	/**
	 * 【レース結果詳細解析】
	 */
	public Race parseRaceResultPagePublic(String raceId, LocalDate date) {
		String resultUrl = "https://db.netkeiba.com/race/" + raceId + "/";
		System.out.println("[🕷️スクレイピング] レース詳細ページを解析中: " + resultUrl);

		try {
			Document doc = executeWithRetry(() -> {
				try {
					org.jsoup.Connection.Response response = Jsoup.connect(resultUrl)
							.userAgent(
									"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
							.referrer("https://db.netkeiba.com/")
							.execute();

					return response.charset("EUC-JP").parse();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}, 3);

			Race race = new Race();
			race.setNetkeibaRaceId(raceId);
			race.setRaceDate(date);

			Element raceNameEl = doc.selectFirst(".data_intro h1, .race_head_inner h1");
			String raceName = (raceNameEl != null) ? raceNameEl.text() : "不明なレース";
			race.setRaceName(raceName.trim());

			String rNoText = doc.select(".race_num, .RaceNum").text();
			int raceNum = 11;
			try {
				raceNum = Integer.parseInt(rNoText.replaceAll("[^0-9]", ""));
			} catch (Exception e) {
			}
			race.setRaceNumber(raceNum);

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

			String locCode = raceId.substring(4, 6);
			String location = KeibaUtils.convertLocationCode(locCode);
			race.setLocation(location);

			List<RaceResult> results = new ArrayList<>();
			Elements rows = doc.select("table.race_table_01 tbody tr");
			if (rows.isEmpty()) {
				rows = doc.select("table#All_Result_Table tbody tr");
			}

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
				result.setSexAge(seireiStr);
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

				try {
					String last3fStr = tds.get(15).text().trim();
					if (last3fStr != null) {
						result.setLast3fTime(Double.parseDouble(last3fStr));
					} else {
						result.setLast3fTime(null);
					}
				} catch (Exception e) {
					result.setLast3fTime(null);
				}

				results.add(result);
			}

			race.setRaceResults(results);
			return race;

		} catch (Exception e) {
			System.err.println("[❌エラー] レース詳細の解析に失敗しました: " + e.getMessage());
			// 失敗ログの記録（レースIDを保存）
			saveFailureLog(raceId, e.getMessage());
			return null;
		}
	}

	/**
	 * 【失敗ログ保存用プライベートヘルパー】
	 */
	private void saveFailureLog(String targetId, String errorMessage) {
		try {
			ScrapingFailureLog log = new ScrapingFailureLog(targetId, errorMessage);
			failureLogRepository.save(log);
			System.out.println("[📝ログ記録] 失敗データをDBに保存しました - 対象ID: " + targetId);
		} catch (Exception ex) {
			System.err.println("[❌ログ保存失敗] " + ex.getMessage());
		}
	}

	/**
	 * 【リトライ共通ヘルパーメソッド】
	 */
	public static <T> T executeWithRetry(Supplier<T> action, int maxRetries) {
		int attempts = 0;
		while (true) {
			try {
				return action.get();
			} catch (Exception e) {
				attempts++;
				if (attempts >= maxRetries) {
					System.err.println("[❌リトライ上限オーバー] 処理が規定回数失敗しました: " + e.getMessage());
					throw new RuntimeException(e);
				}
				long waitTime = attempts * 2000L;
				System.out.println("[⚠️一時エラー] 接続に失敗しました。 " + waitTime + "ミリ秒後にリトライします... (試行回数: " + attempts + ")");
				try {
					Thread.sleep(waitTime);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(ie);
				}
			}
		}
	}
}