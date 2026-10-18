export interface LoginRequestDto {
  username: string;
  password: string;
}

export interface RegisterRequestDto {
  username: string;
  email: string;
  password: string;
}

export interface UserResponseDto {
  id: number;
  username: string;
  email: string;
}

export interface TokenResponseDto {
  token: string;
  tokenType: string;
  expiresAt: string;
}
