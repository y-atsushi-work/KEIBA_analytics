package com.keiba.analytics.util;

public class KeibaUtils {

	// 競馬場コードを名前に変換
	public static String convertLocationCode(String code) {
		return switch (code) {
		case "01" -> "札幌";
		case "02" -> "函館";
		case "03" -> "福島";
		case "04" -> "新潟";
		case "05" -> "東京";
		case "06" -> "中山";
		case "07" -> "中京";
		case "08" -> "京都";
		case "09" -> "阪神";
		case "10" -> "小倉";
		case "30" -> "門別";
		case "35" -> "盛岡";
		case "36" -> "水沢";
		case "42" -> "船橋";
		case "43" -> "大井";
		case "44" -> "川崎";
		case "45" -> "浦和";
		case "46" -> "金沢";
		case "51" -> "笠松";
		case "54" -> "名古屋";
		case "65" -> "園田";
		case "66" -> "姫路";
		case "73" -> "高知";
		case "86" -> "佐賀";
		default -> "その他";
		};
	}
}
