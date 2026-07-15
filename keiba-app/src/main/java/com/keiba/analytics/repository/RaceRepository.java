package com.keiba.analytics.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.keiba.analytics.entity.Race;

@Repository
public interface RaceRepository extends JpaRepository<Race, Long> {

    /**
     * 【ページネーション対応】全レースをページ単位で取得します。
     * コントローラー側で Pageable オブジェクトを渡すことで、表示件数を制御できます。
     */
    Page<Race> findAll(Pageable pageable);

    /**
     * MySQLのracesテーブルの中から、race_date（開催日）が一番新しいレコードを1件だけ見つけて返す
     */
    Optional<Race> findFirstByOrderByRaceDateDesc();

    /**
     * 指定されたnetkeibaRaceId（レースID）がすでにデータベースに登録されているか確認する
     */
    boolean existsByNetkeibaRaceId(String netkeibaRaceId);
}