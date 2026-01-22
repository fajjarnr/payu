import SwiftUI

struct CardsView: View {
    @State private var cards: [Card] = []
    @State private var isLoading = true
    
    var body: some View {
        NavigationView {
        ZStack {
            if isLoading {
                ProgressView()
            } else {
                ScrollView {
                    VStack(spacing: 20) {
                        ForEach(cards) { card in
                            CardView(card: card)
                        }
                        
                        Button(action: addNewCard) {
                            Label("Add New Card", systemImage: "plus.circle.fill")
                                .font(.headline)
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color.blue)
                                .cornerRadius(12)
                        }
                    }
                    .padding()
                }
            }
        }
        .navigationTitle("Cards")
        .onAppear {
            loadCards()
        }
    }
}
    
    private func loadCards() {
        Task {
            do {
                let api = APIClient.shared
                cards = try await api.getCards()
                isLoading = false
            } catch {
                print("Error loading cards: \(error)")
                isLoading = false
            }
        }
    }
    
    private func addNewCard() {
        
    }
}

struct CardView: View {
    let card: Card
    
    var body: some View {
        ZStack {
            LinearGradient(
                gradient: Gradient(colors: card.type == "virtual" ? [.purple, .pink] : [.blue, .cyan]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .cornerRadius(16)
            .padding(4)
            
            VStack(alignment: .leading, spacing: 20) {
                HStack {
                    Image(systemName: "creditcard")
                        .font(.title)
                    
                    Spacer()
                    
                    Text(card.type.uppercased())
                        .font(.caption)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color.white.opacity(0.2))
                        .cornerRadius(8)
                }
                
                Text(card.cardNumber)
                    .font(.system(size: 22, weight: .bold, design: .monospaced))
                    .tracking(2)
                
                HStack {
                    VStack(alignment: .leading) {
                        Text("Card Holder")
                            .font(.caption)
                            .foregroundColor(.white.opacity(0.7))
                        
                        Text(card.holderName)
                            .font(.subheadline)
                            .fontWeight(.semibold)
                    }
                    
                    Spacer()
                    
                    VStack(alignment: .trailing) {
                        Text("Expires")
                            .font(.caption)
                            .foregroundColor(.white.opacity(0.7))
                        
                        Text(card.expiry)
                            .font(.subheadline)
                            .fontWeight(.semibold)
                    }
                }
            }
            .padding()
            .foregroundColor(.white)
        }
        .frame(height: 200)
    }
}
