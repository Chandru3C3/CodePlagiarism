package com.plagiarism.detector.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.plagiarism.detector.entity.ComparisonResult;

@Repository
public interface ComparisonResultRepository extends JpaRepository<ComparisonResult, Long> {

	// Find comparisons by file IDs
	List<ComparisonResult> findByFile1IdOrFile2Id(Long file1Id, Long file2Id);

	// Find comparisons by status
	List<ComparisonResult> findByStatus(String status);

	// Custom query: Find high risk comparisons
	@Query("SELECT c FROM ComparisonResult c WHERE c.status = 'HIGH' ORDER BY c.overallSimilarity DESC")
	List<ComparisonResult> findHighRiskComparisons();

	// Custom query: Find comparisons with similarity above threshold
	@Query("SELECT c FROM ComparisonResult c WHERE c.overallSimilarity >= :threshold")
	List<ComparisonResult> findBySimilarityThreshold(@Param("threshold") Double threshold);

	// Custom query: Get average similarity
	@Query("SELECT AVG(c.overallSimilarity) FROM ComparisonResult c")
	Double getAverageSimilarity();

	// Custom query: Count by status
	@Query("SELECT c.status, COUNT(c) FROM ComparisonResult c GROUP BY c.status")
	List<Object[]> countByStatus();
}