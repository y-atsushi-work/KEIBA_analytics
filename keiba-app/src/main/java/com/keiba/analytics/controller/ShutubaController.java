package com.keiba.analytics.controller;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.keiba.analytics.entity.ShutubaRace;
import com.keiba.analytics.service.AnalysisService;
import com.keiba.analytics.service.ShutubaService;

@Controller
public class ShutubaController {

	private final ShutubaService shutubaService;
	private final AnalysisService analysisService;

	public ShutubaController(ShutubaService shutubaService, AnalysisService analysisService) {
		this.shutubaService = shutubaService;
		this.analysisService = analysisService;
	}

	@GetMapping("/shutuba")
	public String showShutubaList(Model model) {
		// 今後の開催予定レースを取得
		List<ShutubaRace> list = shutubaService.getUpcomingRaces();

		// 予想スコアをJSONから読み込み、モデルに追加
		try {
			Map<String, Double> scoreMap = analysisService.getPredictionScores();
			model.addAttribute("scoreMap", scoreMap);
			// 💡 デバッグ用ログ
			System.out.println("★スコアマップの読み込み成功: " + (scoreMap != null ? scoreMap.size() : 0) + "件");
		} catch (Exception e) {
			System.err.println("❌ 予想スコアの読み込みに失敗しました: " + e.getMessage());
			e.printStackTrace(); // エラーの詳細を出す
		}

		model.addAttribute("shutubaList", list);
		return "shutuba-list"; // テンプレート名（shutuba-list.html）
	}
}