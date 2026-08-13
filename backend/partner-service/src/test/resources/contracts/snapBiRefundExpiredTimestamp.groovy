package contracts.partner

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should reject SNAP-BI refund with expired timestamp (4002508)"
    request {
        method POST()
        url "/v1/partner/payments/550e8400-e29b-41d4-a716-446655440000/refund"
        headers {
            header("Content-Type", "application/json")
            header("Authorization", "Bearer contract-token")
            header("X-TIMESTAMP", "2020-01-01T00:00:00+07:00")
            header("X-SIGNATURE", "fixed-signature-for-contract")
        }
        body("{}")
    }
    response {
        status BAD_REQUEST()
        headers {
            contentType(applicationJson())
        }
        body(
            responseCode: "4002508",
            responseMessage: "Invalid or expired timestamp"
        )
    }
}
