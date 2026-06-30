package com.keiba.analytics.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

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
	 * 自動同期マネージャー（RaceSyncManager）からループで呼び出されます
	 */
	@Transactional
	public void syncRaceResult(String raceId) {
		// 1. 重複チェック
		if (raceRepository.existsByNetkeibaRaceId(raceId)) {
			return;
		}

		// 2. レースIDから開催日付を逆算（例: "202006010101" -> "20200601"）
		LocalDate raceDate;
		try {
			String dateStr = raceId.substring(0, 8);
			raceDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
		} catch (Exception e) {
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

	public List<Race> getAllRaces() {
		return raceRepository.findAll();
	}

	public Race getRaceById(Long id) {
		return raceRepository.findById(id).orElse(null);
	}
}