package com.plagiarism.detector.controller;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.plagiarism.detector.dto.AnalysisRequest;
import com.plagiarism.detector.dto.ComparisonResponse;
import com.plagiarism.detector.dto.MatchSegment;
import com.plagiarism.detector.dto.UploadResponse;
import com.plagiarism.detector.entity.ComparisonResult;
import com.plagiarism.detector.entity.Submission;
import com.plagiarism.detector.repository.ComparisonResultRepository;
import com.plagiarism.detector.repository.SubmissionRepository;
import com.plagiarism.detector.service.PlagiarismDetectionService;

@RestController
@RequestMapping("/api")
//@CrossOrigin(origins = { "http://localhost:4200", "http://localhost:3000" })
public class PlagiarismController {

	private final SubmissionRepository submissionRepository;
	private final ComparisonResultRepository comparisonResultRepository;
	private final PlagiarismDetectionService detectionService;

	@Autowired
	public PlagiarismController(SubmissionRepository submissionRepository,
			ComparisonResultRepository comparisonResultRepository, PlagiarismDetectionService detectionService) {
		this.submissionRepository = submissionRepository;
		this.comparisonResultRepository = comparisonResultRepository;
		this.detectionService = detectionService;
	}

	/**
	 * Upload multiple files
	 */
	@PostMapping("/upload")
	public ResponseEntity<?> uploadFiles(@RequestParam("files") MultipartFile[] files,
			@RequestParam("username") String username) {
		List<Long> ids = new ArrayList<>();

		try {
			for (MultipartFile file : files) {
				// Read file content
				String content = new String(file.getBytes());

				// Detect language
				String language = detectLanguage(file.getOriginalFilename());

				// Create submission entity
				Submission submission = new Submission();
				submission.setFileName(file.getOriginalFilename());
				submission.setFileContent(content);
				submission.setLanguage(language);
				submission.setUploadedAt(LocalDateTime.now());
				submission.setUploadedBy(username);

				// Save to database
				Submission savedSubmission = submissionRepository.save(submission);
				ids.add(savedSubmission.getId());
			}

			UploadResponse response = new UploadResponse("Files uploaded successfully", ids);
			return ResponseEntity.ok(response);

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload failed: " + e.getMessage());
		}
	}

	/**
	 * Analyze submissions for plagiarism
	 */
	@PostMapping("/analyze")
	public ResponseEntity<?> analyzeSubmissions(@RequestBody AnalysisRequest request) {
		List<ComparisonResponse> results = new ArrayList<>();
		List<Long> ids = request.getSubmissionIds();

		try {
			// Compare all pairs
			for (int i = 0; i < ids.size() - 1; i++) {
				for (int j = i + 1; j < ids.size(); j++) {
					Optional<Submission> sub1Opt = submissionRepository.findById(ids.get(i));
					Optional<Submission> sub2Opt = submissionRepository.findById(ids.get(j));

					if (sub1Opt.isPresent() && sub2Opt.isPresent()) {
						Submission sub1 = sub1Opt.get();
						Submission sub2 = sub2Opt.get();

						ComparisonResponse response = compareSubmissions(sub1, sub2);
						results.add(response);

						// Save to database
						saveComparisonResult(sub1, sub2, response);
					}
				}
			}

			return ResponseEntity.ok(results);

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Analysis failed: " + e.getMessage());
		}
	}

	/**
	 * Compare two submissions
	 */
	private ComparisonResponse compareSubmissions(Submission s1, Submission s2) {
		String code1 = s1.getFileContent();
		String code2 = s2.getFileContent();
		String language = s1.getLanguage();

		// Calculate similarities
		double tokenSim = detectionService.calculateTokenSimilarity(code1, code2);
		double structuralSim = 0.0;

		if ("java".equalsIgnoreCase(language)) {
			structuralSim = detectionService.calculateStructuralSimilarity(code1, code2);
		}

		double overallSim = detectionService.calculateOverallSimilarity(code1, code2, language);

		// Find matching segments
		List<MatchSegment> matches = detectionService.findMatchingSegments(code1, code2);

		// Determine status
		String status = detectionService.determineStatus(overallSim);

		// Create response
		ComparisonResponse response = new ComparisonResponse();
		response.setFile1Name(s1.getFileName());
		response.setFile2Name(s2.getFileName());
		response.setOverallSimilarity(overallSim);
		response.setTokenSimilarity(tokenSim);
		response.setStructuralSimilarity(structuralSim);
		response.setStatus(status);
		response.setMatches(matches);

		return response;
	}

	/**
	 * Save comparison result to database
	 */
	private void saveComparisonResult(Submission s1, Submission s2, ComparisonResponse response) {
		ComparisonResult result = new ComparisonResult();
		result.setFile1Id(s1.getId());
		result.setFile2Id(s2.getId());
		result.setFile1Name(s1.getFileName());
		result.setFile2Name(s2.getFileName());
		result.setOverallSimilarity(response.getOverallSimilarity());
		result.setTokenSimilarity(response.getTokenSimilarity());
		result.setStructuralSimilarity(response.getStructuralSimilarity());
		result.setStatus(response.getStatus());
		result.setComparedAt(LocalDateTime.now());

		// Store matching segments as JSON string (simple approach)
		StringBuilder matchesJson = new StringBuilder("[");
		List<MatchSegment> matches = response.getMatches();
		for (int i = 0; i < matches.size(); i++) {
			MatchSegment match = matches.get(i);
			matchesJson.append("{");
			matchesJson.append("\"line1Range\":\"").append(match.getLine1Range()).append("\",");
			matchesJson.append("\"line2Range\":\"").append(match.getLine2Range()).append("\",");
			matchesJson.append("\"similarity\":").append(match.getSimilarity());
			matchesJson.append("}");
			if (i < matches.size() - 1) {
				matchesJson.append(",");
			}
		}
		matchesJson.append("]");
		result.setMatchingSegments(matchesJson.toString());

		comparisonResultRepository.save(result);
	}

	/**
	 * Get all submissions
	 */
	@GetMapping("/submissions")
	public ResponseEntity<List<Submission>> getAllSubmissions() {
		try {
			List<Submission> submissions = submissionRepository.findAll();
			return ResponseEntity.ok(submissions);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Get submission by ID
	 */
	@GetMapping("/submissions/{id}")
	public ResponseEntity<?> getSubmissionById(@PathVariable Long id) {
		try {
			Optional<Submission> submission = submissionRepository.findById(id);
			if (submission.isPresent()) {
				return ResponseEntity.ok(submission.get());
			} else {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Submission not found");
			}
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Get all comparison results
	 */
	@GetMapping("/results")
	public ResponseEntity<List<ComparisonResult>> getAllResults() {
		try {
			List<ComparisonResult> results = comparisonResultRepository.findAll();
			return ResponseEntity.ok(results);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Get comparison result by ID
	 */
	@GetMapping("/results/{id}")
	public ResponseEntity<?> getResultById(@PathVariable Long id) {
		try {
			Optional<ComparisonResult> result = comparisonResultRepository.findById(id);
			if (result.isPresent()) {
				return ResponseEntity.ok(result.get());
			} else {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Result not found");
			}
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Get high risk comparisons
	 */
	@GetMapping("/results/high-risk")
	public ResponseEntity<List<ComparisonResult>> getHighRiskComparisons() {
		try {
			List<ComparisonResult> results = comparisonResultRepository.findHighRiskComparisons();
			return ResponseEntity.ok(results);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Get statistics
	 */
	@GetMapping("/statistics")
	public ResponseEntity<?> getStatistics() {
		try {
			long totalSubmissions = submissionRepository.count();
			long totalComparisons = comparisonResultRepository.count();
			Double avgSimilarity = comparisonResultRepository.getAverageSimilarity();

			if (avgSimilarity == null) {
				avgSimilarity = 0.0;
			}

			// Create response map
			java.util.Map<String, Object> stats = new java.util.HashMap<>();
			stats.put("totalSubmissions", totalSubmissions);
			stats.put("totalComparisons", totalComparisons);
			stats.put("averageSimilarity", avgSimilarity);

			return ResponseEntity.ok(stats);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Delete submission
	 */
	@DeleteMapping("/submissions/{id}")
	public ResponseEntity<?> deleteSubmission(@PathVariable Long id) {
		try {
			if (submissionRepository.existsById(id)) {
				submissionRepository.deleteById(id);
				return ResponseEntity.ok("Submission deleted successfully");
			} else {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Submission not found");
			}
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Delete failed: " + e.getMessage());
		}
	}

	/**
	 * Detect programming language from filename
	 */
	private String detectLanguage(String filename) {
		if (filename == null) {
			return "unknown";
		}

		String lowerFilename = filename.toLowerCase();

		if (lowerFilename.endsWith(".java")) {
			return "java";
		} else if (lowerFilename.endsWith(".py")) {
			return "python";
		} else if (lowerFilename.endsWith(".cpp") || lowerFilename.endsWith(".c")) {
			return "cpp";
		} else if (lowerFilename.endsWith(".js")) {
			return "javascript";
		} else {
			return "unknown";
		}
	}

	@PostMapping("/generate-report")
	public ResponseEntity<byte[]> generateReport(@RequestBody AnalysisRequest request) {
		try {
			List<ComparisonResponse> results = new ArrayList<>();
			List<Long> ids = request.getSubmissionIds();

			// Get results
			for (int i = 0; i < ids.size() - 1; i++) {
				for (int j = i + 1; j < ids.size(); j++) {
					Optional<Submission> sub1Opt = submissionRepository.findById(ids.get(i));
					Optional<Submission> sub2Opt = submissionRepository.findById(ids.get(j));

					if (sub1Opt.isPresent() && sub2Opt.isPresent()) {
						results.add(compareSubmissions(sub1Opt.get(), sub2Opt.get()));
					}
				}
			}

			// Generate PDF
			byte[] pdfBytes = generatePdfReport(results);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			headers.set(HttpHeaders.CONTENT_DISPOSITION,
					"attachment; filename=\"plagiarism-report-" + System.currentTimeMillis() + ".pdf\"");
			headers.setContentLength(pdfBytes.length);

			return ResponseEntity.ok().headers(headers).body(pdfBytes);

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}

	private byte[] generatePdfReport(List<ComparisonResponse> results) throws Exception {
		Document document = new Document(PageSize.A4);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PdfWriter.getInstance(document, baos);

		document.open();

		// Title
		Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLUE);
		Paragraph title = new Paragraph("CODE PLAGIARISM ANALYSIS REPORT", titleFont);
		title.setAlignment(Element.ALIGN_CENTER);
		title.setSpacingAfter(20);
		document.add(title);

		// Generation info
		Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
		document.add(new Paragraph("Generated: " + LocalDateTime.now(), normalFont));
		document.add(new Paragraph("Total Comparisons: " + results.size(), normalFont));
		document.add(new Paragraph(" ", normalFont)); // Spacer

		// Summary
		long highRisk = results.stream().filter(r -> "HIGH".equals(r.getStatus())).count();
		long mediumRisk = results.stream().filter(r -> "MEDIUM".equals(r.getStatus())).count();
		long lowRisk = results.stream().filter(r -> "LOW".equals(r.getStatus())).count();

		Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
		document.add(new Paragraph("Summary:", boldFont));
		document.add(new Paragraph("  High Risk Cases: " + highRisk, normalFont));
		document.add(new Paragraph("  Medium Risk Cases: " + mediumRisk, normalFont));
		document.add(new Paragraph("  Low Risk Cases: " + lowRisk, normalFont));
		document.add(new Paragraph(" ", normalFont)); // Spacer

		// Results
		document.add(new Paragraph("DETAILED RESULTS", boldFont));
		document.add(new Paragraph(" ", normalFont)); // Spacer

		int count = 1;
		for (ComparisonResponse result : results) {
			document.add(new Paragraph("Comparison " + count++, boldFont));
			document.add(new Paragraph("  File 1: " + result.getFile1Name(), normalFont));
			document.add(new Paragraph("  File 2: " + result.getFile2Name(), normalFont));
			document.add(new Paragraph(
					"  Overall Similarity: " + String.format("%.2f%%", result.getOverallSimilarity()), normalFont));
			document.add(new Paragraph("  Status: " + result.getStatus() + " RISK", normalFont));
			document.add(new Paragraph("  Token Similarity: " + String.format("%.2f%%", result.getTokenSimilarity()),
					normalFont));
			document.add(new Paragraph(
					"  Structural Similarity: " + String.format("%.2f%%", result.getStructuralSimilarity()),
					normalFont));
			document.add(new Paragraph(" ", normalFont)); // Spacer
		}

		document.close();

		return baos.toByteArray();
	}

	/**
	 * Health check endpoint
	 */
	@GetMapping("/health")
	public ResponseEntity<String> healthCheck() {
		return ResponseEntity.ok("Server is running");
	}
}