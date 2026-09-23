package com.amstudio.drpoint.network;

import com.amstudio.drpoint.model.Clinic;
import com.amstudio.drpoint.model.CommissionSettings;
import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.model.Speciality;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.Query;

public interface SupabaseDoctorService {

    @GET("rest/v1/doctors?select=*")
    Call<List<Doctor>> getDoctors();

    @GET("rest/v1/doctors?select=*")
    Call<List<Doctor>> getDoctorById(@Query("id") String idQuery);

    @GET("rest/v1/specialities?select=*")
    Call<List<Speciality>> getSpecialities();

    @GET("rest/v1/clinics?select=*&is_active=eq.true")
    Call<List<Clinic>> getDoctorClinics(@Query("doctor_id") String doctorIdQuery);

    @GET("rest/v1/commission_settings?select=*")
    Call<List<CommissionSettings>> getCommissionSettings();

    @PATCH("rest/v1/doctors")
    Call<Void> updateDoctorEarnings(
            @Query("id") String idQuery,
            @Header("Prefer") String preferHeader,
            @Body Map<String, Object> earningsMap
    );
}
