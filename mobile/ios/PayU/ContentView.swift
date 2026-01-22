import SwiftUI

struct ContentView: View {
    @EnvironmentObject var appState: AppState
    @State private var selectedTab: Tab = .home
    
    enum Tab {
        case home, accounts, transfers, cards, profile
    }
    
    var body: some View {
        TabView(selection: $selectedTab) {
            HomeView()
                .tabItem {
                    Label("Home", systemImage: "house.fill")
                }
                .tag(Tab.home)
            
            AccountsView()
                .tabItem {
                    Label("Accounts", systemImage: "banknote.fill")
                }
                .tag(Tab.accounts)
            
            TransfersView()
                .tabItem {
                    Label("Transfers", systemImage: "arrow.left.arrow.right")
                }
                .tag(Tab.transfers)
            
            CardsView()
                .tabItem {
                    Label("Cards", systemImage: "creditcard.fill")
                }
                .tag(Tab.cards)
            
            ProfileView()
                .tabItem {
                    Label("Profile", systemImage: "person.fill")
                }
                .tag(Tab.profile)
        }
        .accentColor(.blue)
    }
}
