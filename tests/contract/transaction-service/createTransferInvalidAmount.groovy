package contracts.transaction

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should return RFC 9457 problem+json (400) for invalid transfer payload (missing amount)"
    request {
        method POST()
        url "/api/v1/transactions/transfer"
        headers {
            header("Authorization", "Bearer {{token}}")
            header("Content-Type", "application/json")
            header("X-Idempotency-Key", $(anyUuid()))
        }
        body([
            sourceAccountId: $(anyUuid()),
            destinationAccountId: $(anyUuid()),
            currency: "IDR",
            description: "missing amount"
        ])
    }
    response {
        status BAD_REQUEST()
        headers {
            contentType("application/problem+json")
        }
        body(
            type: $(anyNonBlankString()),
            title: "Validation failed",
            status: 400,
            detail: $(anyNonBlankString()),
            error_code: $(anyNonBlankString()),
            trace_id: $(anyNonBlankString()),
            timestamp: $(anyNonBlankString())
        )
    }
}
