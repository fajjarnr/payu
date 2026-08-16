import { PayUClient } from '../../src/client';
import { PaymentsApi, TransfersApi, WalletsApi, TransactionsApi } from '../../src/generated/api';

describe('PayUClient resources', () => {
  const config = {
    apiKey: 'test-key',
    apiSecret: 'test-secret'
  };

  it('exposes typed resource clients without crashing', () => {
    const client = new PayUClient(config);

    expect(client.payments).toBeInstanceOf(PaymentsApi);
    expect(client.transfers).toBeInstanceOf(TransfersApi);
    expect(client.wallets).toBeInstanceOf(WalletsApi);
    expect(client.transactions).toBeInstanceOf(TransactionsApi);
  });

  it('caches resource instances', () => {
    const client = new PayUClient(config);
    expect(client.payments).toBe(client.payments);
  });

  it('requires apiKey and apiSecret', () => {
    expect(() => new PayUClient({ apiKey: 'k' } as never)).toThrow('API Secret is required');
    expect(() => new PayUClient({} as never)).toThrow('API Key is required');
  });
});
