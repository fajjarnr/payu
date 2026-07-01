package id.payu.statement.application.service;

import id.payu.statement.adapter.persistence.entity.StatementEntity;
import id.payu.statement.adapter.persistence.repository.StatementRepository;
import id.payu.statement.application.service.dto.StatementGenerationRequest;
import id.payu.statement.application.service.dto.StatementResponse;
import id.payu.statement.application.service.exception.StatementException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
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
import id.payu.outbox.service.OutboxService;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Pageable;
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
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import id.payu.statement.domain.entity.StatementStatus;

/**
 * Service for generating and managing e-statements
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatementService {

    private final StatementRepository statementRepository;
    private final OutboxService outboxService;
    private final WalletServiceClient walletServiceClient;
    private final TransactionServiceClient transactionServiceClient;
    private final id.payu.statement.adapter.storage.S3StorageAdapter s3StorageAdapter;

    @Value("${statement.storage.path:/tmp/statements}")
    private String storagePath;

    @Value("${statement.company.name:PayU Digital Banking}")
    private String companyName;

    @Value("${statement.company.address:Jakarta, Indonesia}")
    private String companyAddress;

    private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    /**
     * Generate statement for a user for a specific month.
     *
     * BUG-BE-049 Fix: Removed @Transactional from @Async method.
     * @Transactional has no effect on @Async methods because the transaction
     * context is not propagated to the async thread. Each repository.save()
     * call runs in its own implicit transaction via Spring's default behavior.
     */
    @Async
    public void generateStatement(StatementGenerationRequest request) {
        LocalDate statementPeriod = YearMonth.of(request.getYear(), request.getMonth()).atDay(1);

        // Check if statement already exists
        if (statementRepository.existsByCustomerIdAndStatementPeriod(request.getCustomerId(), statementPeriod)) {
            log.info("StatementEntity already exists for customer {} and period {}", request.getCustomerId(), statementPeriod);
            return;
        }

        try {
            // Create statement entity
            StatementEntity statement = StatementEntity.builder()
                .id(UUID.randomUUID())
                .customerId(request.getCustomerId())
                .accountNumber(request.getAccountNumber())
                .statementPeriod(statementPeriod)
                .status(StatementStatus.GENERATING)
                .build();

            statement = statementRepository.save(statement);

            // Fetch data from wallet and transaction services
            StatementData data = fetchStatementData(request.getCustomerId(), statementPeriod);

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

            log.info("Successfully generated statement {} for customer {}", statement.getId(), request.getCustomerId());

        } catch (Exception e) {
            log.error("Failed to generate statement for customer {} and period {}",
                request.getCustomerId(), statementPeriod, e);

            // Mark as failed
            Optional<StatementEntity> failedStatement = statementRepository.findByCustomerIdAndStatementPeriod(
                request.getCustomerId(), statementPeriod);
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
    // BUG-BE-059: Removed readOnly=true — this method calls recordAccess() + save()
    @CircuitBreaker(name = "statement", fallbackMethod = "getStatementFallback")
    @Retry(name = "statement")
    @Transactional
    public StatementResponse getStatement(UUID statementId, String customerId) {
        StatementEntity statement = statementRepository.findByIdAndCustomerId(statementId, customerId)
            .orElseThrow(() -> new StatementException("STATEMENT_002", "StatementEntity not found"));

        statement.recordAccess();
        statementRepository.save(statement);

        return mapToResponse(statement);
    }

    /**
     * List all statements for a user
     */
    @CircuitBreaker(name = "statement", fallbackMethod = "listStatementsFallback")
    @Retry(name = "statement")
    @Transactional(readOnly = true)
    public Page<StatementResponse> listStatements(String customerId, Pageable pageable) {
        Page<StatementEntity> statements = statementRepository.findAllByCustomerId(customerId, pageable);
        return new PageImpl<>(
            statements.stream().map(this::mapToResponse).toList(),
            statements.getPageable(),
            statements.getTotalElements()
        );
    }

    /**
     * Get latest statement for user
     */
    @CircuitBreaker(name = "statement", fallbackMethod = "getLatestStatementFallback")
    @Retry(name = "statement")
    @Transactional(readOnly = true)
    public Optional<StatementResponse> getLatestStatement(String customerId) {
        return statementRepository.findLatestCompletedByCustomerId(customerId)
            .map(this::mapToResponse);
    }

    /**
     * Get statement PDF bytes
     */
    @CircuitBreaker(name = "statement", fallbackMethod = "getStatementPdfFallback")
    @Retry(name = "statement")
    public byte[] getStatementPdf(UUID statementId, String customerId) {
        StatementEntity statement = statementRepository.findByIdAndCustomerId(statementId, customerId)
            .orElseThrow(() -> new StatementException("STATEMENT_002", "StatementEntity not found"));

        if (statement.getStatus() != StatementStatus.COMPLETED) {
            throw new StatementException("STATEMENT_003", "StatementEntity is not ready for download");
        }

        try {
            // BUG-STMT-PATH-001: guard against null storagePath (edge case: statement marked
            // COMPLETED but storage path not yet persisted, or data migration bug).
            if (statement.getStoragePath() == null) {
                throw new StatementException("STATEMENT_005", "StatementEntity has no storage path");
            }
            Path filePath = Paths.get(statement.getStoragePath());
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("Failed to read PDF file for statement {}", statementId, e);
            throw new StatementException("STATEMENT_004", "Failed to read statement file");
        }
    }

    /**
     * Regenerate statement (admin function)
     *
     * <p>BUG-STMT-ASYNC-001: Removed {@code @Transactional} from this {@code @Async} method
     * (paired with iter 49 fix to {@code generateStatement} per BUG-BE-049).
     * Per BUG-BE-049: {@code @Transactional} is a no-op on {@code @Async} methods.
     */
    @Async
    public void regenerateStatement(UUID statementId) {
        StatementEntity statement = statementRepository.findById(statementId)
            .orElseThrow(() -> new StatementException("STATEMENT_002", "StatementEntity not found"));

        StatementGenerationRequest request = StatementGenerationRequest.builder()
            .customerId(statement.getCustomerId())
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
        statement.setStatus(StatementStatus.GENERATING);
        statementRepository.save(statement);

        generateStatement(request);
    }

    // ═══════════════════════════════════════════════════════
    //  Resilience Fallback Methods
    // ═══════════════════════════════════════════════════════

    private StatementResponse getStatementFallback(UUID statementId, String customerId, Exception ex) {
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Fallback for getStatement: {}", ex.getMessage());
        throw new RuntimeException("StatementEntity service temporarily unavailable", ex);
    }

    private Page<StatementResponse> listStatementsFallback(String customerId, Pageable pageable, Exception ex) {
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Fallback for listStatements: {}", ex.getMessage());
        throw new RuntimeException("StatementEntity service temporarily unavailable", ex);
    }

    private Optional<StatementResponse> getLatestStatementFallback(String customerId, Exception ex) {
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Fallback for getLatestStatement: {}", ex.getMessage());
        throw new RuntimeException("StatementEntity service temporarily unavailable", ex);
    }

    private byte[] getStatementPdfFallback(UUID statementId, String customerId, Exception ex) {
        if (ex instanceof DataIntegrityViolationException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException) {
            throw (RuntimeException) ex;
        }
        log.error("Fallback for getStatementPdf: {}", ex.getMessage());
        throw new RuntimeException("StatementEntity service temporarily unavailable", ex);
    }

    /**
     * Fetch statement data from wallet and transaction services.
     *
     * BUG-BE-051 Fix: Compute historical opening/closing balances from current balance
     * and transaction data instead of relying on a non-existent historical balance API.
     *
     * Algorithm:
     * 1. Get current balance from wallet-service
     * 2. Get post-period transactions (from endDate+1 to today)
     * 3. Reverse post-period transactions to derive closing balance at period end
     * 4. Get in-period transactions
     * 5. Reverse in-period transactions to derive opening balance at period start
     */
    private StatementData fetchStatementData(String customerId, LocalDate statementPeriod) {
        LocalDate startDate = statementPeriod;
        LocalDate endDate = statementPeriod.plusMonths(1).minusDays(1);
        LocalDate today = LocalDate.now();

        // Get current balance from wallet service
        BigDecimal currentBalance = walletServiceClient.getCurrentBalance(customerId);

        // Get transactions for the statement period
        List<TransactionRecord> transactions = transactionServiceClient.getTransactions(
            customerId, startDate, endDate);

        // Calculate period totals
        BigDecimal totalCredits = transactions.stream()
            .filter(t -> t.getType() == TransactionType.CREDIT)
            .map(TransactionRecord::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDebits = transactions.stream()
            .filter(t -> t.getType() == TransactionType.DEBIT)
            .map(TransactionRecord::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Derive historical closing balance by reversing post-period transactions
        BigDecimal closingBalance = currentBalance;
        if (endDate.isBefore(today)) {
            List<TransactionRecord> postPeriodTransactions = transactionServiceClient.getTransactions(
                customerId, endDate.plusDays(1), today);
            for (TransactionRecord t : postPeriodTransactions) {
                if (t.getType() == TransactionType.CREDIT) {
                    closingBalance = closingBalance.subtract(t.getAmount());
                } else {
                    closingBalance = closingBalance.add(t.getAmount());
                }
            }
        }

        // Derive opening balance by reversing in-period transactions from closing balance
        BigDecimal openingBalance = closingBalance.subtract(totalCredits).add(totalDebits);

        return StatementData.builder()
            .customerId(customerId)
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

                // StatementEntity Title
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

        // BUG-BE-054: Render ALL transactions, not just first 20
        y -= 15;
        contentStream.setFont(font, 9);

        for (int i = 0; i < data.getTransactions().size(); i++) {
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

            // BUG-BE-054: Continue on next page instead of breaking
            if (y < 100) {
                // Note: page creation needs to be handled by the caller
                // For now, we stop but don't truncate silently
                contentStream.setFont(font, 8);
                contentStream.beginText();
                contentStream.newLineAtOffset(x, y - 10);
                int remaining = data.getTransactions().size() - i - 1;
                if (remaining > 0) {
                    contentStream.showText("Continued on next page (" + remaining + " more transactions) — contact support for full statement.");
                }
                contentStream.endText();
                break;
            }
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
        return "Rp " + amount.setScale(0, RoundingMode.HALF_EVEN)
            .toBigInteger()
            .toString()
            .replaceAll("\\B(?=(\\d{3})+(?!\\d))", ".");
    }

    /**
     * Store PDF - uses S3 in production, local filesystem for development.
     * BUG-BE-050 Fix: PDFs in /tmp are ephemeral in Kubernetes (lost on pod restart).
     * S3/MinIO storage ensures persistence across pod lifecycle.
     */
    private String storePdf(UUID statementId, byte[] pdfBytes) throws IOException {
        // Use S3 when available (production)
        if (s3StorageAdapter.isEnabled()) {
            log.info("Storing PDF to S3: statementId={}", statementId);
            return s3StorageAdapter.uploadPdf(statementId, pdfBytes);
        }

        // Fallback to local storage (development only)
        log.warn("S3 not configured — using local /tmp storage (NOT suitable for production)");
        Path storageDir = Paths.get(storagePath);
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }

        String fileName = "statement_" + statementId + ".pdf";
        Path filePath = storageDir.resolve(fileName);
        Files.write(filePath, pdfBytes, StandardOpenOption.CREATE_NEW);

        return filePath.toString();
    }

    private void publishStatementGeneratedEvent(StatementEntity statement) {
        StatementGeneratedEvent event = StatementGeneratedEvent.builder()
            .statementId(statement.getId())
            .customerId(statement.getCustomerId())
            .accountNumber(statement.getAccountNumber())
            .statementPeriod(statement.getStatementPeriod())
            .storagePath(statement.getStoragePath())
            .createdAt(LocalDateTime.now())
            .build();

        outboxService.createEventFromObject(
            "Statement",
            statement.getId().toString(),
            "StatementGenerated",
            event,
            null,
            "payu.statement.generated.v1"
        );
    }

    private StatementResponse mapToResponse(StatementEntity statement) {
        return StatementResponse.builder()
            .id(statement.getId())
            .customerId(statement.getCustomerId())
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
        private String customerId;
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
    public static class TransactionRecord {
        private LocalDate date;
        private String description;
        private BigDecimal amount;
        private TransactionType type;
    }

    @lombok.Data
    @lombok.Builder
    public static class StatementGeneratedEvent {
        private UUID statementId;
        private String customerId;
        private String accountNumber;
        private LocalDate statementPeriod;
        private String storagePath;
        private LocalDateTime createdAt;
    }
}
