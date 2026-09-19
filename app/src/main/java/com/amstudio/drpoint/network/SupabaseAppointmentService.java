package com.amstudio.drpoint.network;

import com.amstudio.drpoint.model.Appointment;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SupabaseAppointmentService {

    @GET("rest/v1/appointments?select=*,doctor:doctors(*)&order=created_at.desc")
    Call<List<Appointment>> getAppointmentsForPatient(@Query("patient_id") String patientIdQuery);

    @GET("rest/v1/appointments?select=*,doctor:doctors(*)&order=created_at.desc")
    Call<List<Appointment>> getAppointmentsForDoctor(@Query("doctor_id") String doctorIdQuery);

    @GET("rest/v1/appointments?select=*,doctor:doctors(*)&order=created_at.desc")
    Call<List<Appointment>> getAppointments(@Query("user_id") String userIdQuery);

    @GET("rest/v1/appointments?select=*,doctor:doctors(*)&order=created_at.desc")
    Call<List<Appointment>> getAllAppointments();

    @POST("rest/v1/appointments")
    Call<List<Appointment>> createAppointment(
            @Header("Prefer") String preferHeader,
            @Body Appointment appointment
    );

    @POST("rest/v1/appointments")
    Call<List<Map<String, Object>>> createAppointmentPayload(
            @Header("Prefer") String preferHeader,
            @Body Map<String, Object> payload
    );

    @PATCH("rest/v1/appointments")
    Call<Void> updateAppointmentStatus(
            @Query("id") String idQuery,
            @Body Map<String, Object> statusMap
    );
}
