export interface User {
  userId: number;
  username: string | null;
  email: string | null;
  isGuest: boolean;
  avatar: number | null;
  totalPoints: number;
  gamesPlayed: number;
  gamesWon: number;
  createdAt: string;
  lastSeenAt: string | null;
}

