package id.payu.transaction.domain.model;

/**
 * Enumeration of available transfer methods for smart routing.
 *
 * <p>Each transfer method has different characteristics in terms of:
 * <ul>
 *   <li>Speed - how quickly the transfer completes</li>
 *   <li>Cost - transaction fees</li>
 *   <li>Limits - minimum and maximum amounts</li>
 *   <li>Availability - operating hours and network coverage</li>
 * </ul>
 *
 * <p>Method selection criteria:</p>
 * <ul>
 *   <li><b>BI-FAST</b> - Real-time, low cost, best for small-to-medium amounts (up to 50M IDR)</li>
 *   <li><b>RTGS</b> - Real-time, high value, for large amounts (100M+ IDR)</li>
 *   <li><b>SKN</b> - Batch processing, lower cost, for non-urgent transfers</li>
 * </ul>
 *
 * @see TransferRoute
 */
public enum TransferMethod {
    /**
     * BI-FAST (Bank Indonesia Fast Payment).
     * Real-time gross settlement system for retail payments.
     *
     * <p>Characteristics:</p>
     * <ul>
     *   <li>Speed: ~30 seconds</li>
     *   <li>Fee: IDR 2,500</li>
     *   <li>Limits: IDR 1 - 50,000,000</li>
     *   <li>Availability: 24/7</li>
     * </ul>
     */
    BI_FAST,

    /**
     * RTGS (Real-Time Gross Settlement).
     * High-value payment system for large transfers.
     *
     * <p>Characteristics:</p>
     * <ul>
     *   <li>Speed: ~5 minutes</li>
     *   <li>Fee: IDR 25,000</li>
     *   <li>Limits: IDR 100,000,000 - 10,000,000,000</li>
     *   <li>Availability: Business hours</li>
     * </ul>
     */
    RTGS,

    /**
     * SKN (Sistem Kliring Nasional).
     * National clearing system for batch processing.
     *
     * <p>Characteristics:</p>
     * <ul>
     *   <li>Speed: ~4 hours</li>
     *   <li>Fee: IDR 5,000</li>
     *   <li>Limits: IDR 1 - 1,000,000,000</li>
     *   <li>Availability: Business hours, batch processing</li>
     * </ul>
     */
    SKN
}
