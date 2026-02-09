package contracts.wallet

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should return wallet balance for valid wallet ID"
    request {
        method GET()
        url "/api/v1/wallets/balance"
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
                walletId: $(anyUuid()),
                balance: $(anyNumber()),
                currency: "IDR",
                availableBalance: $(anyNumber())
            ]
        ])
    }
}
