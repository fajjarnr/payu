package id.payu.integration.adapter.camel.transformer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Transformer for SOAP envelope handling.
 * Wraps/unwraps SOAP envelopes and extracts body content.
 */
@Component
@Slf4j
public class SoapTransformer {

    private static final String SOAP_11_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String SOAP_12_NS = "http://www.w3.org/2003/05/soap-envelope";

    /**
     * Wrap payload in SOAP 1.1 envelope.
     */
    public String wrapSoap11(String payload, String operation) {
        log.debug("Wrapping payload in SOAP 1.1 envelope for operation: {}", operation);

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soap:Envelope xmlns:soap=\"" + SOAP_11_NS + "\" xmlns:payu=\"http://payu.fajjjar.my.id/integration/\">\n" +
                "  <soap:Header/>\n" +
                "  <soap:Body>\n" +
                "    <payu:" + operation + ">\n" +
                "      " + payload + "\n" +
                "    </payu:" + operation + ">\n" +
                "  </soap:Body>\n" +
                "</soap:Envelope>";
    }

    /**
     * Wrap payload in SOAP 1.2 envelope.
     */
    public String wrapSoap12(String payload, String operation) {
        log.debug("Wrapping payload in SOAP 1.2 envelope for operation: {}", operation);

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soap12:Envelope xmlns:soap12=\"" + SOAP_12_NS + "\" xmlns:payu=\"http://payu.fajjjar.my.id/integration/\">\n" +
                "  <soap12:Header/>\n" +
                "  <soap12:Body>\n" +
                "    <payu:" + operation + ">\n" +
                "      " + payload + "\n" +
                "    </payu:" + operation + ">\n" +
                "  </soap12:Body>\n" +
                "</soap12:Envelope>";
    }

    /**
     * Unwrap SOAP envelope and extract body content.
     */
    public String unwrapSoap(String soapResponse) {
        log.debug("Unwrapping SOAP response");

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(soapResponse)));

            // Find Body element (works for both SOAP 1.1 and 1.2)
            Element body = findElementByLocalName(doc.getDocumentElement(), "Body");
            if (body == null) {
                throw new IllegalArgumentException("SOAP Body not found");
            }

            // Extract first child of Body as response
            NodeList children = body.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    return nodeToString(child);
                }
            }

            return "";
        } catch (Exception e) {
            log.error("Failed to unwrap SOAP response", e);
            throw new SoapTransformationException("Failed to unwrap SOAP response", e);
        }
    }

    /**
     * Extract SOAP Fault details if present.
     */
    public Map<String, String> extractFault(String soapResponse) {
        Map<String, String> fault = new HashMap<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(soapResponse)));

            Element faultElement = findElementByLocalName(doc.getDocumentElement(), "Fault");
            if (faultElement != null) {
                Element faultCode = findElementByLocalName(faultElement, "faultcode");
                Element faultString = findElementByLocalName(faultElement, "faultstring");
                Element detail = findElementByLocalName(faultElement, "detail");

                if (faultCode != null) fault.put("code", getTextContent(faultCode));
                if (faultString != null) fault.put("message", getTextContent(faultString));
                if (detail != null) fault.put("detail", getTextContent(detail));
            }
        } catch (Exception e) {
            log.warn("Failed to extract SOAP fault", e);
        }

        return fault;
    }

    /**
     * Check if response contains a SOAP Fault.
     */
    public boolean hasFault(String soapResponse) {
        return soapResponse != null && (
                soapResponse.contains(":Fault>") ||
                        soapResponse.contains("Fault>")
        );
    }

    /**
     * Create a generic SOAP request for common operations.
     */
    public String createRequest(String operation, Map<String, String> parameters) {
        StringBuilder payload = new StringBuilder();

        for (Map.Entry<String, String> param : parameters.entrySet()) {
            payload.append("    <").append(param.getKey()).append(">");
            payload.append(escapeXml(param.getValue()));
            payload.append("</").append(param.getKey()).append(">\n");
        }

        return wrapSoap11(payload.toString(), operation);
    }

    private Element findElementByLocalName(Element parent, String localName) {
        NodeList children = parent.getElementsByTagNameNS("*", localName);
        if (children.getLength() > 0) {
            return (Element) children.item(0);
        }
        // Try without namespace
        children = parent.getElementsByTagName(localName);
        if (children.getLength() > 0) {
            return (Element) children.item(0);
        }
        return null;
    }

    private String nodeToString(Node node) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        Transformer transformer = factory.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(node), new StreamResult(writer));
        return writer.toString();
    }

    private String getTextContent(Element element) {
        StringBuilder text = new StringBuilder();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                text.append(child.getTextContent());
            }
        }
        return text.toString().trim();
    }

    private String escapeXml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * Exception for SOAP transformation errors.
     */
    public static class SoapTransformationException extends RuntimeException {
        public SoapTransformationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
