package com.amstudio.drpoint.network;

import com.amstudio.drpoint.network.model.AuthResponse;
import com.amstudio.drpoint.network.model.LoginRequest;
import com.amstudio.drpoint.network.model.SignUpRequest;
import com.amstudio.drpoint.network.model.User;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface SupabaseAuthService {

    @POST("auth/v1/signup")
    Call<AuthResponse> signUp(@Body SignUpRequest request);

    @POST("auth/v1/token?grant_type=password")
    Call<AuthResponse> loginWithPassword(@Body LoginRequest request);

    @GET("auth/v1/user")
    Call<User> getUser(@Header("Authorization") String bearerToken);
}
