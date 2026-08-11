package contracts.auth

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should exchange OIDC authorization code with PKCE verifier and return token"
    request {
        method POST()
        url "/api/v1/auth/callback"
        headers {
            header("Content-Type", "application/json")
        }
        body([
            code: $(anyNonBlankString()),
            codeVerifier: $(anyNonBlankString()),
            redirectUri: $(anyNonBlankString())
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
