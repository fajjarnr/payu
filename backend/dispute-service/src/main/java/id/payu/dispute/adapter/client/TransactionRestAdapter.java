package id.payu.dispute.adapter.client;

import id.payu.dispute.domain.model.TransactionDetails;
import id.payu.dispute.domain.port.out.TransactionLookupPort;
import id.payu.dispute.dto.TransactionRefundDetailsResponse;
import id.payu.shared.restclient.PayuRestClient;
import id.payu.shared.restclient.RestClientErrorHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.AbstractOAuth2Token;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.Optional;
import java.util.UUID;

/**
 * HTTP adapter for the transaction service refund-details query.
 */
@Component
@RequiredArgsConstructor
public class TransactionRestAdapter implements TransactionLookupPort {

    private final PayuRestClient restClient;

    @Value("${TRANSACTION_SERVICE_URL:http://transaction-service:8080}")
    private String transactionServiceUrl;

    @Override
    public Optional<TransactionDetails> findById(UUID transactionId) {
        HttpHeaders headers = new HttpHeaders();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getCredentials() instanceof AbstractOAuth2Token token)) {
            throw new IllegalStateException("No bearer token available for transaction lookup");
        }
        headers.setBearerAuth(token.getTokenValue());

        try {
            ResponseEntity<TransactionRefundDetailsResponse> response = restClient.getWithHeaders(
                    "transaction-service",
                    transactionServiceUrl + "/api/v1/transactions/internal/" + transactionId + "/refund-details",
                    headers,
                    TransactionRefundDetailsResponse.class);
            TransactionRefundDetailsResponse body = response.getBody();
            return body == null
                    ? Optional.empty()
                    : Optional.of(new TransactionDetails(body.amount(), body.currency(),
                            body.senderAccountId(), body.recipientAccountId()));
        } catch (RestClientException exception) {
            if (isNotFound(exception)) {
                return Optional.empty();
            }
            throw exception;
        }
    }

    private boolean isNotFound(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof RestClientErrorHandler.ExternalServiceClientException clientException
                    && "EXT_NOT_FOUND".equals(clientException.getCode())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
