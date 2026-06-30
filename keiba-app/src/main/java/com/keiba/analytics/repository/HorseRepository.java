package com.keiba.analytics.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.keiba.analytics.entity.Horse;

@Repository
public interface HorseRepository extends JpaRepository<Horse, Long> {
    // 馬IDで重複チェックをするためのメソッド
    Optional<Horse> findByNetkeibaHorseId(String netkeibaHorseId);
}