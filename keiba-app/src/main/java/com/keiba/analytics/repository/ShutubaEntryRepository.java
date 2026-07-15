package com.keiba.analytics.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.keiba.analytics.entity.ShutubaEntry;

@Repository
public interface ShutubaEntryRepository extends JpaRepository<ShutubaEntry, Long> {
    
    // アンダースコア(_)でShutubaRaceの中にあるフィールドを指定する
    Optional<ShutubaEntry> findByShutubaRace_NetkeibaRaceIdAndHorseNumber(String netkeibaRaceId, Integer horseNumber);
}