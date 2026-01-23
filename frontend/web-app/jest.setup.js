import '@testing-library/jest-dom';

global.IntersectionObserver = jest.fn().mockImplementation(() => ({
  disconnect: jest.fn(),
  observe: jest.fn(),
  takeRecords: jest.fn(),
  unobserve: jest.fn(),
}));
