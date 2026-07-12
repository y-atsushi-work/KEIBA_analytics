package com.keiba.analytics.service;

import java.time.LocalDate;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keiba.analytics.entity.ShutubaEntry;
import com.keiba.analytics.entity.ShutubaRace;
import com.keiba.analytics.repository.ShutubaRaceRepository;

@Service
public class ShutubaService {

	@Autowired
	private ShutubaRaceRepository shutubaRaceRepository;

	/**
	 * 出馬表データをDBに保存（または上書き更新）する
	 */
	@Transactional
	public void saveOrUpdateShutuba(ShutubaRace newShutubaData) {
		String raceId = newShutubaData.getNetkeibaRaceId();

		// 既に同じレースIDの予定データが存在する場合は、一度削除してから入れ直す
		shutubaRaceRepository.findByNetkeibaRaceId(raceId)
				.ifPresent(existingRace -> shutubaRaceRepository.delete(existingRace));

		shutubaRaceRepository.flush(); // 即時反映

		// 新しい出馬表データを保存
		shutubaRaceRepository.save(newShutubaData);
		System.out.println("[⏳予定保存] 出馬表: " + newShutubaData.getRaceName() + " (" + raceId + ") を一時テーブルに保存しました。");
	}

	/**
	 * 【クリーンアップ処理】
	 * 既に終了した過去の出馬表データ（テンポラリ）を削除する。
	 */
	@Transactional
	public void cleanupOldShutubaData() {
		LocalDate today = LocalDate.now();
		shutubaRaceRepository.deleteByRaceDateBefore(today);
		System.out.println("[🧹掃除完了] 昨日以前の出馬表一時データを削除しました。");
	}

	/**
	 * 出走予定ページをスクレイピングして解析する
	 */
	public void scrapeAndSaveShutuba(String raceId) {
		String url = "https://race.netkeiba.com/race/shutuba.html?race_id=" + raceId;
		try {
			Document doc = Jsoup.connect(url)
					.userAgent(
							"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
					.timeout(10000)
					.get();

			ShutubaRace race = new ShutubaRace();
			race.setNetkeibaRaceId(raceId);
			race.setRaceName(doc.select("h1").text());

			for (Element row : doc.select("tr.HorseList")) {
				try {
					// セレクタで値を取得し、trim() で空白を除去
					String wakuText = row.select("td[class^=Waku]").text().trim();
					String umabanText = row.select("td[class^=Umaban]").text().trim();

					// どちらか一方でも空なら、それはデータ行ではない(ヘッダー等)とみなしてスキップ
					if (wakuText.isEmpty() || umabanText.isEmpty())
						continue;

					ShutubaEntry entry = new ShutubaEntry();
					entry.setBracketNumber(Integer.parseInt(wakuText));
					entry.setHorseNumber(Integer.parseInt(umabanText));
					entry.setHorseName(row.select("td.HorseInfo span.HorseName").text());
					entry.setJockey(row.select("td.Jockey a").text());

					// HorseId の抽出
					Element horseLink = row.selectFirst("td.HorseInfo a");
					if (horseLink != null) {
						String href = horseLink.attr("href");
						entry.setNetkeibaHorseId(href.substring(href.lastIndexOf("/") + 1));
					}

					// 斤量 (Weight) の抽出
					entry.setWeight(row.select("td.Weight").text().trim());

					race.addEntry(entry);
					System.out.println("抽出: " + entry.getHorseNumber() + "番 " + entry.getHorseName());
				} catch (Exception e) {
					// ここで個別の行の解析エラーをキャッチすることで、ループを止めない
					System.err.println("行解析スキップ: " + e.getMessage());
				}
			}

			// 保存処理：データが1件以上取れた場合のみ保存
			if (!race.getEntries().isEmpty()) {
				saveOrUpdateShutuba(race);
			} else {
				System.err.println("[⚠️警告] 1件も馬データが取得できませんでした: " + raceId);
			}

		} catch (Exception e) {
			System.err.println("[❌出走予定解析エラー] " + raceId + ": " + e.getMessage());
		}
	}
}