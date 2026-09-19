package com.amstudio.drpoint.network;

import android.content.Context;

import com.amstudio.drpoint.BuildConfig;
import com.amstudio.drpoint.DoctorPointApp;
import com.amstudio.drpoint.util.PreferenceManager;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SupabaseClient {

    private static Retrofit retrofit;
    private static SupabaseAuthService authService;
    private static SupabaseDoctorService doctorService;
    private static SupabaseAppointmentService appointmentService;
    private static SupabaseSlotService slotService;
    private static SupabasePatientService patientService;
    private static SupabaseMedicalRecordService medicalRecordService;
    private static SupabaseNotificationService notificationService;

    private static synchronized Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            Interceptor headersInterceptor = chain -> {
                Request original = chain.request();

                String authHeader = "Bearer " + BuildConfig.SUPABASE_KEY;
                Context appContext = DoctorPointApp.getAppContext();
                if (appContext != null) {
                    String accessToken = PreferenceManager.getInstance(appContext).getAccessToken();
                    if (accessToken != null && !accessToken.trim().isEmpty() && accessToken.startsWith("eyJ")) {
                        authHeader = "Bearer " + accessToken;
                    }
                }

                Request request = original.newBuilder()
                        .header("apikey", BuildConfig.SUPABASE_KEY)
                        .header("Authorization", authHeader)
                        .header("Content-Type", "application/json")
                        .method(original.method(), original.body())
                        .build();

                Response response = chain.proceed(request);

                // If JWT expired (HTTP 401), automatically retry once using Anon API Key
                if (response.code() == 401 && !authHeader.equals("Bearer " + BuildConfig.SUPABASE_KEY)) {
                    response.close();
                    Request retryRequest = original.newBuilder()
                            .header("apikey", BuildConfig.SUPABASE_KEY)
                            .header("Authorization", "Bearer " + BuildConfig.SUPABASE_KEY)
                            .header("Content-Type", "application/json")
                            .method(original.method(), original.body())
                            .build();
                    return chain.proceed(retryRequest);
                }

                return response;
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

    public static synchronized SupabaseAppointmentService getAppointmentService() {
        if (appointmentService == null) {
            appointmentService = getRetrofitInstance().create(SupabaseAppointmentService.class);
        }
        return appointmentService;
    }

    public static synchronized SupabaseSlotService getSlotService() {
        if (slotService == null) {
            slotService = getRetrofitInstance().create(SupabaseSlotService.class);
        }
        return slotService;
    }

    public static synchronized SupabasePatientService getPatientService() {
        if (patientService == null) {
            patientService = getRetrofitInstance().create(SupabasePatientService.class);
        }
        return patientService;
    }

    public static synchronized SupabaseMedicalRecordService getMedicalRecordService() {
        if (medicalRecordService == null) {
            medicalRecordService = getRetrofitInstance().create(SupabaseMedicalRecordService.class);
        }
        return medicalRecordService;
    }

    public static synchronized SupabaseNotificationService getNotificationService() {
        if (notificationService == null) {
            notificationService = getRetrofitInstance().create(SupabaseNotificationService.class);
        }
        return notificationService;
    }
}
