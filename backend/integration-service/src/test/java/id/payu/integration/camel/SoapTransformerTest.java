package id.payu.integration.camel;

import id.payu.integration.adapter.camel.transformer.SoapTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SoapTransformer.
 */
public class SoapTransformerTest {

    private SoapTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new SoapTransformer();
    }

    @Test
    void testWrapSoap11() {
        String payload = "<accountNumber>1234567890</accountNumber>";
        String operation = "GetAccountBalance";

        String result = transformer.wrapSoap11(payload, operation);

        assertNotNull(result);
        assertTrue(result.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(result.contains("<soap:Envelope"));
        assertTrue(result.contains("xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\""));
        assertTrue(result.contains("<soap:Body>"));
        assertTrue(result.contains("<payu:GetAccountBalance>"));
        assertTrue(result.contains("<accountNumber>1234567890</accountNumber>"));
        assertTrue(result.contains("</soap:Envelope>"));
    }

    @Test
    void testWrapSoap12() {
        String payload = "<accountNumber>1234567890</accountNumber>";
        String operation = "GetAccountBalance";

        String result = transformer.wrapSoap12(payload, operation);

        assertNotNull(result);
        assertTrue(result.contains("<soap12:Envelope"));
        assertTrue(result.contains("xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\""));
    }

    @Test
    void testUnwrapSoap() {
        String soapResponse = "<?xml version=\"1.0\"?>" +
                "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                "<soap:Header/>" +
                "<soap:Body>" +
                "<GetAccountBalanceResponse>" +
                "<balance>1000000.00</balance>" +
                "</GetAccountBalanceResponse>" +
                "</soap:Body>" +
                "</soap:Envelope>";

        String result = transformer.unwrapSoap(soapResponse);

        assertNotNull(result);
        assertTrue(result.contains("<GetAccountBalanceResponse>"));
        assertTrue(result.contains("<balance>1000000.00</balance>"));
    }

    @Test
    void testHasFault() {
        String faultResponse = "<?xml version=\"1.0\"?>" +
                "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                "<soap:Body>" +
                "<soap:Fault>" +
                "<faultcode>soap:Server</faultcode>" +
                "<faultstring>Internal Server Error</faultstring>" +
                "</soap:Fault>" +
                "</soap:Body>" +
                "</soap:Envelope>";

        String normalResponse = "<?xml version=\"1.0\"?>" +
                "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                "<soap:Body>" +
                "<Response>Success</Response>" +
                "</soap:Body>" +
                "</soap:Envelope>";

        assertTrue(transformer.hasFault(faultResponse));
        assertFalse(transformer.hasFault(normalResponse));
        assertFalse(transformer.hasFault(null));
    }

    @Test
    void testExtractFault() {
        String faultResponse = "<?xml version=\"1.0\"?>" +
                "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                "<soap:Body>" +
                "<soap:Fault>" +
                "<faultcode>soap:Server</faultcode>" +
                "<faultstring>Internal Server Error</faultstring>" +
                "<detail>Error details here</detail>" +
                "</soap:Fault>" +
                "</soap:Body>" +
                "</soap:Envelope>";

        Map<String, String> fault = transformer.extractFault(faultResponse);

        assertNotNull(fault);
        assertEquals("soap:Server", fault.get("code"));
        assertEquals("Internal Server Error", fault.get("message"));
        assertEquals("Error details here", fault.get("detail"));
    }

    @Test
    void testCreateRequest() {
        Map<String, String> params = Map.of(
                "accountNumber", "1234567890",
                "currency", "IDR"
        );

        String result = transformer.createRequest("GetBalance", params);

        assertNotNull(result);
        assertTrue(result.contains("<GetBalance>"));
        assertTrue(result.contains("<accountNumber>1234567890</accountNumber>"));
        assertTrue(result.contains("<currency>IDR</currency>"));
    }
}
