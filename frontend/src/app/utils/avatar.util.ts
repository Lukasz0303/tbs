export function getAvatarPath(avatarType: number | null | undefined): string {
  if (!avatarType || avatarType < 1 || avatarType > 6) {
    return 'assets/1_3.png';
  }
  return `assets/${avatarType}_3.png`;
}

export const AVAILABLE_AVATARS = [1, 2, 3, 4, 5, 6] as const;

export type AvatarType = typeof AVAILABLE_AVATARS[number];

