import Foundation

class APIClient {
    static let shared = APIClient()
    
    private let baseURL: String
    private let session: URLSession
    
    private init() {
        self.baseURL = UserDefaults.standard.string(forKey: "api_base_url") ?? "http://localhost:8080/api/v1"
        let configuration = URLSessionConfiguration.default
        configuration.timeoutIntervalForRequest = 30
        configuration.timeoutIntervalForResource = 60
        self.session = URLSession(configuration: configuration)
    }
    
    private func performRequest<T: Codable>(
        endpoint: String,
        method: String = "GET",
        body: Data? = nil
    ) async throws -> T {
        guard let url = URL(string: "\(baseURL)\(endpoint)") else {
            throw APIError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        if let token = UserDefaults.standard.string(forKey: "auth_token") {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        if let body = body {
            request.httpBody = body
        }
        
        let (data, response) = try await session.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.invalidResponse
        }
        
        guard 200..<300 ~= httpResponse.statusCode else {
            throw APIError.httpError(statusCode: httpResponse.statusCode)
        }
        
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        
        do {
            return try decoder.decode(T.self, from: data)
        } catch {
            throw APIError.decodingError(error)
        }
    }
    
    func getBalance() async throws -> Double {
        let response: BalanceResponse = try await performRequest(endpoint: "/accounts/balance")
        return response.balance
    }
    
    func getAccounts() async throws -> [Account] {
        return try await performRequest(endpoint: "/accounts")
    }
    
    func getCards() async throws -> [Card] {
        return try await performRequest(endpoint: "/cards")
    }
    
    func getTransactions() async throws -> [Transaction] {
        return try await performRequest(endpoint: "/transactions")
    }
    
    func transfer(to recipient: String, amount: Double, note: String?) async throws -> Transaction {
        let requestBody = TransferRequest(
            recipientAccount: recipient,
            amount: amount,
            note: note
        )
        
        let encoder = JSONEncoder()
        let body = try encoder.encode(requestBody)
        
        return try await performRequest(
            endpoint: "/transfers",
            method: "POST",
            body: body
        )
    }
    
    func login(email: String, password: String) async throws -> AuthResponse {
        let requestBody = LoginRequest(email: email, password: password)
        
        let encoder = JSONEncoder()
        let body = try encoder.encode(requestBody)
        
        return try await performRequest(
            endpoint: "/auth/login",
            method: "POST",
            body: body
        )
    }
}

struct TransferRequest: Codable {
    let recipientAccount: String
    let amount: Double
    let note: String?
}

struct LoginRequest: Codable {
    let email: String
    let password: String
}

struct AuthResponse: Codable {
    let accessToken: String
    let refreshToken: String
    let user: User
}

enum APIError: Error, LocalizedError {
    case invalidURL
    case invalidResponse
    case httpError(statusCode: Int)
    case decodingError(Error)
    case networkError(Error)
    
    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "Invalid URL"
        case .invalidResponse:
            return "Invalid response"
        case .httpError(let statusCode):
            return "HTTP error: \(statusCode)"
        case .decodingError(let error):
            return "Decoding error: \(error.localizedDescription)"
        case .networkError(let error):
            return "Network error: \(error.localizedDescription)"
        }
    }
}
