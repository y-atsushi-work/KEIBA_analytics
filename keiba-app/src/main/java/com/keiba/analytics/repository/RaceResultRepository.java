package com.keiba.analytics.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.keiba.analytics.entity.RaceResult;

@Repository
public interface RaceResultRepository extends JpaRepository<RaceResult, Long> {
}