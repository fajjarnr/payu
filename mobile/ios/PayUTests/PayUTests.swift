import XCTest
@testable import PayU

final class PayUTests: XCTestCase {
    
    override func setUpWithError() throws {
        // Put setup code here. This method is called before the invocation of each test method in the class.
    }
    
    override func tearDownWithError() throws {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
    }
    
    func testAccountModel() throws {
        let account = Account(
            id: "1",
            name: "Primary Account",
            accountNumber: "1234567890",
            type: "savings",
            balance: 5000000.0,
            currency: "IDR"
        )
        
        XCTAssertEqual(account.id, "1")
        XCTAssertEqual(account.name, "Primary Account")
        XCTAssertEqual(account.balance, 5000000.0)
        XCTAssertEqual(account.currency, "IDR")
    }
    
    func testCardModel() throws {
        let card = Card(
            id: "1",
            cardNumber: "**** **** **** 1234",
            holderName: "John Doe",
            expiry: "12/27",
            type: "virtual",
            isLocked: false
        )
        
        XCTAssertEqual(card.id, "1")
        XCTAssertEqual(card.type, "virtual")
        XCTAssertFalse(card.isLocked)
    }
    
    func testUserInitials() throws {
        let user = User(
            id: "1",
            fullName: "John Doe",
            email: "john@example.com",
            phoneNumber: "08123456789"
        )
        
        XCTAssertEqual(user.initials, "JD")
    }
    
    func testAPIClientSingleton() throws {
        let client1 = APIClient.shared
        let client2 = APIClient.shared
        
        XCTAssertTrue(client1 === client2, "APIClient should be a singleton")
    }
    
    func testAppState() throws {
        let appState = AppState()
        
        XCTAssertNil(appState.user)
        XCTAssertFalse(appState.isAuthenticated)
        
        let user = User(
            id: "1",
            fullName: "Test User",
            email: "test@example.com",
            phoneNumber: "08123456789"
        )
        
        appState.setUser(user)
        
        XCTAssertNotNil(appState.user)
        XCTAssertTrue(appState.isAuthenticated)
        XCTAssertEqual(appState.user?.fullName, "Test User")
        
        appState.logout()
        
        XCTAssertNil(appState.user)
        XCTAssertFalse(appState.isAuthenticated)
    }
}
