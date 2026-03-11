package com.plagiarism.detector.controller;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
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
@RequestMapping("/cpd")
//@CrossOrigin(origins = { "http://localhost:4200", "http://localhost:3000" })
public class PlagiarismController {

	private static final BaseColor COLOR_PRIMARY = new BaseColor(0x1E, 0x27, 0x61); // #1E2761 navy
	private static final BaseColor COLOR_ACCENT = new BaseColor(0x66, 0x7E, 0xEA); // #667eea purple-blue
	private static final BaseColor COLOR_PURPLE = new BaseColor(0x76, 0x4B, 0xA2); // #764ba2
	private static final BaseColor COLOR_HIGH = new BaseColor(0xF4, 0x43, 0x36); // red
	private static final BaseColor COLOR_MEDIUM = new BaseColor(0xFF, 0x98, 0x00); // orange
	private static final BaseColor COLOR_LOW = new BaseColor(0x4C, 0xAF, 0x50); // green
	private static final BaseColor COLOR_WHITE = BaseColor.WHITE;
	private static final BaseColor COLOR_LIGHT_GRAY = new BaseColor(0xF5, 0xF5, 0xF5);
	private static final BaseColor COLOR_MID_GRAY = new BaseColor(0xE0, 0xE0, 0xE0);
	private static final BaseColor COLOR_TEXT = new BaseColor(0x21, 0x21, 0x21);
	private static final BaseColor COLOR_SUBTEXT = new BaseColor(0x75, 0x75, 0x75);

	// ─────────────────────────────────────────────────────────────────────────────
	// FONTS
	// ─────────────────────────────────────────────────────────────────────────────
	private static final Font FONT_SECTION = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, COLOR_PRIMARY);
	private static final Font FONT_LABEL = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, COLOR_SUBTEXT);
	private static final Font FONT_VALUE_BOLD = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, COLOR_TEXT);
	private static final Font FONT_TABLE_HEADER = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, COLOR_WHITE);
	private static final Font FONT_TABLE_CELL = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, COLOR_TEXT);
	private static final Font FONT_FOOTER = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, COLOR_SUBTEXT);

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
				var content = new String(file.getBytes());

				// Detect language
				String language = detectLanguage(file.getOriginalFilename());

				// Create submission entity
				var submission = new Submission();
				submission.setFileName(file.getOriginalFilename());
				submission.setFileContent(content);
				submission.setLanguage(language);
				submission.setUploadedAt(LocalDateTime.now());
				submission.setUploadedBy(username);

				// Save to database
				Submission savedSubmission = submissionRepository.save(submission);
				ids.add(savedSubmission.getId());
			}

			var response = new UploadResponse("Files uploaded successfully", ids);
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
			for (var i = 0; i < ids.size() - 1; i++) {
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
		var structuralSim = 0.0;

		if ("java".equalsIgnoreCase(language)) {
			structuralSim = detectionService.calculateStructuralSimilarity(code1, code2);
		}

		double overallSim = detectionService.calculateOverallSimilarity(code1, code2, language);

		// Find matching segments
		List<MatchSegment> matches = detectionService.findMatchingSegments(code1, code2);

		// Determine status
		String status = detectionService.determineStatus(overallSim);

		// Create response
		var response = new ComparisonResponse();
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
		var result = new ComparisonResult();
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
		var matchesJson = new StringBuilder("[");
		List<MatchSegment> matches = response.getMatches();
		for (var i = 0; i < matches.size(); i++) {
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

			for (var i = 0; i < ids.size() - 1; i++) {
				for (int j = i + 1; j < ids.size(); j++) {
					Optional<Submission> sub1Opt = submissionRepository.findById(ids.get(i));
					Optional<Submission> sub2Opt = submissionRepository.findById(ids.get(j));
					if (sub1Opt.isPresent() && sub2Opt.isPresent()) {
						results.add(compareSubmissions(sub1Opt.get(), sub2Opt.get()));
					}
				}
			}

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
		var document = new Document(PageSize.A4, 40, 40, 60, 60);
		var baos = new ByteArrayOutputStream();
		var writer = PdfWriter.getInstance(document, baos);

		// Header / Footer event handler
		writer.setPageEvent(new PdfPageEventHelper() {
			@Override
			public void onStartPage(PdfWriter w, Document doc) {
				// Top gradient band
				PdfContentByte cb = w.getDirectContentUnder();
				cb.saveState();
				cb.setColorFill(COLOR_PRIMARY);
				cb.rectangle(0, PageSize.A4.getHeight() - 45, PageSize.A4.getWidth(), 45);
				cb.fill();
				cb.restoreState();
			}

			@Override
			public void onEndPage(PdfWriter w, Document doc) {
				// Footer line + text
				PdfContentByte cb = w.getDirectContent();
				cb.saveState();
				cb.setColorStroke(COLOR_MID_GRAY);
				cb.setLineWidth(0.5f);
				cb.moveTo(40, 38);
				cb.lineTo(PageSize.A4.getWidth() - 40, 38);
				cb.stroke();

				ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
						new Phrase("AI Code Plagiarism Detector  |  Anna University CDE", FONT_FOOTER), 40, 25, 0);
				ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
						new Phrase("Page " + w.getPageNumber(), FONT_FOOTER), PageSize.A4.getWidth() - 40, 25, 0);
				cb.restoreState();
			}
		});

		document.open();

		// ── COVER BLOCK ──────────────────────────────────────────────────────────
		PdfContentByte canvas = writer.getDirectContentUnder();

		// Full-width hero gradient rectangle (draws at page top)
		canvas.saveState();
		// Simulate gradient with two overlapping rects
		canvas.setColorFill(COLOR_PRIMARY);
		canvas.rectangle(0, PageSize.A4.getHeight() - 160, PageSize.A4.getWidth(), 160);
		canvas.fill();
		canvas.setColorFill(COLOR_ACCENT);
		canvas.setColorFill(new BaseColor(102, 126, 234, 80)); // semi-transparent accent
		canvas.rectangle(PageSize.A4.getWidth() / 2, PageSize.A4.getHeight() - 160, PageSize.A4.getWidth() / 2, 160);
		canvas.fill();
		// Bottom accent stripe
		canvas.setColorFill(COLOR_PURPLE);
		canvas.rectangle(0, PageSize.A4.getHeight() - 163, PageSize.A4.getWidth(), 3);
		canvas.fill();
		canvas.restoreState();

		// Overlay title text on the hero
		PdfContentByte directContent = writer.getDirectContent();
		var heroTitle = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, COLOR_WHITE);
		var heroSub = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, new BaseColor(220, 225, 255));
		ColumnText.showTextAligned(directContent, Element.ALIGN_LEFT,
				new Phrase("CODE PLAGIARISM ANALYSIS REPORT", heroTitle), 40, PageSize.A4.getHeight() - 90, 0);
		ColumnText.showTextAligned(directContent, Element.ALIGN_LEFT,
				new Phrase("AI-Powered Detection System  |  Anna University, Centre for Distance Education", heroSub),
				40, PageSize.A4.getHeight() - 110, 0);

		String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
		ColumnText.showTextAligned(directContent, Element.ALIGN_LEFT, new Phrase("Generated: " + ts, FONT_FOOTER), 40,
				PageSize.A4.getHeight() - 130, 0);

		// Spacer to push content below hero
		document.add(new Paragraph("\n\n\n\n\n\n\n"));

		// ── SUMMARY STATS ────────────────────────────────────────────────────────
		document.add(sectionHeading("Analysis Summary"));

		long highRisk = results.stream().filter(r -> "HIGH".equals(r.getStatus())).count();
		long mediumRisk = results.stream().filter(r -> "MEDIUM".equals(r.getStatus())).count();
		long lowRisk = results.stream().filter(r -> "LOW".equals(r.getStatus())).count();
		double avgSim = results.isEmpty() ? 0
				: results.stream().mapToDouble(ComparisonResponse::getOverallSimilarity).average().orElse(0);

		// 5-column stat table
		var statTable = new PdfPTable(5);
		statTable.setWidthPercentage(100);
		statTable.setSpacingBefore(8);
		statTable.setSpacingAfter(16);

		addStatCell(statTable, String.valueOf(results.size()), "Total\nComparisons", COLOR_ACCENT);
		addStatCell(statTable, String.valueOf(highRisk), "High\nRisk", COLOR_HIGH);
		addStatCell(statTable, String.valueOf(mediumRisk), "Medium\nRisk", COLOR_MEDIUM);
		addStatCell(statTable, String.valueOf(lowRisk), "Low\nRisk", COLOR_LOW);
		addStatCell(statTable, String.format("%.1f%%", avgSim), "Avg\nSimilarity", COLOR_PRIMARY);
		document.add(statTable);

		// Divider
		document.add(divider());

		// ── DETAILED RESULTS ─────────────────────────────────────────────────────
		document.add(sectionHeading("Detailed Comparison Results"));

		for (var i = 0; i < results.size(); i++) {
			ComparisonResponse r = results.get(i);
			document.add(comparisonBlock(i + 1, r));
			if (i < results.size() - 1) {
				document.add(new Paragraph("\n"));
			}
		}

		// ── FOOTER NOTE ──────────────────────────────────────────────────────────
		document.add(divider());
		Paragraph note = new Paragraph(
				"This report was automatically generated by the AI Code Plagiarism Detection System. "
						+ "Similarity scores are computed using Token-Based and AST Structural analysis algorithms. "
						+ "Results should be reviewed by a faculty member before taking academic action.",
				new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, COLOR_SUBTEXT));
		note.setSpacingBefore(6);
		document.add(note);

		document.close();
		return baos.toByteArray();
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// HELPER: Section heading with left accent bar
	// ─────────────────────────────────────────────────────────────────────────────
	private Paragraph sectionHeading(String text) {
		var p = new Paragraph(text, FONT_SECTION);
		p.setSpacingBefore(10);
		p.setSpacingAfter(6);
		// Left border via chunk with background
		p.setIndentationLeft(8);
		return p;
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// HELPER: Thin divider line
	// ─────────────────────────────────────────────────────────────────────────────
	private Paragraph divider() {
		var p = new Paragraph();
		p.add(new Chunk(new LineSeparator(0.5f, 100, COLOR_MID_GRAY, Element.ALIGN_CENTER, -2)));
		p.setSpacingBefore(4);
		p.setSpacingAfter(4);
		return p;
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// HELPER: Stat box cell
	// ─────────────────────────────────────────────────────────────────────────────
	private void addStatCell(PdfPTable table, String value, String label, BaseColor color) {
		var cell = new PdfPCell();
		cell.setBorder(Rectangle.NO_BORDER);
		cell.setBackgroundColor(color);
		cell.setPadding(12);

		Paragraph val = new Paragraph(value, new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, COLOR_WHITE));
		val.setAlignment(Element.ALIGN_CENTER);
		cell.addElement(val);

		Paragraph lbl = new Paragraph(label,
				new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, new BaseColor(220, 225, 255)));
		lbl.setAlignment(Element.ALIGN_CENTER);
		cell.addElement(lbl);

		table.addCell(cell);
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// HELPER: Full comparison block (card-style)
	// ─────────────────────────────────────────────────────────────────────────────
	private PdfPTable comparisonBlock(int index, ComparisonResponse r) throws Exception {
		// Outer wrapper table (1 col, gives card border)
		var wrapper = new PdfPTable(1);
		wrapper.setWidthPercentage(100);
		wrapper.setSpacingBefore(8);

		var card = new PdfPCell();
		card.setBorderColor(COLOR_MID_GRAY);
		card.setBorderWidth(0.5f);
		card.setPadding(0);

		// ── Card header row: "Comparison N" + status badge ──
		var header = new PdfPTable(new float[] { 6f, 2f });
		header.setWidthPercentage(100);

		// Left: title
		var titleCell = new PdfPCell();
		titleCell.setBorder(Rectangle.NO_BORDER);
		titleCell.setBackgroundColor(COLOR_LIGHT_GRAY);
		titleCell.setPadding(10);
		var compTitle = new Paragraph("Comparison " + index,
				new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, COLOR_PRIMARY));
		titleCell.addElement(compTitle);
		header.addCell(titleCell);

		// Right: status badge
		BaseColor badgeColor = "HIGH".equals(r.getStatus()) ? COLOR_HIGH
				: "MEDIUM".equals(r.getStatus()) ? COLOR_MEDIUM : COLOR_LOW;
		var badgeCell = new PdfPCell();
		badgeCell.setBorder(Rectangle.NO_BORDER);
		badgeCell.setBackgroundColor(badgeColor);
		badgeCell.setPadding(10);
		badgeCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		var badge = new Paragraph(r.getStatus() + " RISK",
				new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, COLOR_WHITE));
		badge.setAlignment(Element.ALIGN_CENTER);
		badgeCell.addElement(badge);
		header.addCell(badgeCell);

		// ── File names row ──
		var fileRow = new PdfPTable(new float[] { 5f, 1f, 5f });
		fileRow.setWidthPercentage(100);

		var f1 = fileCell(r.getFile1Name(), true);
		var vs = new PdfPCell(new Phrase("↔", new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, COLOR_ACCENT)));
		vs.setHorizontalAlignment(Element.ALIGN_CENTER);
		vs.setVerticalAlignment(Element.ALIGN_MIDDLE);
		vs.setBorder(Rectangle.NO_BORDER);
		vs.setPadding(8);
		PdfPCell f2 = fileCell(r.getFile2Name(), false);

		fileRow.addCell(f1);
		fileRow.addCell(vs);
		fileRow.addCell(f2);

		// ── Similarity progress bar section ──
		var simSection = new PdfPTable(1);
		simSection.setWidthPercentage(100);
		var simCell = new PdfPCell();
		simCell.setBorder(Rectangle.NO_BORDER);
		simCell.setPadding(10);

		// Label + value
		var simLabelRow = new PdfPTable(new float[] { 7f, 3f });
		simLabelRow.setWidthPercentage(100);
		var simLbl = new PdfPCell(new Phrase("Overall Similarity", FONT_LABEL));
		simLbl.setBorder(Rectangle.NO_BORDER);
		var simVal = new PdfPCell(new Phrase(String.format("%.2f%%", r.getOverallSimilarity()),
				new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, COLOR_ACCENT)));
		simVal.setBorder(Rectangle.NO_BORDER);
		simVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
		simLabelRow.addCell(simLbl);
		simLabelRow.addCell(simVal);
		simCell.addElement(simLabelRow);

		// Progress bar (drawn as two stacked rectangles via a nested table)
		float pct = (float) Math.min(r.getOverallSimilarity(), 100.0);
		var progressOuter = new PdfPTable(1);
		progressOuter.setWidthPercentage(100);
		var bgBar = new PdfPCell();
		bgBar.setBorder(Rectangle.NO_BORDER);
		bgBar.setFixedHeight(8);
		bgBar.setBackgroundColor(COLOR_MID_GRAY);
		bgBar.setPadding(0);

		// Inner filled portion (as nested table with 2 cells by percentage)
		float fillW = pct;
		float emptyW = 100f - fillW;
		if (fillW > 0 && emptyW > 0) {
			var innerBar = new PdfPTable(new float[] { fillW, emptyW });
			innerBar.setWidthPercentage(100);
			var filled = new PdfPCell();
			filled.setBorder(Rectangle.NO_BORDER);
			filled.setFixedHeight(8);
			filled.setBackgroundColor(badgeColor);
			var empty = new PdfPCell();
			empty.setBorder(Rectangle.NO_BORDER);
			empty.setFixedHeight(8);
			empty.setBackgroundColor(COLOR_MID_GRAY);
			innerBar.addCell(filled);
			innerBar.addCell(empty);
			bgBar.addElement(innerBar);
		} else if (fillW >= 100) {
			bgBar.setBackgroundColor(badgeColor);
		}

		progressOuter.addCell(bgBar);
		simCell.addElement(progressOuter);
		simSection.addCell(simCell);

		// ── Algorithm breakdown (2-col) ──
		var algoTable = new PdfPTable(2);
		algoTable.setWidthPercentage(100);

		algoTable.addCell(
				algoCell("Token-Based Similarity", String.format("%.2f%%", r.getTokenSimilarity()), COLOR_ACCENT));
		algoTable.addCell(algoCell("Structural (AST) Similarity", String.format("%.2f%%", r.getStructuralSimilarity()),
				COLOR_PURPLE));

		// ── Matching segments table ──
		PdfPCell matchWrapper = null;
		if (r.getMatches() != null && !r.getMatches().isEmpty()) {
			matchWrapper = new PdfPCell();
			matchWrapper.setBorder(Rectangle.NO_BORDER);
			matchWrapper.setPaddingLeft(10);
			matchWrapper.setPaddingRight(10);
			matchWrapper.setPaddingBottom(10);

			var matchTitle = new Paragraph("Matching Code Segments", FONT_SECTION);
			matchTitle.setSpacingAfter(6);
			matchWrapper.addElement(matchTitle);

			var matchTable = new PdfPTable(new float[] { 4f, 4f, 2f });
			matchTable.setWidthPercentage(100);

			// Header
			addMatchHeader(matchTable, "File 1 Lines");
			addMatchHeader(matchTable, "File 2 Lines");
			addMatchHeader(matchTable, "Similarity");

			// Rows
			var alt = false;
			for (var match : r.getMatches()) {
				BaseColor rowBg = alt ? COLOR_LIGHT_GRAY : COLOR_WHITE;
				alt = !alt;
				addMatchRow(matchTable, "Lines " + match.getLine1Range(), rowBg);
				addMatchRow(matchTable, "Lines " + match.getLine2Range(), rowBg);

				var simChip = new PdfPCell(new Phrase(String.format("%.2f%%", match.getSimilarity()),
						new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, COLOR_WHITE)));
				simChip.setBackgroundColor(COLOR_ACCENT);
				simChip.setHorizontalAlignment(Element.ALIGN_CENTER);
				simChip.setVerticalAlignment(Element.ALIGN_MIDDLE);
				simChip.setPadding(5);
				simChip.setBorder(Rectangle.NO_BORDER);
				matchTable.addCell(simChip);
			}

			matchWrapper.addElement(matchTable);
		}

		// ── Assemble card ──
		card.addElement(header);
		card.addElement(fileRow);
		card.addElement(new Paragraph(" ")); // small spacer
		card.addElement(simSection);
		card.addElement(algoTable);
		if (matchWrapper != null) {
			card.addElement(matchWrapper);
		}

		wrapper.addCell(card);
		return wrapper;
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// HELPER: File name cell
	// ─────────────────────────────────────────────────────────────────────────────
	private PdfPCell fileCell(String name, boolean isLeft) {
		var cell = new PdfPCell();
		cell.setBorder(Rectangle.NO_BORDER);
		cell.setBackgroundColor(COLOR_LIGHT_GRAY);
		cell.setPadding(10);
		cell.setHorizontalAlignment(isLeft ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);

		var p = new Paragraph("📄 " + name, FONT_VALUE_BOLD);
		p.setAlignment(isLeft ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
		cell.addElement(p);
		return cell;
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// HELPER: Algorithm score cell
	// ─────────────────────────────────────────────────────────────────────────────
	private PdfPCell algoCell(String label, String value, BaseColor accentColor) {
		var cell = new PdfPCell();
		cell.setBorder(Rectangle.LEFT);
		cell.setBorderColorLeft(accentColor);
		cell.setBorderWidthLeft(3f);
		cell.setBackgroundColor(COLOR_LIGHT_GRAY);
		cell.setPadding(10);
		cell.setPaddingLeft(14);

		var lbl = new Paragraph(label, FONT_LABEL);
		lbl.setSpacingAfter(4);
		cell.addElement(lbl);

		var val = new Paragraph(value, new Font(Font.FontFamily.HELVETICA, 15, Font.BOLD, accentColor));
		cell.addElement(val);
		return cell;
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// HELPER: Match table header cell
	// ─────────────────────────────────────────────────────────────────────────────
	private void addMatchHeader(PdfPTable table, String text) {
		var cell = new PdfPCell(new Phrase(text, FONT_TABLE_HEADER));
		cell.setBackgroundColor(COLOR_PRIMARY);
		cell.setPadding(7);
		cell.setBorder(Rectangle.NO_BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// HELPER: Match table data cell
	// ─────────────────────────────────────────────────────────────────────────────
	private void addMatchRow(PdfPTable table, String text, BaseColor bg) {
		var cell = new PdfPCell(new Phrase(text, FONT_TABLE_CELL));
		cell.setBackgroundColor(bg);
		cell.setPadding(6);
		cell.setBorder(Rectangle.BOTTOM);
		cell.setBorderColor(COLOR_MID_GRAY);
		cell.setBorderWidth(0.3f);
		table.addCell(cell);
	}

	/**
	 * Health check endpoint
	 */
	@GetMapping("/health")
	public ResponseEntity<String> healthCheck() {
		return ResponseEntity.ok("Server is running");
	}
}