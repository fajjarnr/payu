import Foundation

struct Account: Identifiable, Codable {
    let id: String
    let name: String
    let accountNumber: String
    let type: String
    let balance: Double
    let currency: String
}

struct Card: Identifiable, Codable {
    let id: String
    let cardNumber: String
    let holderName: String
    let expiry: String
    let type: String
    let isLocked: Bool
}

struct User: Codable {
    let id: String
    let fullName: String
    let email: String
    let phoneNumber: String
    
    var initials: String {
        let components = fullName.components(separatedBy: " ")
        return components.map { $0.first?.uppercased() ?? "" }.joined()
    }
}

struct Transaction: Identifiable, Codable {
    let id: String
    let title: String
    let amount: Double
    let date: String
    let type: String
    let status: String
}

struct BalanceResponse: Codable {
    let balance: Double
    let available: Double
}
