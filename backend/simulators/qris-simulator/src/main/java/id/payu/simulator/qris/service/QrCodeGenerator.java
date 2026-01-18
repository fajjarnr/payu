package id.payu.simulator.qris.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import id.payu.simulator.qris.config.SimulatorConfig;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;

/**
 * Service for generating QRIS QR codes.
 * Uses ZXing library for QR code generation.
 */
@ApplicationScoped
public class QrCodeGenerator {

    @Inject
    SimulatorConfig config;

    /**
     * Generate QRIS-like QR content string.
     * This simulates the actual QRIS format (EMVCo QR Code Specification).
     */
    public String generateQrisContent(String merchantId, String merchantName, 
                                       BigDecimal amount, String referenceNumber) {
        // Simplified QRIS format for simulation
        // Real QRIS uses EMVCo format with TLV encoding
        StringBuilder content = new StringBuilder();
        content.append("00020101");  // Payload Format Indicator
        content.append("010212");    // Point of Initiation (Dynamic)
        content.append("5204"); // Merchant Category Code
        content.append("5802ID"); // Country Code
        content.append("5303360"); // Currency (IDR)
        
        // Amount (if provided)
        if (amount != null) {
            String amountStr = amount.setScale(0).toString();
            content.append("54").append(String.format("%02d", amountStr.length())).append(amountStr);
        }
        
        // Merchant Name
        content.append("59").append(String.format("%02d", merchantName.length())).append(merchantName);
        
        // Reference Number
        content.append("62").append(String.format("%02d", referenceNumber.length())).append(referenceNumber);
        
        // Merchant ID
        content.append("26").append(String.format("%02d", merchantId.length())).append(merchantId);
        
        // Checksum (simplified)
        String checksum = String.format("%04X", content.toString().hashCode() & 0xFFFF);
        content.append("6304").append(checksum);
        
        return content.toString();
    }

    /**
     * Generate QR code image as Base64 string.
     */
    public String generateQrImage(String content) {
        int size = config.qr().imageSize();
        String format = config.qr().format();

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 2);
            
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, size, size, hints);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, format, outputStream);
            
            byte[] imageBytes = outputStream.toByteArray();
            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            
            Log.debugf("Generated QR code image: %d bytes, format=%s", imageBytes.length, format);
            
            return "data:image/" + format.toLowerCase() + ";base64," + base64;
            
        } catch (WriterException | IOException e) {
            Log.errorf(e, "Failed to generate QR code image");
            return null;
        }
    }
}
