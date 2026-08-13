package contracts.partner

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should reject SNAP-BI auth-token request with expired timestamp (4002508)"
    request {
        method POST()
        url "/v1/partner/auth/token"
        headers {
            header("Content-Type", "application/json")
            header("X-CLIENT-KEY", "client-key-001")
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
