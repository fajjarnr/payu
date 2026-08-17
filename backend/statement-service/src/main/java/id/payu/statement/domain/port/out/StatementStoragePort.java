package id.payu.statement.domain.port.out;

import java.io.IOException;
import java.util.UUID;

/**
 * Output port for storing and retrieving statement PDFs.
 */
public interface StatementStoragePort {

    String uploadPdf(UUID statementId, byte[] pdfBytes);

    byte[] downloadPdf(String s3Path) throws IOException;

    boolean isEnabled();
}
