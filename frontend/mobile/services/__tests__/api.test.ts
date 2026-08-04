import axios from 'axios';
import type { AxiosAdapter, AxiosResponse } from 'axios';
import { apiClientInstance } from '../api';

describe('ApiClient request deduplication', () => {
  const client = apiClientInstance.getInstance();
  let originalAdapter: AxiosAdapter | AxiosAdapter[] | undefined;

  beforeEach(() => {
    originalAdapter = client.defaults.adapter;
    apiClientInstance.cancelAllRequests();
    apiClientInstance.clearPendingIdempotencyKeys();
  });

  afterEach(() => {
    client.defaults.adapter = originalAdapter;
    apiClientInstance.cancelAllRequests();
    apiClientInstance.clearPendingIdempotencyKeys();
  });

  it('does not cancel concurrent financial POSTs with different bodies', async () => {
    const resolvers: (() => void)[] = [];
    const adapter = jest.fn((config) => new Promise<AxiosResponse>((resolve, reject) => {
      const onAbort = () => reject(new axios.CanceledError('request cancelled'));
      if (config.signal?.aborted) {
        onAbort();
        return;
      }

      config.signal?.addEventListener('abort', onAbort, { once: true });
      resolvers.push(() => {
        config.signal?.removeEventListener('abort', onAbort);
        resolve({
          config,
          data: { accepted: true },
          headers: {},
          request: {},
          status: 200,
          statusText: 'OK',
        });
      });
    }));
    client.defaults.adapter = adapter;

    const first = apiClientInstance.postWithIdempotency(
      '/transfers',
      { amount: 100, recipientAccount: 'ACC-001' },
      'transfer::one'
    );
    const second = apiClientInstance.postWithIdempotency(
      '/transfers',
      { amount: 200, recipientAccount: 'ACC-002' },
      'transfer::two'
    );

    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(adapter).toHaveBeenCalledTimes(2);
    resolvers.forEach((resolve) => resolve());

    await expect(Promise.all([first, second])).resolves.toEqual([
      { accepted: true },
      { accepted: true },
    ]);
  });
});
