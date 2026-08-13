package contracts.auth

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should return RFC 9457 problem+json (400) for OIDC callback missing code"
    request {
        method POST()
        url "/api/v1/auth/callback"
        headers {
            header("Content-Type", "application/json")
        }
        body([
            codeVerifier: "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._",
            redirectUri: $(anyNonBlankString())
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
