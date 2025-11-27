import { DestroyRef } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { WebSocketService } from './websocket.service';
import { LoggerService } from './logger.service';

describe('WebSocketService', () => {
  let service: WebSocketService;
  let loggerMock: {
    error: jest.Mock;
    debug: jest.Mock;
    warn: jest.Mock;
  };

  const createDestroyRefMock = (): DestroyRef =>
    ({
      onDestroy: (callback: () => void) => {
        return () => {};
      },
    } as unknown as DestroyRef);

  beforeEach(() => {
    loggerMock = {
      error: jest.fn(),
      debug: jest.fn(),
      warn: jest.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        WebSocketService,
        { provide: LoggerService, useValue: loggerMock },
        { provide: DestroyRef, useValue: createDestroyRefMock() },
      ],
    });

    service = TestBed.inject(WebSocketService);
  });

  it('should reset reconnect attempts after hitting the limit', () => {
    const disconnectSpy = jest.spyOn(service, 'disconnect').mockImplementation(() => {});
    (service as any).reconnectAttempts = (service as any).maxReconnectAttempts;

    (service as any).handleReconnect(42);

    expect(disconnectSpy).toHaveBeenCalled();
    expect(loggerMock.error).toHaveBeenCalled();
    expect((service as any).reconnectAttempts).toBe(0);
  });
});

