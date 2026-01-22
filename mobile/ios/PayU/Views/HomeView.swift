import SwiftUI

struct HomeView: View {
    @EnvironmentObject var appState: AppState
    @State private var balance: Double = 0
    @State private var isLoading = true
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 20) {
                    if isLoading {
                        ProgressView()
                            .scaleEffect(1.5)
                    } else {
                        balanceCard
                        quickActions
                        recentTransactions
                    }
                }
                .padding()
            }
            .navigationTitle("Home")
            .onAppear {
                loadBalance()
            }
        }
    }
    
    private var balanceCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Total Balance")
                .font(.headline)
                .foregroundColor(.secondary)
            
            Text("IDR \(String(format: "%.2f", balance))")
                .font(.system(size: 32, weight: .bold))
            
            Text("Available")
                .font(.caption)
                .foregroundColor(.green)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(
            LinearGradient(
                gradient: Gradient(colors: [.blue, .purple]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .foregroundColor(.white)
        .cornerRadius(16)
    }
    
    private var quickActions: some View {
        VStack(alignment: .leading, spacing: 15) {
            Text("Quick Actions")
                .font(.headline)
            
            HStack(spacing: 20) {
                QuickActionButton(icon: "arrow.up.circle.fill", title: "Send")
                QuickActionButton(icon: "arrow.down.circle.fill", title: "Receive")
                QuickActionButton(icon: "qrcode", title: "Scan QR")
                QuickActionButton(icon: "ellipsis.circle.fill", title: "More")
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(radius: 2)
    }
    
    private var recentTransactions: some View {
        VStack(alignment: .leading, spacing: 15) {
            Text("Recent Transactions")
                .font(.headline)
            
            VStack(spacing: 10) {
                TransactionRow(icon: "arrow.down.circle.fill", title: "Salary", amount: "+5,000,000", date: "Today", isPositive: true)
                TransactionRow(icon: "arrow.up.circle.fill", title: "Electricity Bill", amount: "-250,000", date: "Yesterday", isPositive: false)
                TransactionRow(icon: "arrow.down.circle.fill", title: "Freelance", amount: "+1,200,000", date: "Jan 20", isPositive: true)
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(radius: 2)
    }
    
    private func loadBalance() {
        Task {
            do {
                let api = APIClient.shared
                balance = try await api.getBalance()
                isLoading = false
            } catch {
                print("Error loading balance: \(error)")
                balance = 0
                isLoading = false
            }
        }
    }
}

struct QuickActionButton: View {
    let icon: String
    let title: String
    
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.system(size: 32))
                .foregroundColor(.blue)
            
            Text(title)
                .font(.caption)
                .foregroundColor(.primary)
        }
    }
}

struct TransactionRow: View {
    let icon: String
    let title: String
    let amount: String
    let date: String
    let isPositive: Bool
    
    var body: some View {
        HStack {
            Image(systemName: icon)
                .font(.system(size: 24))
                .foregroundColor(isPositive ? .green : .red)
                .frame(width: 40)
            
            VStack(alignment: .leading) {
                Text(title)
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                Text(date)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            Text(amount)
                .font(.subheadline)
                .fontWeight(.semibold)
                .foregroundColor(isPositive ? .green : .red)
        }
        .padding(.vertical, 8)
    }
}
