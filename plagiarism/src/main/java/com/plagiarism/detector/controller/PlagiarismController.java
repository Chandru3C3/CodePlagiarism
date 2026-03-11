package com.plagiarism.detector.controller;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

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

	private static final BaseColor C_NAVY = new BaseColor(0x1E, 0x27, 0x61);
	private static final BaseColor C_ACCENT = new BaseColor(0x66, 0x7E, 0xEA);
	private static final BaseColor C_PURPLE = new BaseColor(0x76, 0x4B, 0xA2);
	private static final BaseColor C_HIGH = new BaseColor(0xF4, 0x43, 0x36);
	private static final BaseColor C_MEDIUM = new BaseColor(0xFF, 0x98, 0x00);
	private static final BaseColor C_LOW = new BaseColor(0x4C, 0xAF, 0x50);
	private static final BaseColor C_WHITE = BaseColor.WHITE;
	private static final BaseColor C_LGRAY = new BaseColor(0xF5, 0xF5, 0xF5);
	private static final BaseColor C_MGRAY = new BaseColor(0xE0, 0xE0, 0xE0);
	private static final BaseColor C_TEXT = new BaseColor(0x21, 0x21, 0x21);
	private static final BaseColor C_SUBTEXT = new BaseColor(0x75, 0x75, 0x75);

	private Font f(int size, int style, BaseColor color) {
		return new Font(Font.FontFamily.HELVETICA, size, style, color);
	}

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
	public ResponseEntity<StreamingResponseBody> generateReport(@RequestBody AnalysisRequest request) {

		List<Long> ids = request.getSubmissionIds();

		if (ids == null || ids.size() < 2) {
			return ResponseEntity.badRequest().build();
		}

		StreamingResponseBody stream = outputStream -> {
			try {
				// ── Collect results safely (skip any failing pair) ────────────
				List<ComparisonResponse> results = new ArrayList<>();
				List<String> errors = new ArrayList<>();

				for (int i = 0; i < ids.size() - 1; i++) {
					for (int j = i + 1; j < ids.size(); j++) {
						try {
							Optional<Submission> s1 = submissionRepository.findById(ids.get(i));
							Optional<Submission> s2 = submissionRepository.findById(ids.get(j));
							if (s1.isPresent() && s2.isPresent()) {
								results.add(compareSubmissions(s1.get(), s2.get()));
							}
						} catch (Exception pairEx) {
							// Log but continue — don't let one bad pair break the whole report
							String msg = "Skipped pair (" + ids.get(i) + "," + ids.get(j) + "): " + pairEx.getMessage();
							errors.add(msg);
							System.err.println("[Report] " + msg);
						}
					}
				}

				// ── Stream PDF directly to response ──────────────────────────
				writePdfToStream(results, errors, outputStream);
				outputStream.flush();

			} catch (Exception e) {
				e.printStackTrace();
				// Write a minimal error PDF so the browser gets a valid response
				try {
					writeErrorPdf("PDF generation failed: " + e.getMessage(), outputStream);
					outputStream.flush();
				} catch (Exception ignored) {
				}
			}
		};

		String filename = "plagiarism-report-" + System.currentTimeMillis() + ".pdf";
		return ResponseEntity.ok().header("Content-Type", "application/pdf")
				.header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
				.header("Cache-Control", "no-cache").body(stream);
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// MAIN PDF WRITER (streams directly to OutputStream — no ByteArrayOutputStream)
	// ─────────────────────────────────────────────────────────────────────────────
	private void writePdfToStream(List<ComparisonResponse> results, List<String> skippedErrors, OutputStream out)
			throws Exception {

		Document doc = new Document(PageSize.A4, 40, 40, 60, 60);
		PdfWriter writer = PdfWriter.getInstance(doc, out);

		// ── Page event: running header + footer ──────────────────────────────
		writer.setPageEvent(new PdfPageEventHelper() {
			@Override
			public void onStartPage(PdfWriter w, Document d) {
				PdfContentByte cb = w.getDirectContentUnder();
				cb.saveState();
				cb.setColorFill(C_NAVY);
				cb.rectangle(0, PageSize.A4.getHeight() - 42, PageSize.A4.getWidth(), 42);
				cb.fill();
				cb.setColorFill(C_ACCENT);
				cb.rectangle(PageSize.A4.getWidth() - 150, PageSize.A4.getHeight() - 42, 150, 42);
				cb.fill();
				cb.restoreState();
				// Header text
				PdfContentByte ct = w.getDirectContent();
				ColumnText.showTextAligned(ct, Element.ALIGN_LEFT,
						new Phrase("AI Code Plagiarism Detector",
								new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, C_WHITE)),
						50, PageSize.A4.getHeight() - 26, 0);
			}

			@Override
			public void onEndPage(PdfWriter w, Document d) {
				PdfContentByte cb = w.getDirectContent();
				// Footer rule
				cb.saveState();
				cb.setColorStroke(C_MGRAY);
				cb.setLineWidth(0.5f);
				cb.moveTo(40, 36);
				cb.lineTo(PageSize.A4.getWidth() - 40, 36);
				cb.stroke();
				cb.restoreState();
				ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
						new Phrase("Anna University – Centre for Distance Education",
								new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL, C_SUBTEXT)),
						40, 24, 0);
				ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
						new Phrase("Page " + w.getPageNumber(),
								new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL, C_SUBTEXT)),
						PageSize.A4.getWidth() - 40, 24, 0);
			}
		});

		doc.open();

		// ── HERO BANNER ──────────────────────────────────────────────────────
		PdfContentByte canvas = writer.getDirectContentUnder();
		canvas.saveState();
		canvas.setColorFill(C_NAVY);
		canvas.rectangle(0, PageSize.A4.getHeight() - 160, PageSize.A4.getWidth(), 118);
		canvas.fill();
		canvas.setColorFill(C_ACCENT);
		canvas.rectangle(PageSize.A4.getWidth() / 2, PageSize.A4.getHeight() - 160, PageSize.A4.getWidth() / 2, 118);
		canvas.fill();
		canvas.setColorFill(C_PURPLE);
		canvas.rectangle(0, PageSize.A4.getHeight() - 163, PageSize.A4.getWidth(), 3);
		canvas.fill();
		canvas.restoreState();

		PdfContentByte dc = writer.getDirectContent();
		ColumnText.showTextAligned(dc, Element.ALIGN_LEFT,
				new Phrase("CODE PLAGIARISM ANALYSIS REPORT",
						new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, C_WHITE)),
				40, PageSize.A4.getHeight() - 95, 0);

		String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
		ColumnText.showTextAligned(dc, Element.ALIGN_LEFT,
				new Phrase("Generated: " + ts + "   |   Total comparisons: " + results.size(),
						new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, new BaseColor(210, 220, 255))),
				40, PageSize.A4.getHeight() - 115, 0);

		// Spacer below hero
		doc.add(new Paragraph("\n\n\n\n\n\n\n"));

		// ── SUMMARY STATS ────────────────────────────────────────────────────
		doc.add(sectionHeading("Analysis Summary"));

		long high = results.stream().filter(r -> "HIGH".equals(safe(r.getStatus()))).count();
		long medium = results.stream().filter(r -> "MEDIUM".equals(safe(r.getStatus()))).count();
		long low = results.stream().filter(r -> "LOW".equals(safe(r.getStatus()))).count();
		double avg = results.isEmpty() ? 0
				: results.stream().mapToDouble(r -> r.getOverallSimilarity()).average().orElse(0);

		PdfPTable stats = new PdfPTable(5);
		stats.setWidthPercentage(100);
		stats.setSpacingBefore(8);
		stats.setSpacingAfter(14);
		addStatCell(stats, String.valueOf(results.size()), "Total\nComparisons", C_ACCENT);
		addStatCell(stats, String.valueOf(high), "High\nRisk", C_HIGH);
		addStatCell(stats, String.valueOf(medium), "Medium\nRisk", C_MEDIUM);
		addStatCell(stats, String.valueOf(low), "Low\nRisk", C_LOW);
		addStatCell(stats, String.format("%.1f%%", avg), "Avg\nSimilarity", C_NAVY);
		doc.add(stats);

		// Warn about any skipped pairs
		if (!skippedErrors.isEmpty()) {
			Paragraph warn = new Paragraph(
					"⚠  " + skippedErrors.size()
							+ " comparison(s) were skipped due to errors and excluded from this report.",
					new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC, C_MEDIUM));
			warn.setSpacingAfter(8);
			doc.add(warn);
		}

		doc.add(divider());

		// ── DETAILED RESULTS (streamed one card at a time) ──────────────────
		doc.add(sectionHeading("Detailed Comparison Results"));

		// Locate this loop in your writePdfToStream method
		for (int i = 0; i < results.size(); i++) {
		    try {
		        // Fix: Capture the list of elements and add them one by one
		        List<Element> cardElements = buildComparisonCard(i + 1, results.get(i));
		        for (Element element : cardElements) {
		            doc.add(element);
		        }
		    } catch (Exception cardEx) {
		        System.err.println("[Report] Card " + (i + 1) + " failed: " + cardEx.getMessage());
		        Paragraph errP = new Paragraph(
		                "⚠  Comparison " + (i + 1) + " could not be rendered (" + cardEx.getMessage() + ")",
		                new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC, C_HIGH));
		        doc.add(errP);
		    }

		    if ((i + 1) % 10 == 0) {
		        writer.flush();
		    }
		}

		// ── DISCLAIMER ───────────────────────────────────────────────────────
		doc.add(divider());
		doc.add(new Paragraph(
				"This report was auto-generated by the AI Code Plagiarism Detection System. "
						+ "Scores use Token-Based + AST Structural algorithms. "
						+ "Results should be reviewed by faculty before academic action.",
				new Font(Font.FontFamily.HELVETICA, 7, Font.ITALIC, C_SUBTEXT)));

		doc.close();
	}

	private String safe(String s) {
		return s == null ? "" : s;
	}

	private double safeDbl(Double d) {
		return d == null ? 0.0 : d;
	}

	private List<Element> buildComparisonCard(int idx, ComparisonResponse r) throws Exception {
		List<Element> elements = new ArrayList<>();

		String status = safe(r.getStatus()).toUpperCase();
		double overall = safeDbl(r.getOverallSimilarity());
		double token = safeDbl(r.getTokenSimilarity());
		double ast = safeDbl(r.getStructuralSimilarity());
		String f1 = safe(r.getFile1Name());
		String f2 = safe(r.getFile2Name());

		BaseColor badge = "HIGH".equals(status) ? C_HIGH : "MEDIUM".equals(status) ? C_MEDIUM : C_LOW;

		// ── 1. Header row: "Comparison N" | "STATUS RISK" ─────────────────────
		PdfPTable header = new PdfPTable(new float[] { 7f, 2.5f });
		header.setWidthPercentage(100);
		header.setSpacingBefore(12);

		PdfPCell titleCell = new PdfPCell(
				new Phrase("Comparison " + idx, new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, C_NAVY)));
		titleCell.setBackgroundColor(C_LGRAY);
		titleCell.setPadding(10);
		titleCell.setBorder(Rectangle.TOP | Rectangle.LEFT | Rectangle.BOTTOM);
		titleCell.setBorderColor(C_MGRAY);
		header.addCell(titleCell);

		Phrase badgePhrase = new Phrase(status + " RISK", new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, C_WHITE));
		PdfPCell badgeCell = new PdfPCell(badgePhrase);
		badgeCell.setBackgroundColor(badge);
		badgeCell.setPadding(10);
		badgeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
		badgeCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		badgeCell.setBorder(Rectangle.TOP | Rectangle.RIGHT | Rectangle.BOTTOM);
		badgeCell.setBorderColor(C_MGRAY);
		header.addCell(badgeCell);
		elements.add(header);

		// ── 2. File names row: file1 | ↔ | file2 ────────────────────────────
		PdfPTable files = new PdfPTable(new float[] { 5f, 1f, 5f });
		files.setWidthPercentage(100);

		PdfPCell fc1 = new PdfPCell(
				new Phrase("  \uD83D\uDCC4  " + f1, new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, C_TEXT)));
		fc1.setBackgroundColor(new BaseColor(0xEE, 0xF1, 0xFB));
		fc1.setPadding(9);
		fc1.setBorder(Rectangle.LEFT | Rectangle.BOTTOM);
		fc1.setBorderColor(C_MGRAY);
		files.addCell(fc1);

		PdfPCell vsCell = new PdfPCell(
				new Phrase("\u2194", new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, C_ACCENT)));
		vsCell.setHorizontalAlignment(Element.ALIGN_CENTER);
		vsCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		vsCell.setPadding(8);
		vsCell.setBorder(Rectangle.BOTTOM);
		vsCell.setBorderColor(C_MGRAY);
		files.addCell(vsCell);

		PdfPCell fc2 = new PdfPCell(
				new Phrase("\uD83D\uDCC4  " + f2 + "  ", new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, C_TEXT)));
		fc2.setBackgroundColor(new BaseColor(0xEE, 0xF1, 0xFB));
		fc2.setPadding(9);
		fc2.setHorizontalAlignment(Element.ALIGN_RIGHT);
		fc2.setBorder(Rectangle.RIGHT | Rectangle.BOTTOM);
		fc2.setBorderColor(C_MGRAY);
		files.addCell(fc2);
		elements.add(files);

		// ── 3. Similarity label + percentage ─────────────────────────────────────
		PdfPTable simLabel = new PdfPTable(new float[] { 7f, 3f });
		simLabel.setWidthPercentage(100);

		PdfPCell slCell = new PdfPCell(
				new Phrase("Overall Similarity", new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, C_SUBTEXT)));
		slCell.setPadding(10);
		slCell.setPaddingBottom(2);
		slCell.setBorder(Rectangle.LEFT);
		slCell.setBorderColor(C_MGRAY);
		simLabel.addCell(slCell);

		PdfPCell spCell = new PdfPCell(new Phrase(String.format("%.2f%%", overall),
				new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, C_ACCENT)));
		spCell.setPadding(10);
		spCell.setPaddingBottom(2);
		spCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		spCell.setBorder(Rectangle.RIGHT);
		spCell.setBorderColor(C_MGRAY);
		simLabel.addCell(spCell);
		elements.add(simLabel);

		// ── 4. Progress bar (two-cell table simulating fill) ─────────────────────
		float fill = (float) Math.max(0.5, Math.min(overall, 99.5)); // avoid 0 or 100 cols
		float empty = 100f - fill;
		PdfPTable bar = new PdfPTable(new float[] { fill, empty });
		bar.setWidthPercentage(100);

		PdfPCell filledCell = new PdfPCell(new Phrase(""));
		filledCell.setBackgroundColor(badge);
		filledCell.setFixedHeight(8);
		filledCell.setBorder(Rectangle.LEFT);
		filledCell.setBorderColor(C_MGRAY);
		bar.addCell(filledCell);

		PdfPCell emptyCell = new PdfPCell(new Phrase(""));
		emptyCell.setBackgroundColor(C_MGRAY);
		emptyCell.setFixedHeight(8);
		emptyCell.setBorder(Rectangle.RIGHT);
		emptyCell.setBorderColor(C_MGRAY);
		bar.addCell(emptyCell);
		elements.add(bar);

		// ── 5. Algorithm breakdown ────────────────────────────────────────────────
		PdfPTable algo = new PdfPTable(2);
		algo.setWidthPercentage(100);
		algo.setSpacingBefore(2);
		algo.addCell(algoCell("Token-Based Similarity", String.format("%.2f%%", token), C_ACCENT, true));
		algo.addCell(algoCell("Structural (AST) Similarity", String.format("%.2f%%", ast), C_PURPLE, false));
		elements.add(algo);

		// ── 6. Matching segments table ────────────────────────────────────────────
		if (r.getMatches() != null && !r.getMatches().isEmpty()) {
			// Section label
			Paragraph mTitle = new Paragraph("  Matching Code Segments",
					new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, C_NAVY));
			mTitle.setSpacingBefore(4);
			mTitle.setSpacingAfter(4);
			elements.add(mTitle);

			PdfPTable mt = new PdfPTable(new float[] { 4f, 4f, 2f });
			mt.setWidthPercentage(100);

			// Header
			for (String h : new String[] { "File 1 Lines", "File 2 Lines", "Similarity" }) {
				PdfPCell mh = new PdfPCell(new Phrase(h, new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, C_WHITE)));
				mh.setBackgroundColor(C_NAVY);
				mh.setPadding(6);
				mh.setBorder(Rectangle.NO_BORDER);
				mh.setHorizontalAlignment(Element.ALIGN_CENTER);
				mt.addCell(mh);
			}

			// Rows
			boolean alt = false;
			for (var m : r.getMatches()) {
				BaseColor rowBg = alt ? C_LGRAY : C_WHITE;
				alt = !alt;

				for (String txt : new String[] { "Lines " + safe(m.getLine1Range()),
						"Lines " + safe(m.getLine2Range()) }) {
					PdfPCell mc = new PdfPCell(
							new Phrase(txt, new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, C_TEXT)));
					mc.setBackgroundColor(rowBg);
					mc.setPadding(6);
					mc.setBorder(Rectangle.BOTTOM);
					mc.setBorderColor(C_MGRAY);
					mc.setBorderWidth(0.3f);
					mt.addCell(mc);
				}

				PdfPCell sc = new PdfPCell(new Phrase(String.format("%.2f%%", safeDbl(m.getSimilarity())),
						new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, C_WHITE)));
				sc.setBackgroundColor(C_ACCENT);
				sc.setHorizontalAlignment(Element.ALIGN_CENTER);
				sc.setVerticalAlignment(Element.ALIGN_MIDDLE);
				sc.setPadding(6);
				sc.setBorder(Rectangle.NO_BORDER);
				mt.addCell(sc);
			}
			elements.add(mt);
		}

		// ── 7. Bottom border spacer ───────────────────────────────────────────────
		PdfPTable bottomBorder = new PdfPTable(1);
		bottomBorder.setWidthPercentage(100);
		PdfPCell spacer = new PdfPCell(new Phrase(""));
		spacer.setFixedHeight(4);
		spacer.setBorder(Rectangle.BOTTOM | Rectangle.LEFT | Rectangle.RIGHT);
		spacer.setBorderColor(C_MGRAY);
		spacer.setBorderWidth(0.5f);
		bottomBorder.addCell(spacer);
		elements.add(bottomBorder);

		return elements;
	}

	private void writeErrorPdf(String message, OutputStream out) throws Exception {
		Document doc = new Document(PageSize.A4, 60, 60, 80, 60);
		PdfWriter.getInstance(doc, out);
		doc.open();
		doc.add(new Paragraph("Report Generation Error", new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, C_HIGH)));
		doc.add(new Paragraph("\n"));
		doc.add(new Paragraph(message, new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, C_TEXT)));
		doc.add(new Paragraph("\nPlease check the server logs and try again.",
				new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC, C_SUBTEXT)));
		doc.close();
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// SECTION HEADING
	// ─────────────────────────────────────────────────────────────────────────────
	private Paragraph sectionHeading(String text) {
		Paragraph p = new Paragraph(text, new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, C_NAVY));
		p.setSpacingBefore(10);
		p.setSpacingAfter(6);
		p.setIndentationLeft(8);
		return p;
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// DIVIDER
	// ─────────────────────────────────────────────────────────────────────────────
	private Paragraph divider() {
		Paragraph p = new Paragraph();
		p.add(new Chunk(new LineSeparator(0.5f, 100, C_MGRAY, Element.ALIGN_CENTER, -2)));
		p.setSpacingBefore(6);
		p.setSpacingAfter(6);
		return p;
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// STAT CELL
	// ─────────────────────────────────────────────────────────────────────────────
	private void addStatCell(PdfPTable t, String val, String label, BaseColor color) {
		PdfPCell c = new PdfPCell();
		c.setBorder(Rectangle.NO_BORDER);
		c.setBackgroundColor(color);
		c.setPadding(10);
		Paragraph v = new Paragraph(val, new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, C_WHITE));
		v.setAlignment(Element.ALIGN_CENTER);
		c.addElement(v);
		Paragraph l = new Paragraph(label,
				new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, new BaseColor(220, 225, 255)));
		l.setAlignment(Element.ALIGN_CENTER);
		c.addElement(l);
		t.addCell(c);
	}

	// ─────────────────────────────────────────────────────────────────────────────
	// UPDATED algoCell — added isLeft param to control which border sides to draw
	// ─────────────────────────────────────────────────────────────────────────────
	private PdfPCell algoCell(String label, String value, BaseColor accent, boolean isLeft) {
		PdfPCell c = new PdfPCell();
		c.setBorder(isLeft ? Rectangle.LEFT | Rectangle.BOTTOM : Rectangle.LEFT | Rectangle.RIGHT | Rectangle.BOTTOM);
		c.setBorderColor(C_MGRAY);
		c.setBorderColorLeft(accent);
		c.setBorderWidthLeft(3f);
		c.setBorderWidth(0.5f);
		c.setBackgroundColor(C_LGRAY);
		c.setPadding(10);
		c.setPaddingLeft(14);

		Paragraph l = new Paragraph(label, new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, C_SUBTEXT));
		l.setSpacingAfter(4);
		c.addElement(l);
		c.addElement(new Paragraph(value, new Font(Font.FontFamily.HELVETICA, 15, Font.BOLD, accent)));
		return c;
	}

	/**
	 * Health check endpoint
	 */
	@GetMapping("/health")
	public ResponseEntity<String> healthCheck() {
		return ResponseEntity.ok("Server is running");
	}
}