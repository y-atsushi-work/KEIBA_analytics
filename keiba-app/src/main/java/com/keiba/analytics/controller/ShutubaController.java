package com.keiba.analytics.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.keiba.analytics.entity.ShutubaRace;
import com.keiba.analytics.service.ShutubaService;

@Controller
public class ShutubaController {

	private final ShutubaService shutubaService;

	public ShutubaController(ShutubaService shutubaService) {
		this.shutubaService = shutubaService;
	}
	
	@GetMapping("/shutuba")
	public String showShutubaList(Model model) {
		// 今後の開催予定レースを取得
	    List<ShutubaRace> list = shutubaService.getUpcomingRaces();
	    
	    // オッズ取得処理（fetchAndSaveOdds）はサービスから削除したため、
	    // コントローラー側の呼び出しも削除しました。
	    
	    model.addAttribute("shutubaList", list);
	    return "shutuba-list";
	}
}