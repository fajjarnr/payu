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
            email: $(email()),
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
                accessToken: $(anyNonBlankString()),
                refreshToken: $(anyNonBlankString()),
                tokenType: "Bearer",
                expiresIn: $(anyPositiveInt())
            ]
        ])
    }
}
