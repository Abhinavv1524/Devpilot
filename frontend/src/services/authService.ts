import { axiosClient } from "../api/axiosClient";
import { endpoints } from "../api/endpoints";
import type {
  LoginRequestDto,
  RegisterRequestDto,
  TokenResponseDto,
  UserResponseDto,
} from "../types/auth";

export const authService = {
  async register(account: RegisterRequestDto): Promise<UserResponseDto> {
    const response = await axiosClient.post<UserResponseDto>(
      endpoints.auth.signup,
      account,
    );
    return response.data;
  },

  async login(credentials: LoginRequestDto): Promise<TokenResponseDto> {
    const response = await axiosClient.post<TokenResponseDto>(
      endpoints.auth.login,
      credentials,
    );
    return response.data;
  },
};
