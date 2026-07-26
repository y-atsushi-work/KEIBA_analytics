package com.keiba.analytics.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.keiba.analytics.entity.ScrapingFailureLog;

@Repository
public interface ScrapingFailureLogRepository extends JpaRepository<ScrapingFailureLog, Long> {

	// まだ解決していない（resolved = false）失敗ログの一覧を取得する
	List<ScrapingFailureLog> findByResolvedFalse();

	// 特定のIDに関するログを検索する
	boolean existsByTargetIdAndResolvedFalse(String targetId);
}