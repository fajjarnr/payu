import SwiftUI

struct AccountsView: View {
    @State private var accounts: [Account] = []
    @State private var isLoading = true
    
    var body: some View {
        NavigationView {
            ZStack {
                if isLoading {
                    ProgressView()
                } else {
                    List {
                        ForEach(accounts) { account in
                            AccountRow(account: account)
                        }
                    }
                }
            }
            .navigationTitle("Accounts")
            .onAppear {
                loadAccounts()
            }
        }
    }
    
    private func loadAccounts() {
        Task {
            do {
                let api = APIClient.shared
                accounts = try await api.getAccounts()
                isLoading = false
            } catch {
                print("Error loading accounts: \(error)")
                isLoading = false
            }
        }
    }
}

struct AccountRow: View {
    let account: Account
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(account.name)
                    .font(.headline)
                
                Spacer()
                
                Text("IDR \(String(format: "%.2f", account.balance))")
                    .font(.headline)
                    .foregroundColor(.blue)
            }
            
            Text(account.accountNumber)
                .font(.caption)
                .foregroundColor(.secondary)
            
            Text(account.type)
                .font(.caption)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(Color.blue.opacity(0.1))
                .foregroundColor(.blue)
                .cornerRadius(8)
        }
        .padding(.vertical, 4)
    }
}
