package com.amstudio.drpoint.network;

import com.amstudio.drpoint.model.PatientProfile;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SupabasePatientService {

    @GET("rest/v1/profiles?select=*")
    Call<List<PatientProfile>> getProfile(@Query("id") String idQuery);

    @GET("rest/v1/patients?select=*")
    Call<List<PatientProfile>> getPatientDetails(@Query("id") String idQuery);

    @PATCH("rest/v1/profiles")
    Call<Void> updateProfile(
            @Query("id") String idQuery,
            @Body Map<String, Object> profileData
    );

    @PATCH("rest/v1/patients")
    Call<Void> updatePatientDetails(
            @Query("id") String idQuery,
            @Body Map<String, Object> patientData
    );

    @POST("rest/v1/patients")
    Call<Void> createPatientRecord(
            @Header("Prefer") String preferHeader,
            @Body Map<String, Object> patientData
    );

    @POST("rest/v1/profiles")
    Call<Void> createProfileRecord(
            @Header("Prefer") String preferHeader,
            @Body Map<String, Object> profileData
    );
}
