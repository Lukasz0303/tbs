import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { User } from '../models/user.model';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  const apiUrl = 'http://localhost:8080/api';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  describe('loadCurrentUser', () => {
    it('should load current user successfully', (done) => {
      const mockResponse = {
        userId: 1,
        username: 'testuser',
        email: 'test@example.com',
        isGuest: false,
        avatar: 2,
        totalPoints: 100,
        gamesPlayed: 10,
        gamesWon: 5,
        createdAt: '2024-01-01T00:00:00Z',
        lastSeenAt: '2024-01-02T00:00:00Z',
      };

      service.loadCurrentUser().subscribe((user) => {
        expect(user).toBeTruthy();
        expect(user?.userId).toBe(1);
        expect(user?.username).toBe('testuser');
        expect(user?.isGuest).toBe(false);
        done();
      });

      const req = httpMock.expectOne(`${apiUrl}/v1/auth/me`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should handle 401 error and force logout', (done) => {
      service.loadCurrentUser().subscribe((user) => {
        expect(user).toBeNull();
        done();
      });

      const req = httpMock.expectOne(`${apiUrl}/v1/auth/me`);
      req.flush(null, { status: 401, statusText: 'Unauthorized' });
    });
  });

  describe('createGuestSession', () => {
    it('should create guest session successfully', (done) => {
      const mockResponse = {
        userId: 2,
        isGuest: true,
        avatar: 1,
        totalPoints: 0,
        gamesPlayed: 0,
        gamesWon: 0,
        createdAt: '2024-01-01T00:00:00Z',
      };

      service.createGuestSession().subscribe((response) => {
        expect(response).toBeTruthy();
        expect(response.userId).toBe(2);
        expect(response.isGuest).toBe(true);
        done();
      });

      const req = httpMock.expectOne(`${apiUrl}/v1/guests`);
      expect(req.request.method).toBe('POST');
      req.flush(mockResponse);
    });
  });

  describe('login', () => {
    it('should login successfully', (done) => {
      const mockLoginResponse = {
        userId: 1,
        username: 'testuser',
        email: 'test@example.com',
        isGuest: false,
        avatar: 2,
        totalPoints: 100,
        gamesPlayed: 10,
        gamesWon: 5,
        createdAt: '2024-01-01T00:00:00Z',
        lastSeenAt: '2024-01-02T00:00:00Z',
      };

      const mockMeResponse = {
        userId: 1,
        username: 'testuser',
        email: 'test@example.com',
        isGuest: false,
        avatar: 2,
        totalPoints: 100,
        gamesPlayed: 10,
        gamesWon: 5,
        createdAt: '2024-01-01T00:00:00Z',
        lastSeenAt: '2024-01-02T00:00:00Z',
      };

      service.login('test@example.com', 'password123').subscribe((user) => {
        expect(user).toBeTruthy();
        expect(user.userId).toBe(1);
        expect(user.username).toBe('testuser');
        done();
      });

      const loginReq = httpMock.expectOne(`${apiUrl}/v1/auth/login`);
      expect(loginReq.request.method).toBe('POST');
      loginReq.flush(mockLoginResponse);

      const meReq = httpMock.expectOne(`${apiUrl}/v1/auth/me`);
      expect(meReq.request.method).toBe('GET');
      meReq.flush(mockMeResponse);
    });
  });

  describe('logout', () => {
    it('should logout registered user', (done) => {
      const user: User = {
        userId: 1,
        username: 'testuser',
        email: 'test@example.com',
        isGuest: false,
        avatar: 2,
        totalPoints: 100,
        gamesPlayed: 10,
        gamesWon: 5,
        createdAt: '2024-01-01T00:00:00Z',
        lastSeenAt: '2024-01-02T00:00:00Z',
      };

      service.updateCurrentUser(user);

      service.logout().subscribe(() => {
        done();
      });

      const req = httpMock.expectOne(`${apiUrl}/v1/auth/logout`);
      expect(req.request.method).toBe('POST');
      req.flush(null);
    });

    it('should logout guest user without API call', (done) => {
      const guestUser: User = {
        userId: 2,
        username: null,
        email: null,
        isGuest: true,
        avatar: 1,
        totalPoints: 0,
        gamesPlayed: 0,
        gamesWon: 0,
        createdAt: '2024-01-01T00:00:00Z',
        lastSeenAt: null,
      };

      service.updateCurrentUser(guestUser);

      service.logout().subscribe(() => {
        done();
      });

      httpMock.expectNone(`${apiUrl}/v1/auth/logout`);
    });
  });

  describe('isAuthenticated', () => {
    it('should return false for guest user', () => {
      const guestUser: User = {
        userId: 2,
        username: null,
        email: null,
        isGuest: true,
        avatar: 1,
        totalPoints: 0,
        gamesPlayed: 0,
        gamesWon: 0,
        createdAt: '2024-01-01T00:00:00Z',
        lastSeenAt: null,
      };

      service.updateCurrentUser(guestUser);
      expect(service.isAuthenticated()).toBe(false);
    });

    it('should return true for registered user', () => {
      const user: User = {
        userId: 1,
        username: 'testuser',
        email: 'test@example.com',
        isGuest: false,
        avatar: 2,
        totalPoints: 100,
        gamesPlayed: 10,
        gamesWon: 5,
        createdAt: '2024-01-01T00:00:00Z',
        lastSeenAt: '2024-01-02T00:00:00Z',
      };

      service.updateCurrentUser(user);
      expect(service.isAuthenticated()).toBe(true);
    });

    it('should return false when no user', () => {
      service.updateCurrentUser(null);
      expect(service.isAuthenticated()).toBe(false);
    });
  });
});

