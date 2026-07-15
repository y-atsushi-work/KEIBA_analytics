package com.keiba.analytics.service;

import java.time.LocalDate;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keiba.analytics.entity.ShutubaEntry;
import com.keiba.analytics.entity.ShutubaRace;
import com.keiba.analytics.repository.ShutubaEntryRepository;
import com.keiba.analytics.repository.ShutubaRaceRepository;
import com.keiba.analytics.util.KeibaUtils;

@Service
public class ShutubaService {
	// 1. @Autowiredは不要。final をつけることでコンストラクタで必ず注入されるようになる
	private final ShutubaRaceRepository shutubaRaceRepository;
	private final ShutubaEntryRepository shutubaEntryRepository;

	// 2. コンストラクタでまとめて注入（これがベストプラクティス）
	public ShutubaService(ShutubaRaceRepository shutubaRaceRepository,
			ShutubaEntryRepository shutubaEntryRepository) {
		this.shutubaRaceRepository = shutubaRaceRepository;
		this.shutubaEntryRepository = shutubaEntryRepository;
	}

	/**
	 * 出馬表データをDBに保存（または上書き更新）する
	 */
	@Transactional
	public void saveOrUpdateShutuba(ShutubaRace newRace) {
		String raceId = newRace.getNetkeibaRaceId();

		// 既存のレースを探す
		shutubaRaceRepository.findByNetkeibaRaceId(raceId).ifPresentOrElse(existingRace -> { // 既存の各馬のデータを「最新のスクレイピング結果」で更新する
			for (ShutubaEntry newEntry : newRace.getEntries()) {
				existingRace.getEntries().stream()
						.filter(e -> e.getHorseNumber().equals(newEntry.getHorseNumber()))
						.findFirst()
						.ifPresent(oldEntry -> {
							// ここで「値がある場合のみ更新」するようにする
							if (newEntry.getOdds() != null)
								oldEntry.setOdds(newEntry.getOdds());
							if (newEntry.getPopularity() != null)
								oldEntry.setPopularity(newEntry.getPopularity());
							if (newEntry.getHorseWeight() != null)
								oldEntry.setHorseWeight(newEntry.getHorseWeight());
						});
			}
			System.out.println("[🔄更新] 出馬表: " + existingRace.getRaceName() + " (" + raceId + ") のデータを更新しました。");
		}, () -> {
			// 【新規の場合】そのまま保存
			shutubaRaceRepository.save(newRace);
			System.out.println("[🆕保存] 出馬表: " + newRace.getRaceName() + " (" + raceId + ") を新規保存しました。");
		});
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
	public void scrapeAndSaveShutuba(String raceId, LocalDate raceDate) {
		String locationCode = raceId.substring(4, 6);
		String roundStr = raceId.substring(6, 8);

		String url = "https://race.netkeiba.com/race/shutuba.html?race_id=" + raceId;
		try {
			Document doc = Jsoup.connect(url)
					.userAgent(
							"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
					.timeout(10000)
					.get();

			ShutubaRace race = new ShutubaRace();
			race.setNetkeibaRaceId(raceId);
			race.setRaceDate(raceDate);
			race.setRaceName(doc.select("h1").text());
			race.setLocation(KeibaUtils.convertLocationCode(locationCode));
			race.setRound(Integer.parseInt(roundStr));

			for (Element row : doc.select("tr.HorseList")) {

				try {
					ShutubaEntry entry = new ShutubaEntry();
					// 1. 枠番 (class="Waku1 Txt_C") -> spanの中身を取得
					entry.setBracketNumber(Integer.parseInt(row.selectFirst("td[class*=Waku] span").text()));
					// 2. 馬番 (class="Umaban1 Txt_C")
					entry.setHorseNumber(Integer.parseInt(row.selectFirst("td[class*=Umaban]").text()));
					// 3. 馬名 (class="HorseName") -> spanの中のaタグのtitle属性またはテキスト
					entry.setHorseName(row.selectFirst("span.HorseName a").text());
					// 4. 性齢 (class="Barei Txt_C")
					entry.setSexAge(row.selectFirst("td.Barei").text());
					// 5. 斤量 (class="Txt_C" だが他と被るため、クラスの特定が難しい場合)
					// ここは前後の位置関係から「Bareiの次のTD」を指定するのが最も確実です
					entry.setCarriedWeight(row.selectFirst("td.Barei").nextElementSibling().text());
					// 6. 騎手 (class="Jockey")
					entry.setJockey(row.selectFirst("td.Jockey a").text());
					// 7. 馬体重(増減) (class="Weight") -> spanの中身まで含めるなら .text()
					entry.setHorseWeight(row.selectFirst("td.Weight").text());
					// 8. 予想オッズ
					Element oddsElem = row.selectFirst("td.Popular span");
					if (oddsElem != null) {
					    String oddsText = oddsElem.text().trim();
					    // オッズが "---.-" の場合は null にする
					    if (oddsText.contains("-") || oddsText.isEmpty()) {
					        entry.setOdds(null);
					    } else {
					        entry.setOdds(Double.parseDouble(oddsText));
					    }
					}
					// 9. 人気
					Element ninkiElem = row.selectFirst("td.Popular_Ninki span[id^=ninki-]");
					if (ninkiElem != null && !ninkiElem.text().contains("*")) {
						entry.setPopularity(Integer.parseInt(ninkiElem.text()));
					} else {
						entry.setPopularity(null);
					}
					// HorseId の抽出
					Element horseLink = row.selectFirst("td.HorseInfo a");
					if (horseLink != null) {
						String href = horseLink.attr("href");
						entry.setNetkeibaHorseId(href.substring(href.lastIndexOf("/") + 1));
					}
					race.addEntry(entry);
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

	/**
	 * 今日以降の出馬表データを開催日順で取得する
	 */
	public List<ShutubaRace> getUpcomingRaces() {
		//		LocalDate today = LocalDate.now();  今日移行で検索する処理なので、一旦コメントアウト
		LocalDate today = LocalDate.of(2026, 07, 01);
		System.out.println("検索に使用する日付: " + today); // ここをコンソールで確認

		List<ShutubaRace> result = shutubaRaceRepository.findByRaceDateGreaterThanEqualOrderByRaceDateAsc(today);
		System.out.println("検索結果の件数: " + result.size());
		return result;
	}

}