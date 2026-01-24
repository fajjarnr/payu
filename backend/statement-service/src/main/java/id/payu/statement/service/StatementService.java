package id.payu.statement.service;

import id.payu.statement.domain.entity.Statement;
import id.payu.statement.domain.repository.StatementRepository;
import id.payu.statement.service.dto.StatementGenerationRequest;
import id.payu.statement.service.dto.StatementResponse;
import id.payu.statement.service.exception.StatementException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for generating and managing e-statements
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatementService {

    private final StatementRepository statementRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final WalletServiceClient walletServiceClient;
    private final TransactionServiceClient transactionServiceClient;

    @Value("${statement.storage.path:/tmp/statements}")
    private String storagePath;

    @Value("${statement.company.name:PayU Digital Banking}")
    private String companyName;

    @Value("${statement.company.address:Jakarta, Indonesia}")
    private String companyAddress;

    private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    /**
     * Generate statement for a user for a specific month
     */
    @Async
    @Transactional
    public void generateStatement(StatementGenerationRequest request) {
        LocalDate statementPeriod = YearMonth.of(request.getYear(), request.getMonth()).atDay(1);

        // Check if statement already exists
        if (statementRepository.existsByUserIdAndStatementPeriod(request.getUserId(), statementPeriod)) {
            log.info("Statement already exists for user {} and period {}", request.getUserId(), statementPeriod);
            return;
        }

        try {
            // Create statement entity
            Statement statement = Statement.builder()
                .id(UUID.randomUUID())
                .userId(request.getUserId())
                .accountNumber(request.getAccountNumber())
                .statementPeriod(statementPeriod)
                .status(Statement.StatementStatus.GENERATING)
                .build();

            statement = statementRepository.save(statement);

            // Fetch data from wallet and transaction services
            StatementData data = fetchStatementData(request.getUserId(), statementPeriod);

            // Generate PDF
            byte[] pdfBytes = generatePdf(statement.getId(), data);

            // Store PDF
            String filePath = storePdf(statement.getId(), pdfBytes);

            // Update statement
            statement.markCompleted(filePath, (long) pdfBytes.length);
            statement.setOpeningBalance(data.getOpeningBalance());
            statement.setClosingBalance(data.getClosingBalance());
            statement.setTotalCredits(data.getTotalCredits());
            statement.setTotalDebits(data.getTotalDebits());
            statement.setTransactionCount(data.getTransactionCount());

            statement = statementRepository.save(statement);

            // Publish event
            publishStatementGeneratedEvent(statement);

            log.info("Successfully generated statement {} for user {}", statement.getId(), request.getUserId());

        } catch (Exception e) {
            log.error("Failed to generate statement for user {} and period {}",
                request.getUserId(), statementPeriod, e);

            // Mark as failed
            Optional<Statement> failedStatement = statementRepository.findByUserIdAndStatementPeriod(
                request.getUserId(), statementPeriod);
            failedStatement.ifPresent(s -> {
                s.markFailed();
                statementRepository.save(s);
            });

            throw new StatementException("STATEMENT_001", "Failed to generate statement: " + e.getMessage());
        }
    }

    /**
     * Get statement by ID (with user validation)
     */
    @Transactional(readOnly = true)
    public StatementResponse getStatement(UUID statementId, UUID userId) {
        Statement statement = statementRepository.findByIdAndUserId(statementId, userId)
            .orElseThrow(() -> new StatementException("STATEMENT_002", "Statement not found"));

        statement.recordAccess();
        statementRepository.save(statement);

        return mapToResponse(statement);
    }

    /**
     * List all statements for a user
     */
    @Transactional(readOnly = true)
    public Page<StatementResponse> listStatements(UUID userId, Pageable pageable) {
        Page<Statement> statements = statementRepository.findAllByUserId(userId, pageable);
        return new PageImpl<>(
            statements.stream().map(this::mapToResponse).toList(),
            statements.getPageable(),
            statements.getTotalElements()
        );
    }

    /**
     * Get latest statement for user
     */
    @Transactional(readOnly = true)
    public Optional<StatementResponse> getLatestStatement(UUID userId) {
        return statementRepository.findLatestCompletedByUserId(userId)
            .map(this::mapToResponse);
    }

    /**
     * Get statement PDF bytes
     */
    public byte[] getStatementPdf(UUID statementId, UUID userId) {
        Statement statement = statementRepository.findByIdAndUserId(statementId, userId)
            .orElseThrow(() -> new StatementException("STATEMENT_002", "Statement not found"));

        if (statement.getStatus() != Statement.StatementStatus.COMPLETED) {
            throw new StatementException("STATEMENT_003", "Statement is not ready for download");
        }

        try {
            Path filePath = Paths.get(statement.getStoragePath());
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("Failed to read PDF file for statement {}", statementId, e);
            throw new StatementException("STATEMENT_004", "Failed to read statement file");
        }
    }

    /**
     * Regenerate statement (admin function)
     */
    @Async
    @Transactional
    public void regenerateStatement(UUID statementId) {
        Statement statement = statementRepository.findById(statementId)
            .orElseThrow(() -> new StatementException("STATEMENT_002", "Statement not found"));

        StatementGenerationRequest request = StatementGenerationRequest.builder()
            .userId(statement.getUserId())
            .accountNumber(statement.getAccountNumber())
            .year(statement.getStatementPeriod().getYear())
            .month(statement.getStatementPeriod().getMonthValue())
            .build();

        // Delete existing file
        if (statement.getStoragePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(statement.getStoragePath()));
            } catch (IOException e) {
                log.warn("Failed to delete old statement file: {}", e.getMessage());
            }
        }

        // Reset and regenerate
        statement.setStatus(Statement.StatementStatus.GENERATING);
        statementRepository.save(statement);

        generateStatement(request);
    }

    /**
     * Fetch statement data from wallet and transaction services
     */
    private StatementData fetchStatementData(UUID userId, LocalDate statementPeriod) {
        LocalDate startDate = statementPeriod;
        LocalDate endDate = statementPeriod.plusMonths(1).minusDays(1);

        // Get balances from wallet service
        BigDecimal openingBalance = walletServiceClient.getBalanceAtDate(userId, startDate.minusDays(1));
        BigDecimal closingBalance = walletServiceClient.getBalanceAtDate(userId, endDate);

        // Get transactions from transaction service
        List<TransactionRecord> transactions = transactionServiceClient.getTransactions(
            userId, startDate, endDate);

        // Calculate totals
        BigDecimal totalCredits = transactions.stream()
            .filter(t -> t.getType() == TransactionType.CREDIT)
            .map(TransactionRecord::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDebits = transactions.stream()
            .filter(t -> t.getType() == TransactionType.DEBIT)
            .map(TransactionRecord::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return StatementData.builder()
            .userId(userId)
            .statementPeriod(statementPeriod)
            .openingBalance(openingBalance)
            .closingBalance(closingBalance)
            .totalCredits(totalCredits)
            .totalDebits(totalDebits)
            .transactionCount(transactions.size())
            .transactions(transactions)
            .build();
    }

    /**
     * Generate PDF document
     */
    private byte[] generatePdf(UUID statementId, StatementData data) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float margin = 50;
                float yPosition = page.getMediaBox().getHeight() - margin;
                float startX = margin;
                float pageWidth = page.getMediaBox().getWidth() - (2 * margin);

                // Font setup
                PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

                // Header - Company Info
                yPosition = drawHeader(contentStream, startX, yPosition, font, fontBold, pageWidth);

                // Statement Title
                yPosition -= 20;
                contentStream.setFont(fontBold, 16);
                contentStream.beginText();
                contentStream.newLineAtOffset(startX, yPosition);
                contentStream.showText("MONTHLY STATEMENT");
                contentStream.endText();

                // Period
                yPosition -= 25;
                contentStream.setFont(font, 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(startX, yPosition);
                String periodText = "Period: " + data.getStatementPeriod().format(MONTH_YEAR_FORMATTER);
                contentStream.showText(periodText);
                contentStream.endText();

                // Account Summary Section
                yPosition -= 40;
                yPosition = drawAccountSummary(contentStream, startX, yPosition, fontBold, font, pageWidth, data);

                // Transaction Summary
                yPosition -= 30;
                yPosition = drawTransactionSummary(contentStream, startX, yPosition, fontBold, font, pageWidth, data);

                // Footer
                yPosition = drawFooter(contentStream, startX, 50, font, pageWidth);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    private float drawHeader(PDPageContentStream contentStream, float x, float y,
                             PDType1Font font, PDType1Font fontBold, float width) throws IOException {
        contentStream.setFont(fontBold, 18);
        contentStream.beginText();
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(companyName);
        contentStream.endText();

        y -= 20;
        contentStream.setFont(font, 10);
        contentStream.beginText();
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(companyAddress);
        contentStream.endText();

        return y - 30;
    }

    private float drawAccountSummary(PDPageContentStream contentStream, float x, float y,
                                      PDType1Font fontBold, PDType1Font font, float width,
                                      StatementData data) throws IOException {
        // Box border
        contentStream.setLineWidth(0.5f);
        contentStream.addRect(x, y - 100, width, 100);
        contentStream.stroke();

        // Title
        y -= 20;
        contentStream.setFont(fontBold, 14);
        contentStream.beginText();
        contentStream.newLineAtOffset(x + 10, y);
        contentStream.showText("ACCOUNT SUMMARY");
        contentStream.endText();

        // Opening Balance
        y -= 25;
        contentStream.setFont(font, 11);
        contentStream.beginText();
        contentStream.newLineAtOffset(x + 10, y);
        contentStream.showText("Opening Balance:");
        contentStream.endText();

        contentStream.beginText();
        contentStream.newLineAtOffset(x + width - 10 - 100, y);
        contentStream.showText(formatCurrency(data.getOpeningBalance()));
        contentStream.endText();

        // Credits
        y -= 20;
        contentStream.beginText();
        contentStream.newLineAtOffset(x + 10, y);
        contentStream.showText("Total Credits:");
        contentStream.endText();

        contentStream.beginText();
        contentStream.newLineAtOffset(x + width - 10 - 100, y);
        String creditText = "+" + formatCurrency(data.getTotalCredits());
        contentStream.showText(creditText);
        contentStream.endText();

        // Debits
        y -= 20;
        contentStream.beginText();
        contentStream.newLineAtOffset(x + 10, y);
        contentStream.showText("Total Debits:");
        contentStream.endText();

        contentStream.beginText();
        contentStream.newLineAtOffset(x + width - 10 - 100, y);
        String debitText = "-" + formatCurrency(data.getTotalDebits());
        contentStream.showText(debitText);
        contentStream.endText();

        // Closing Balance
        y -= 20;
        contentStream.setFont(fontBold, 12);
        contentStream.beginText();
        contentStream.newLineAtOffset(x + 10, y);
        contentStream.showText("Closing Balance:");
        contentStream.endText();

        contentStream.beginText();
        contentStream.newLineAtOffset(x + width - 10 - 100, y);
        contentStream.showText(formatCurrency(data.getClosingBalance()));
        contentStream.endText();

        return y - 110;
    }

    private float drawTransactionSummary(PDPageContentStream contentStream, float x, float y,
                                          PDType1Font fontBold, PDType1Font font, float width,
                                          StatementData data) throws IOException {
        contentStream.setFont(fontBold, 14);
        contentStream.beginText();
        contentStream.newLineAtOffset(x, y);
        contentStream.showText("TRANSACTION SUMMARY (" + data.getTransactionCount() + " transactions)");
        contentStream.endText();

        y -= 25;

        // Draw header row
        float col1 = x + 10;
        float col2 = x + 80;
        float col3 = x + 250;
        float col4 = x + width - 10 - 80;

        contentStream.setFont(fontBold, 10);
        contentStream.beginText();
        contentStream.newLineAtOffset(col1, y);
        contentStream.showText("Date");
        contentStream.endText();

        contentStream.beginText();
        contentStream.newLineAtOffset(col2, y);
        contentStream.showText("Description");
        contentStream.endText();

        contentStream.beginText();
        contentStream.newLineAtOffset(col4, y);
        contentStream.showText("Amount");
        contentStream.endText();

        // Draw line
        y -= 5;
        contentStream.moveTo(x, y);
        contentStream.lineTo(x + width, y);
        contentStream.stroke();

        // Draw up to 20 transactions (first page summary)
        y -= 15;
        contentStream.setFont(font, 9);

        int maxTransactions = Math.min(20, data.getTransactions().size());
        for (int i = 0; i < maxTransactions; i++) {
            TransactionRecord txn = data.getTransactions().get(i);

            // Date
            contentStream.beginText();
            contentStream.newLineAtOffset(col1, y);
            contentStream.showText(txn.getDate().format(DATE_FORMATTER));
            contentStream.endText();

            // Description (truncated)
            String desc = txn.getDescription();
            if (desc.length() > 30) {
                desc = desc.substring(0, 27) + "...";
            }
            contentStream.beginText();
            contentStream.newLineAtOffset(col2, y);
            contentStream.showText(desc);
            contentStream.endText();

            // Amount
            String amount = (txn.getType() == TransactionType.CREDIT ? "+" : "-")
                + formatCurrency(txn.getAmount());
            contentStream.beginText();
            contentStream.newLineAtOffset(col4, y);
            contentStream.showText(amount);
            contentStream.endText();

            y -= 15;

            // New page if needed
            if (y < 100) {
                break;
            }
        }

        if (data.getTransactions().size() > 20) {
            contentStream.setFont(font, 10);
            contentStream.beginText();
            contentStream.newLineAtOffset(x, y - 10);
            contentStream.showText("... and " + (data.getTransactions().size() - 20) + " more transactions");
            contentStream.endText();
        }

        return y;
    }

    private float drawFooter(PDPageContentStream contentStream, float x, float y,
                              PDType1Font font, float width) throws IOException {
        contentStream.setFont(font, 8);
        contentStream.beginText();
        contentStream.newLineAtOffset(x, y);
        contentStream.showText("This is a computer-generated statement. No signature required.");
        contentStream.endText();

        y -= 15;
        contentStream.beginText();
        contentStream.newLineAtOffset(x, y);
        contentStream.showText("For inquiries, please contact PayU Customer Service at +62 21 555-1234");
        contentStream.endText();

        y -= 15;
        contentStream.beginText();
        contentStream.newLineAtOffset(x, y);
        contentStream.showText("Generated on: " + LocalDate.now().format(DATE_FORMATTER));
        contentStream.endText();

        return y;
    }

    private String formatCurrency(BigDecimal amount) {
        return "Rp " + amount.setScale(0, RoundingMode.HALF_UP)
            .toBigInteger()
            .toString()
            .replaceAll("\\B(?=(\\d{3})+(?!\\d))", ".");
    }

    private String storePdf(UUID statementId, byte[] pdfBytes) throws IOException {
        Path storageDir = Paths.get(storagePath);
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }

        String fileName = "statement_" + statementId + ".pdf";
        Path filePath = storageDir.resolve(fileName);
        Files.write(filePath, pdfBytes, StandardOpenOption.CREATE_NEW);

        return filePath.toString();
    }

    private void publishStatementGeneratedEvent(Statement statement) {
        StatementGeneratedEvent event = StatementGeneratedEvent.builder()
            .statementId(statement.getId())
            .userId(statement.getUserId())
            .accountNumber(statement.getAccountNumber())
            .statementPeriod(statement.getStatementPeriod())
            .storagePath(statement.getStoragePath())
            .createdAt(LocalDateTime.now())
            .build();

        kafkaTemplate.send("payu.statements.generated", event);
    }

    private StatementResponse mapToResponse(Statement statement) {
        return StatementResponse.builder()
            .id(statement.getId())
            .userId(statement.getUserId())
            .accountNumber(statement.getAccountNumber())
            .statementPeriod(statement.getStatementPeriod())
            .openingBalance(statement.getOpeningBalance())
            .closingBalance(statement.getClosingBalance())
            .totalCredits(statement.getTotalCredits())
            .totalDebits(statement.getTotalDebits())
            .transactionCount(statement.getTransactionCount())
            .status(statement.getStatus())
            .generatedAt(statement.getGeneratedAt())
            .createdAt(statement.getCreatedAt())
            .build();
    }

    // Inner classes for data transfer
    @lombok.Data
    @lombok.Builder
    private static class StatementData {
        private UUID userId;
        private LocalDate statementPeriod;
        private BigDecimal openingBalance;
        private BigDecimal closingBalance;
        private BigDecimal totalCredits;
        private BigDecimal totalDebits;
        private Integer transactionCount;
        private List<TransactionRecord> transactions;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class TransactionRecord {
        private LocalDate date;
        private String description;
        private BigDecimal amount;
        private TransactionType type;
    }

    private enum TransactionType {
        CREDIT, DEBIT
    }

    @lombok.Data
    @lombok.Builder
    private static class StatementGeneratedEvent {
        private UUID statementId;
        private UUID userId;
        private String accountNumber;
        private LocalDate statementPeriod;
        private String storagePath;
        private LocalDateTime createdAt;
    }
}
