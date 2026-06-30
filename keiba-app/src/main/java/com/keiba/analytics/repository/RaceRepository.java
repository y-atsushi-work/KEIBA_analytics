package com.keiba.analytics.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.keiba.analytics.entity.Race;

@Repository
public interface RaceRepository extends JpaRepository<Race, Long> {

    /**
     * MySQLのracesテーブルの中から、race_date（開催日）が一番新しいレコードを1件だけ見つけて返す
     * (Find = 検索する, First = 1件だけ, By = 条件, OrderByRaceDateDesc = 開催日の降順で並び替える)
     */
    Optional<Race> findFirstByOrderByRaceDateDesc();

    /**
     * 指定されたnetkeibaRaceId（レースID）がすでにデータベースに登録されているか確認する
     */
    boolean existsByNetkeibaRaceId(String netkeibaRaceId);
}