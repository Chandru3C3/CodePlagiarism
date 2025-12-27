package com.plagiarism.detector.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.plagiarism.detector.entity.Submission;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

	// Find submissions by language
	List<Submission> findByLanguage(String language);

	// Find submissions by uploaded user
	List<Submission> findByUploadedBy(String username);

	// Custom query: Find submissions by language and user
	@Query("SELECT s FROM Submission s WHERE s.language = :language AND s.uploadedBy = :username")
	List<Submission> findByLanguageAndUser(@Param("language") String language, @Param("username") String username);

	// Custom query: Count submissions by user
	@Query("SELECT COUNT(s) FROM Submission s WHERE s.uploadedBy = :username")
	Long countByUser(@Param("username") String username);

	// Custom query: Find recent submissions
	@Query("SELECT s FROM Submission s ORDER BY s.uploadedAt DESC")
	List<Submission> findRecentSubmissions();
}