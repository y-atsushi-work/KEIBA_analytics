package com.keiba.analytics.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keiba.analytics.entity.Horse;
import com.keiba.analytics.entity.Race;
import com.keiba.analytics.entity.RaceResult;
import com.keiba.analytics.repository.HorseRepository;
import com.keiba.analytics.repository.RaceRepository;

@Service
public class RaceService {

	private final RaceRepository raceRepository;
	private final HorseRepository horseRepository;
	private final KeibaScraper keibaScraper;

	public RaceService(RaceRepository raceRepository, HorseRepository horseRepository, KeibaScraper keibaScraper) {
		this.raceRepository = raceRepository;
		this.horseRepository = horseRepository;
		this.keibaScraper = keibaScraper;
	}

	/**
	 * 指定されたレースIDの結果をスクレイピングし、重複がなければDBに保存する
	 */
	@Transactional
	public void syncRaceResult(String raceId) {
		// 1. 重複チェック
		if (raceRepository.existsByNetkeibaRaceId(raceId)) {
			return;
		}

		// 2. 過去レースIDから開催日付を正しく逆算
		LocalDate raceDate;
		try {
			String yearStr = raceId.substring(0, 4); // 西暦4桁
			String locCode = raceId.substring(4, 6); // 競馬場コード2桁
			int locNum = Integer.parseInt(locCode);

			if (locNum <= 10) {
				raceDate = LocalDate.parse(raceId.substring(0, 8), DateTimeFormatter.ofPattern("yyyyMMdd"));
			} else {
				String mmddStr = raceId.substring(6, 10); 
				String fullDateStr = yearStr + mmddStr;
				raceDate = LocalDate.parse(fullDateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
			}
		} catch (Exception e) {
			System.err.println("[⚠️警告] 日付の逆算に失敗したため、システム日付を使用します: " + raceId);
			raceDate = LocalDate.now();
		}

		// 3. スクレイパーを呼び出してWebからデータを取得
		Race race = keibaScraper.parseRaceResultPagePublic(raceId, raceDate);

		if (race == null || race.getRaceResults() == null || race.getRaceResults().isEmpty()) {
			return;
		}

		// 4. 既存の馬保存ロジック（重複防止）
		for (RaceResult result : race.getRaceResults()) {
			Horse horse = result.getHorse();
			Optional<Horse> existingHorseOpt = horseRepository.findByNetkeibaHorseId(horse.getNetkeibaHorseId());
			if (existingHorseOpt.isPresent()) {
				result.setHorse(existingHorseOpt.get());
			} else {
				horseRepository.save(horse);
			}
		}

		// 5. レースと結果を一括保存
		raceRepository.save(race);
		System.out.println("[💾DB保存完了] レース: " + race.getRaceName() + " (" + raceId + ") のデータを蓄積しました。");
	}

	/**
	 * ページネーション対応：レース一覧をページ単位で取得する
	 */
	public Page<Race> getRaces(Pageable pageable) {
		return raceRepository.findAll(pageable);
	}

	public List<Race> getAllRaces() {
		return raceRepository.findAll();
	}

	public Race getRaceById(Long id) {
		return raceRepository.findById(id).orElse(null);
	}
}