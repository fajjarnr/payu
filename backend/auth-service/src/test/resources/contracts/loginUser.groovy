package contracts.auth

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should authenticate user and return token"
    request {
        method POST()
        url "/api/v1/auth/login"
        headers {
            header("Content-Type", "application/json")
        }
        body([
            username: $(anyNonBlankString()),
            password: $(anyNonBlankString())
        ])
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body([
            success: true,
            data: [
                access_token: $(anyNonBlankString()),
                refresh_token: $(anyNonBlankString()),
                token_type: "Bearer",
                expires_in: $(anyPositiveInt())
            ],
            meta: [
                requestId: $(anyNonBlankString()),
                timestamp: $(anyNonBlankString())
            ]
        ])
    }
}
