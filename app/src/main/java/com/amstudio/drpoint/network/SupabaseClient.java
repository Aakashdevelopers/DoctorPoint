package com.amstudio.drpoint.network;

import com.amstudio.drpoint.BuildConfig;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SupabaseClient {

    private static Retrofit retrofit;
    private static SupabaseAuthService authService;
    private static SupabaseDoctorService doctorService;

    private static synchronized Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            Interceptor headersInterceptor = chain -> {
                Request original = chain.request();
                Request request = original.newBuilder()
                        .header("apikey", BuildConfig.SUPABASE_KEY)
                        .header("Authorization", "Bearer " + BuildConfig.SUPABASE_KEY)
                        .header("Content-Type", "application/json")
                        .method(original.method(), original.body())
                        .build();
                return chain.proceed(request);
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(headersInterceptor)
                    .addInterceptor(loggingInterceptor)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.SUPABASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static synchronized SupabaseAuthService getAuthService() {
        if (authService == null) {
            authService = getRetrofitInstance().create(SupabaseAuthService.class);
        }
        return authService;
    }

    public static synchronized SupabaseDoctorService getDoctorService() {
        if (doctorService == null) {
            doctorService = getRetrofitInstance().create(SupabaseDoctorService.class);
        }
        return doctorService;
    }
}
