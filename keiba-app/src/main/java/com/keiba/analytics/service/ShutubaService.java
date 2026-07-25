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

/**
 * 出馬表情報の収集・管理を行うサービス層。
 * 外部サイト（netkeiba）からのスクレイピングおよびDBへの保存・更新を担当します。
 */
@Service
public class ShutubaService {

	// リポジトリは変更不要のためfinalで定義し、コンストラクタ経由でDIする
	private final ShutubaRaceRepository shutubaRaceRepository;
	private final ShutubaEntryRepository shutubaEntryRepository;

	/**
	 * コンストラクタベースの依存性注入。
	 * Springがリポジトリのインスタンスを自動的に注入します。
	 */
	public ShutubaService(ShutubaRaceRepository shutubaRaceRepository,
			ShutubaEntryRepository shutubaEntryRepository) {
		this.shutubaRaceRepository = shutubaRaceRepository;
		this.shutubaEntryRepository = shutubaEntryRepository;
	}

	/**
	 * スクレイピングしたレースデータをDBに反映します。
	 * 既にレースIDが存在する場合はデータを更新し、存在しない場合は新規保存します。
	 */
	@Transactional
	public void saveOrUpdateShutuba(ShutubaRace newRace) {
		String raceId = newRace.getNetkeibaRaceId();

		// データベースに既存のレース情報が存在するか確認
		shutubaRaceRepository.findByNetkeibaRaceId(raceId).ifPresentOrElse(existingRace -> {
			// 【更新処理】既存レースの各出走馬データをマッチングして更新
			for (ShutubaEntry newEntry : newRace.getEntries()) {
				existingRace.getEntries().stream()
						.filter(e -> e.getHorseNumber().equals(newEntry.getHorseNumber())) // 馬番で一致判定
						.findFirst()
						.ifPresent(oldEntry -> {
							// 現時点で更新が必要な項目（馬体重）を反映
							if (newEntry.getHorseWeight() != null)
								oldEntry.setHorseWeight(newEntry.getHorseWeight());
						});
			}
			System.out.println("[🔄更新] 出馬表: " + existingRace.getRaceName() + " (" + raceId + ") のデータを更新しました。");
		}, () -> {
			// 【新規保存処理】DBに該当レースがない場合はそのまま永続化
			shutubaRaceRepository.save(newRace);
			System.out.println("[🆕保存] 出馬表: " + newRace.getRaceName() + " (" + raceId + ") を新規保存しました。");
		});
	}

	/**
	 * 【クリーンアップ処理】
	 * 保持期間が過ぎた過去のレースデータをDBから削除し、テーブルの肥大化を防ぎます。
	 */
	@Transactional
	public void cleanupOldShutubaData() {
		LocalDate today = LocalDate.now();
		// 指定日付より前のデータを削除
		shutubaRaceRepository.deleteByRaceDateBefore(today);
		System.out.println("[🧹掃除完了] 今日より以前の出馬表一時データを削除しました。");
	}

	/**
	 * 指定されたレースIDの出走予定ページをスクレイピングし、
	 * レース情報および出走馬情報を解析してDBに保存します。
	 */
	public void scrapeAndSaveShutuba(String raceId, LocalDate raceDate) {
		// レースIDから開催場所コードとラウンド数を抽出（netkeibaの仕様に基づく）
		String locationCode = raceId.substring(4, 6);
		String roundStr = raceId.substring(6, 8);

		String url = "https://race.netkeiba.com/race/shutuba.html?race_id=" + raceId;

		try {
			// 指定URLへ接続し、HTMLドキュメントを取得
			// サーバー側からのブロックを防ぐため、適切なUser-Agentを設定
			Document doc = Jsoup.connect(url)
					.userAgent(
							"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
					.timeout(10000)
					.get();

			// レースの基本情報を設定
			ShutubaRace race = new ShutubaRace();
			race.setNetkeibaRaceId(raceId);
			race.setRaceDate(raceDate);
			race.setRaceName(doc.select("h1").text()); // ページタイトルからレース名を取得
			race.setLocation(KeibaUtils.convertLocationCode(locationCode));
			race.setRound(Integer.parseInt(roundStr));

			// 出走馬リスト（tr.HorseList クラス）をループで処理
			for (Element row : doc.select("tr.HorseList")) {
				try {
					ShutubaEntry entry = new ShutubaEntry();

					// 各列の値をDOMのクラス指定から抽出
					entry.setBracketNumber(Integer.parseInt(row.selectFirst("td[class*=Waku] span").text())); // 枠番
					entry.setHorseNumber(Integer.parseInt(row.selectFirst("td[class*=Umaban]").text())); // 馬番
					entry.setHorseName(row.selectFirst("span.HorseName a").text()); // 馬名
					entry.setSexAge(row.selectFirst("td.Barei").text()); // 性齢
					// 馬体重の近くにある「斤量」情報を取得
					entry.setCarriedWeight(row.selectFirst("td.Barei").nextElementSibling().text());
					entry.setJockey(row.selectFirst("td.Jockey a").text()); // 騎手名
					entry.setHorseWeight(row.selectFirst("td.Weight").text()); // 馬体重(増減)

					// 個別の馬詳細ページへのリンクからIDを抜き出し（文字列操作）
					Element horseLink = row.selectFirst("td.HorseInfo a");
					if (horseLink != null) {
						String href = horseLink.attr("href");
						entry.setNetkeibaHorseId(href.substring(href.lastIndexOf("/") + 1));
					}

					// レースエンティティに出走馬を追加
					race.addEntry(entry);
				} catch (Exception e) {
					// 特定の馬データで解析エラーが発生しても、他の馬の解析を止めないための個別キャッチ
					System.err.println("行解析スキップ: " + e.getMessage());
				}
			}

			// 出走馬情報が1件でも取れた場合のみ、DB更新フローへ回す
			if (!race.getEntries().isEmpty()) {
				saveOrUpdateShutuba(race);
			} else {
				System.err.println("[⚠️警告] 1件も馬データが取得できませんでした: " + raceId);
			}

		} catch (Exception e) {
			// スクレイピング全体が失敗した場合のエラーログ
			System.err.println("[❌出走予定解析エラー] " + raceId + ": " + e.getMessage());
		}
	}

	/**
	 * 今後の開催予定レースを、開催日の昇順で全件取得します。
	 */
	public List<ShutubaRace> getUpcomingRaces() {
		// 現在は検証のため固定日付を利用しているが、必要に応じてLocalDate.now()に切り替え可能
		LocalDate today = LocalDate.of(2026, 07, 01);
		System.out.println("検索に使用する日付: " + today);

		List<ShutubaRace> result = shutubaRaceRepository.findByRaceDateGreaterThanEqualOrderByRaceDateAsc(today);
		System.out.println("検索結果の件数: " + result.size());

		return result;
	}
}