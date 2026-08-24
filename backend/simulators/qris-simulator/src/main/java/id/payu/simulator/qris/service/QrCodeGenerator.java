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
import java.nio.charset.StandardCharsets;
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
     * EMVCo TLV with CRC16-CCITT X25 (poly 0x1021, init 0xFFFF) per ADR-0052/0056.
     */
    public String generateQrisContent(String merchantId, String merchantName,
                                       BigDecimal amount, String referenceNumber) {
        StringBuilder content = new StringBuilder();
        content.append("00020101");  // Payload Format Indicator
        content.append("010212");    // Point of Initiation (Dynamic)
        content.append("5204"); // Merchant Category Code placeholder
        content.append("5802ID"); // Country Code
        content.append("5303360"); // Currency (IDR)

        if (amount != null) {
            String amountStr = amount.setScale(0).toString();
            content.append("54").append(String.format("%02d", amountStr.length())).append(amountStr);
        }

        content.append("59").append(String.format("%02d", merchantName.length())).append(merchantName);
        content.append("62").append(String.format("%02d", referenceNumber.length())).append(referenceNumber);
        content.append("26").append(String.format("%02d", merchantId.length())).append(merchantId);

        // EMVCo CRC16: append 6304 + CRC of content + 63040000
        String preCrc = content.toString() + "6304";
        String crc = crc16(preCrc);
        content.append("6304").append(crc);
        return content.toString();
    }

    static String crc16(String data) {
        int crc = 0xFFFF;
        int poly = 0x1021;
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) crc = (crc << 1) ^ poly;
                else crc <<= 1;
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc);
    }

    /** Validate EMVCo TLV CRC16 — true if trailing 6304+CRC matches computed value. */
    public static boolean isValidCrc(String qrContent) {
        if (qrContent == null || qrContent.length() < 8 || !qrContent.contains("6304")) return false;
        int idx = qrContent.lastIndexOf("6304");
        if (idx + 8 != qrContent.length()) return false;
        String data = qrContent.substring(0, idx + 4);
        String expected = crc16(data);
        String actual = qrContent.substring(idx + 4);
        return expected.equalsIgnoreCase(actual);
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
