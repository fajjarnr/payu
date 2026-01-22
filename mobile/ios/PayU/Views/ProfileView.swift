import SwiftUI

struct ProfileView: View {
    @EnvironmentObject var appState: AppState
    @State private var showingLogoutAlert = false
    
    var body: some View {
        NavigationView {
            List {
                Section {
                    HStack(spacing: 16) {
                        Circle()
                            .fill(Color.blue.opacity(0.2))
                            .frame(width: 60, height: 60)
                            .overlay(
                                Text(appState.user?.initials ?? "U")
                                    .font(.title)
                                    .fontWeight(.bold)
                                    .foregroundColor(.blue)
                            )
                        
                        VStack(alignment: .leading) {
                            Text(appState.user?.fullName ?? "User Name")
                                .font(.headline)
                            
                            Text(appState.user?.email ?? "user@example.com")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding(.vertical, 8)
                }
                
                Section("Account") {
                    NavigationLink(destination: Text("Personal Information")) {
                        Label("Personal Information", systemImage: "person.circle")
                    }
                    
                    NavigationLink(destination: Text("Security Settings")) {
                        Label("Security Settings", systemImage: "lock.shield")
                    }
                    
                    NavigationLink(destination: Text("Notification Settings")) {
                        Label("Notifications", systemImage: "bell")
                    }
                }
                
                Section("Support") {
                    NavigationLink(destination: Text("Help Center")) {
                        Label("Help Center", systemImage: "questionmark.circle")
                    }
                    
                    NavigationLink(destination: Text("Contact Us")) {
                        Label("Contact Us", systemImage: "envelope")
                    }
                }
                
                Section {
                    Button(action: { showingLogoutAlert = true }) {
                        HStack {
                            Text("Log Out")
                                .foregroundColor(.red)
                            
                            Spacer()
                        }
                    }
                }
            }
            .navigationTitle("Profile")
            .alert("Log Out", isPresented: $showingLogoutAlert) {
                Button("Cancel", role: .cancel) { }
                Button("Log Out", role: .destructive) {
                    appState.logout()
                }
            } message: {
                Text("Are you sure you want to log out?")
            }
        }
    }
}
