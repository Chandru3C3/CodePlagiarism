package com.plagiarism.detector.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@Entity
@Table(name = "submissions")
@NamedQuery(name = "Submission.findAll", query = "SELECT d FROM Submission d")
public class Submission {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "file_name", nullable = false)
	private String fileName;

	@Column(name = "file_content", columnDefinition = "TEXT", nullable = false)
	private String fileContent;

	@Column(name = "language", nullable = false)
	private String language;

	@Column(name = "uploaded_at", nullable = false)
	private LocalDateTime uploadedAt;

	@Column(name = "uploaded_by", nullable = false)
	private String uploadedBy;

	@Column(name = "tokenized_content", columnDefinition = "TEXT")
	private String tokenizedContent;

	@Column(name = "ast_structure", columnDefinition = "TEXT")
	private String astStructure;

	// Default Constructor
	public Submission() {
	}

	// Parameterized Constructor
	public Submission(String fileName, String fileContent, String language, LocalDateTime uploadedAt,
			String uploadedBy) {
		this.fileName = fileName;
		this.fileContent = fileContent;
		this.language = language;
		this.uploadedAt = uploadedAt;
		this.uploadedBy = uploadedBy;
	}

	// Getters and Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFileContent() {
		return fileContent;
	}

	public void setFileContent(String fileContent) {
		this.fileContent = fileContent;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public LocalDateTime getUploadedAt() {
		return uploadedAt;
	}

	public void setUploadedAt(LocalDateTime uploadedAt) {
		this.uploadedAt = uploadedAt;
	}

	public String getUploadedBy() {
		return uploadedBy;
	}

	public void setUploadedBy(String uploadedBy) {
		this.uploadedBy = uploadedBy;
	}

	public String getTokenizedContent() {
		return tokenizedContent;
	}

	public void setTokenizedContent(String tokenizedContent) {
		this.tokenizedContent = tokenizedContent;
	}

	public String getAstStructure() {
		return astStructure;
	}

	public void setAstStructure(String astStructure) {
		this.astStructure = astStructure;
	}

	@Override
	public String toString() {
		return "Submission{" + "id=" + id + ", fileName='" + fileName + '\'' + ", language='" + language + '\''
				+ ", uploadedAt=" + uploadedAt + ", uploadedBy='" + uploadedBy + '\'' + '}';
	}
}