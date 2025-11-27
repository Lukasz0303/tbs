import 'jest-preset-angular/setup-jest';
import '@testing-library/jest-dom';

Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: jest.fn().mockImplementation((query) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: jest.fn(),
    removeListener: jest.fn(),
    addEventListener: jest.fn(),
    removeEventListener: jest.fn(),
    dispatchEvent: jest.fn(),
  })),
});

const originalError = console.error;
console.error = (...args: unknown[]) => {
  if (
    typeof args[0] === 'string' &&
    (args[0].includes('Could not parse CSS stylesheet') ||
      args[0].includes('css parsing'))
  ) {
    return;
  }
  if (
    args[0] &&
    typeof args[0] === 'object' &&
    'type' in args[0] &&
    args[0].type === 'css parsing'
  ) {
    return;
  }
  originalError.call(console, ...args);
};

