package contracts.billing

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should return a bill payment by id"
    request {
        method GET()
        url "/api/v1/payments/550e8400-e29b-41d4-a716-446655440000"
        headers {
            header("Authorization", "Bearer {{token}}")
        }
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(
            success: true,
            data: [
                id: "550e8400-e29b-41d4-a716-446655440000",
                referenceNumber: $(anyNonBlankString()),
                accountId: "acct-001",
                billerCode: "PLN",
                customerId: "CUST-1",
                amount: $(anyPositiveInt()),
                status: "COMPLETED"
            ]
        )
    }
}
