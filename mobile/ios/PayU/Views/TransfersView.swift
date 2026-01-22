import SwiftUI

struct TransfersView: View {
    @State private var recipient: String = ""
    @State private var amount: String = ""
    @State private var note: String = ""
    @State private var isProcessing = false
    @State private var showAlert = false
    @State private var alertMessage = ""
    
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Transfer Details")) {
                    TextField("Account Number / Phone", text: $recipient)
                        .keyboardType(.numberPad)
                    
                    TextField("Amount (IDR)", text: $amount)
                        .keyboardType(.numberPad)
                    
                    TextField("Note (Optional)", text: $note)
                }
                
                Section {
                    Button(action: performTransfer) {
                        HStack {
                            Spacer()
                            if isProcessing {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            } else {
                                Text("Send Transfer")
                                    .fontWeight(.bold)
                            }
                            Spacer()
                        }
                    }
                    .disabled(recipient.isEmpty || amount.isEmpty || isProcessing)
                }
            }
            .navigationTitle("Transfer")
            .alert(isPresented: $showAlert) {
                Alert(
                    title: Text("Transfer Status"),
                    message: Text(alertMessage),
                    dismissButton: .default(Text("OK"))
                )
            }
        }
    }
    
    private func performTransfer() {
        guard let transferAmount = Double(amount) else { return }
        
        isProcessing = true
        
        Task {
            do {
                let api = APIClient.shared
                _ = try await api.transfer(
                    to: recipient,
                    amount: transferAmount,
                    note: note.isEmpty ? nil : note
                )
                
                DispatchQueue.main.async {
                    alertMessage = "Transfer successful!"
                    showAlert = true
                    isProcessing = false
                    
                    recipient = ""
                    amount = ""
                    note = ""
                }
            } catch {
                DispatchQueue.main.async {
                    alertMessage = "Transfer failed: \(error.localizedDescription)"
                    showAlert = true
                    isProcessing = false
                }
            }
        }
    }
}
