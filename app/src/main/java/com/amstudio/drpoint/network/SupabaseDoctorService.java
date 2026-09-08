package com.amstudio.drpoint.network;

import com.amstudio.drpoint.model.Doctor;
import com.amstudio.drpoint.model.Speciality;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface SupabaseDoctorService {

    @GET("rest/v1/doctors?select=*")
    Call<List<Doctor>> getDoctors();

    @GET("rest/v1/specialities?select=*")
    Call<List<Speciality>> getSpecialities();
}
