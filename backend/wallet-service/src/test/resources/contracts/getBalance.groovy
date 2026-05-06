package contracts.wallet

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should return wallet balance for valid account ID"
    request {
        method GET()
        url "/api/v1/wallets/550e8400-e29b-41d4-a716-446655440000/balance"
        headers {
            header("Authorization", "Bearer {{token}}")
            header("Content-Type", "application/json")
        }
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body([
            success: true,
            data: [
                accountId: "550e8400-e29b-41d4-a716-446655440000",
                balance: $(anyNumber()),
                availableBalance: $(anyNumber()),
                reservedBalance: $(anyNumber()),
                currency: "IDR"
            ],
            meta: [
                requestId: $(anyNonBlankString()),
                timestamp: $(anyNonBlankString())
            ]
        ])
    }
}
