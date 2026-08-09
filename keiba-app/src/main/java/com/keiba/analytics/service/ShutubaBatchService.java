package com.keiba.analytics.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ShutubaBatchService {

	@Autowired
	private ShutubaService shutubaService;

	// ※ 過去7年分のバッチ（RaceSyncManager）を優先して動かすため、
	// 起動時の強制デバッグ実行（@PostConstruct）はコメントアウトを推奨します。
	/*
	@PostConstruct
	public void init() {
		System.out.println("★【デバッグ】強制的にバッチを開始します！");
		processRacesForDate(LocalDate.of(2026, 7, 12));
	}
	*/

	// 1. 【自動実行】30分毎に実行（当日のみ）
	@Scheduled(cron = "0 0/30 9-16 * * *")
	public void scheduledRun() {
		System.out.println("[⏰定時実行] 当日のレースデータを更新します。");
		processRacesForDate(LocalDate.now());
	}

	// 2. 【任意実行】7日分の一括更新
	public void executeWeeklyShutubaCrawl() {
		System.out.println("[🚀全件一括更新] 7日分のバッチを開始します。");
		LocalDate today = LocalDate.now();
		for (int i = 0; i < 7; i++) {
			processRacesForDate(today.plusDays(i));  
		}
	}

	// 3. 【共通化】指定日付のレースIDを取得して保存する共通処理
	private void processRacesForDate(LocalDate targetDate) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
		String dateStr = targetDate.format(formatter);
		String topUrl = "https://race.netkeiba.com/top/race_list_sub.html?kaisai_date=" + dateStr;

		try {
			Thread.sleep(1500); // サーバー負荷軽減

			Document doc = Jsoup.connect(topUrl)
					.userAgent(
							"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
					.header("Accept-Language", "ja,en-US;q=0.9,en;q=0.8")
					.header("Referer", "https://race.netkeiba.com/")
					.get();

			String htmlContent = doc.html();
			Pattern raceIdPattern = Pattern.compile("(?i)race_id[^0-9]{1,5}([0-9]{12})");
			Matcher matcher = raceIdPattern.matcher(htmlContent);

			List<String> raceIds = new ArrayList<>();
			while (matcher.find()) {
				String raceId = matcher.group(1);
				if (!raceIds.contains(raceId)) {
					raceIds.add(raceId);
				}
			}

			if (raceIds.isEmpty()) {
				System.out.println("[ℹ️スキップ] " + targetDate + " はレースIDが見つかりません。");
				return;
			}

			System.out.println(" ➡️ " + targetDate + " : " + raceIds.size() + " 件取得しました。");

			for (String raceId : raceIds) {
				try {
					shutubaService.scrapeAndSaveShutuba(raceId, targetDate);
					Thread.sleep(1000);
				} catch (Exception e) {
					System.err.println("[❌詳細取得エラー] ID: " + raceId + " - " + e.getMessage());
				}
			}
		} catch (Exception e) {
			System.err.println("[❌エラー] " + dateStr + " の取得失敗: " + e.getMessage());
		}
	}
}