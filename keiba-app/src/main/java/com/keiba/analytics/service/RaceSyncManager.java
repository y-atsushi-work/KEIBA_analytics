package com.keiba.analytics.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.keiba.analytics.repository.RaceRepository;

@Component
public class RaceSyncManager implements CommandLineRunner {

    private final RaceRepository raceRepository;
    private final KeibaScraper keibaScraper;
    private final RaceService raceService; // 既存の1レース保存用サービス

    public RaceSyncManager(RaceRepository raceRepository, KeibaScraper keibaScraper, RaceService raceService) {
        this.raceRepository = raceRepository;
        this.keibaScraper = keibaScraper;
        this.raceService = raceService;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("[⚙️同期システム] アプリ起動時のデータ同期チェックを開始します...");

        //==一時コメントアウト
//        // 1. 既存のメソッドを利用して最新のRaceエンティティを取得し、その開催日（raceDate）を取り出す
//        Optional<Race> latestRaceOpt = raceRepository.findFirstByOrderByRaceDateDesc();
//        
//        // データがあればその日付、なければ過去の基準日（例: 2024年12月1日）を設定
//        LocalDate startDate = latestRaceOpt.map(Race::getRaceDate).orElse(LocalDate.of(2024, 12, 1));
//        LocalDate endDate = LocalDate.now(); // 現在の日付まで

        //デバック用に記述
        LocalDate startDate = LocalDate.of(2020, 1, 1);
        LocalDate endDate = LocalDate.of(2020, 1, 31);
        System.out.println("[⚙️同期システム] 【デバッグモード】2020年1月の1ヶ月間限定で同期テストを開始します...");
        
        System.out.println("[⚙️同期システム] 前回最終取得日: " + startDate + " 〜 本日: " + endDate);

        if (startDate.isAfter(endDate) || startDate.isEqual(endDate)) {
            System.out.println("[⚙️同期システム] すでにデータベースは最新です。同期をスキップします。");
            return;
        }

        // 2. 開始月から終了月までの開催日をループで調査
        LocalDate currentMonth = startDate.withDayOfMonth(1);
        while (!currentMonth.isAfter(endDate.withDayOfMonth(1))) {
            System.out.println("[⚙️同期システム] カレンダー巡回中... " + currentMonth.getYear() + "年" + currentMonth.getMonthValue() + "月");
            
            List<LocalDate> raceDates = keibaScraper.fetchRaceDatesOfMonth(currentMonth.getYear(), currentMonth.getMonthValue());
            
            for (LocalDate raceDate : raceDates) {
                // 前回取得日以前の古い開催日はスキップ
                if (raceDate.isBefore(startDate)) {
                    continue;
                }
                
                System.out.println("[⚙️同期システム] 開催日を発見: " + raceDate + "。全レースのインポートを開始します。");
                List<String> raceIds = keibaScraper.fetchRaceIdsOfDate(raceDate);
                
                for (String raceId : raceIds) {
                    try {
                        // 既存の1レース取得・保存ロジックを再利用してDBへ蓄積
                        raceService.syncRaceResult(raceId);
                    } catch (Exception e) {
                        System.err.println("[❌エラー] レースID: " + raceId + " の同期に失敗しました。");
                    }
                }
            }
            // 翌月へ進める
            currentMonth = currentMonth.plusMonths(1);
        }

        System.out.println("[⚙️同期システム] すべてのバックログデータのインポートが完了しました！");
    }
}