package com.keiba.analytics.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.keiba.analytics.entity.ShutubaRace;

@Repository
public interface ShutubaRaceRepository extends JpaRepository<ShutubaRace, Long> {
    
    // IDで存在チェックや取得を行うため
    boolean existsByNetkeibaRaceId(String netkeibaRaceId);
    Optional<ShutubaRace> findByNetkeibaRaceId(String netkeibaRaceId);

    // 【重要】指定した日付より古い「出走予定」を一括削除するためのメソッド
    void deleteByRaceDateBefore(LocalDate date);
}