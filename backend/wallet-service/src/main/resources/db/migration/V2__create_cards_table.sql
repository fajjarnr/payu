CREATE TABLE cards (
    id UUID PRIMARY KEY,
    wallet_id UUID NOT NULL REFERENCES wallets(id),
    card_number VARCHAR(16) NOT NULL UNIQUE,
    cvv VARCHAR(3) NOT NULL,
    expiry_date VARCHAR(5) NOT NULL, -- MM/YY
    card_holder_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    daily_limit DECIMAL(19, 2),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_cards_wallet_id ON cards(wallet_id);
CREATE INDEX idx_cards_card_number ON cards(card_number);
