package com.keiba.analytics.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.keiba.analytics.dto.AnalysisResultDto;
import com.keiba.analytics.entity.Race;
import com.keiba.analytics.entity.RaceResult;
import com.keiba.analytics.service.AnalysisService;
import com.keiba.analytics.service.RaceService;

@Controller
public class RaceController {

	private final RaceService raceService;

	@Autowired
	private AnalysisService analysisService;

	// コンストラクタでRaceServiceを注入
	public RaceController(RaceService raceService) {
		this.raceService = raceService;
	}

	/**
	 * ブラウザで「http://localhost:8080/races」にアクセスしたときに動くメソッド
	 */
	@GetMapping("/races")
	public String showRaceList(
			@PageableDefault(size = 20, sort = "raceDate", direction = Sort.Direction.DESC) Pageable pageable,
			Model model) {

		Page<Race> racePage = raceService.getRaces(pageable);

		model.addAttribute("racePage", racePage);
		model.addAttribute("races", racePage.getContent());

		try {
			List<AnalysisResultDto> analysisList = analysisService.getAnalysisResults();
			model.addAttribute("analysisList", analysisList);
		} catch (Exception e) {
			System.err.println("分析結果の読み込みに失敗しました: " + e.getMessage());
		}
		return "race-list";
	}

	/**
	 * 追加 👇: 各レースの詳細な結果一覧（1着〜16着など）を表示するメソッド
	 * 「http://localhost:8080/races/1/results」などでアクセスします
	 */
	@GetMapping("/races/{raceId}/results")
	public String showRaceResults(@PathVariable("raceId") Long raceId, Model model) {
		// 1. Service、またはRaceの持つリレーションから直接データを取得
		// ※ 既に RaceService があるため、そこから特定のレースを取得する想定です
		Race race = raceService.getRaceById(raceId);

		if (race == null) {
			throw new IllegalArgumentException("対象のレースが見つかりません ID: " + raceId);
		}

		// 2. 既にRaceクラス内に @OneToMany で List<RaceResult> raceResults がマッピングされているため、
		//   そこから直接着順リストを抽出します。
		List<RaceResult> results = race.getRaceResults();

		// 3. Thymeleafにレース情報と結果一覧を渡す
		model.addAttribute("race", race);
		model.addAttribute("results", results);

		// 表示するHTMLファイルの名前を指定（src/main/resources/templates/race-results.html）
		return "race-results";
	}

}