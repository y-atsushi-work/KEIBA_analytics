package com.keiba.analytics.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.keiba.analytics.entity.ShutubaRace;
import com.keiba.analytics.service.ShutubaService; // サービスが用意されている想定

@Controller
public class ShutubaController {

	private final ShutubaService shutubaService;

	public ShutubaController(ShutubaService shutubaService) {
		this.shutubaService = shutubaService;
	}

	@GetMapping("/shutuba")
	public String showShutubaList(Model model) {
		List<ShutubaRace> list = shutubaService.getUpcomingRaces();
		System.out.println("取得したレース数: " + list.size()); // ログ出力
		model.addAttribute("shutubaList", list);
		return "shutuba-list";
	}
	
}