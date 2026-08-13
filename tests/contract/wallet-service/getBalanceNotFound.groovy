package contracts.wallet

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should return RFC 9457 problem+json (404) for unknown wallet"
    request {
        method GET()
        url "/api/v1/wallets/550e8400-e29b-41d4-a716-446655440001/balance"
        headers {
            header("Authorization", "Bearer {{token}}")
            header("Content-Type", "application/json")
        }
    }
    response {
        status NOT_FOUND()
        headers {
            contentType("application/problem+json")
        }
        body(
            type: $(anyNonBlankString()),
            title: $(anyNonBlankString()),
            status: 404,
            detail: $(anyNonBlankString()),
            error_code: $(anyNonBlankString()),
            trace_id: $(anyNonBlankString()),
            timestamp: $(anyNonBlankString())
        )
    }
}
