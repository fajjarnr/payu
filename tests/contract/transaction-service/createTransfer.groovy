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
            sourceWalletId: $(anyUuid()),
            targetWalletId: $(anyUuid()),
            amount: $(anyPositiveInt()),
            currency: "IDR",
            description: $(anyNonBlankString())
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
                status: "COMPLETED",
                amount: fromRequest().body('$.amount'),
                currency: "IDR"
            ]
        ])
    }
}
