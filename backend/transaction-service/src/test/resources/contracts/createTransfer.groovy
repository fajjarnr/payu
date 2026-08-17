package contracts.transaction

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should create a transfer transaction"
    request {
        method POST()
        url "/api/v1/transactions/transfer"
        headers {
            header("Authorization", "Bearer {{token}}")
            header("Content-Type", "application/json")
            header("X-Idempotency-Key", $(anyUuid()))
        }
        body([
            senderAccountId: $(c("550e8400-e29b-41d4-a716-446655440000"), p(anyUuid())),
            recipientAccountNumber: "1234567890",
            amount: $(c(50000.00), p(anyNumber())),
            currency: "IDR",
            description: $(c("Test internal transfer"), p(anyNonBlankString())),
            type: "INTERNAL_TRANSFER"
        ])
    }
    response {
        status CREATED()
        headers {
            contentType(applicationJson())
        }
        body([
            success: true,
            data: [
                transactionId: $(anyUuid()),
                referenceNumber: $(anyNonBlankString()),
                status: "PENDING",
                fee: $(c(regex('[0-9]+(\\\\.[0-9]{1,2})?')))
            ],
            meta: [
                requestId: $(anyNonBlankString()),
                timestamp: $(anyNonBlankString())
            ]
        ])
    }
}
