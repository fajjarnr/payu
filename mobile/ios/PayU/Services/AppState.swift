import Foundation

class AppState: ObservableObject {
    @Published var user: User?
    @Published var isAuthenticated = false
    
    init() {
        loadUser()
    }
    
    func setUser(_ user: User) {
        self.user = user
        self.isAuthenticated = true
        saveUser(user)
    }
    
    func logout() {
        self.user = nil
        self.isAuthenticated = false
        UserDefaults.standard.removeObject(forKey: "user")
        UserDefaults.standard.removeObject(forKey: "auth_token")
    }
    
    private func saveUser(_ user: User) {
        let encoder = JSONEncoder()
        if let encoded = try? encoder.encode(user) {
            UserDefaults.standard.set(encoded, forKey: "user")
        }
    }
    
    private func loadUser() {
        if let data = UserDefaults.standard.data(forKey: "user"),
           let user = try? JSONDecoder().decode(User.self, from: data) {
            self.user = user
            self.isAuthenticated = true
        }
    }
}
