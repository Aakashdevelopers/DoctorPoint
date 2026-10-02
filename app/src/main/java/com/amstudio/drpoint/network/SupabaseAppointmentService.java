package com.amstudio.drpoint.network;

import com.amstudio.drpoint.model.Appointment;
import com.amstudio.drpoint.model.RefundRequest;

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

    @GET("rest/v1/appointments?select=*,doctor:doctors(*)&order=appointment_date.desc,start_time.asc")
    Call<List<Appointment>> getAppointmentsForPatient(@Query("patient_id") String patientIdQuery);

    @GET("rest/v1/appointments?select=*,doctor:doctors(*)&order=appointment_date.desc,start_time.asc")
    Call<List<Appointment>> getAppointmentsForDoctor(@Query("doctor_id") String doctorIdQuery);

    @GET("rest/v1/appointments?select=*,doctor:doctors(*)&order=appointment_date.desc,start_time.asc")
    Call<List<Appointment>> getAppointments(@Query("user_id") String userIdQuery);

    @GET("rest/v1/appointments?select=*,doctor:doctors(*)&order=appointment_date.desc,start_time.asc")
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

    @GET("rest/v1/refund_requests?select=*")
    Call<List<RefundRequest>> getRefundRequestByAppointment(@Query("appointment_id") String appointmentIdQuery);

    @POST("rest/v1/refund_requests")
    Call<Void> postRefundRequest(
            @Header("Prefer") String preferHeader,
            @Body RefundRequest refundRequest
    );

    @POST("rest/v1/refund_requests")
    Call<List<Map<String, Object>>> postRefundRequestPayload(
            @Query("on_conflict") String onConflictQuery,
            @Header("Prefer") String preferHeader,
            @Body Map<String, Object> refundMap
    );

    @PATCH("rest/v1/refund_requests")
    Call<List<Map<String, Object>>> updateRefundRequest(
            @Query("appointment_id") String appointmentIdQuery,
            @Header("Prefer") String preferHeader,
            @Body Map<String, Object> refundMap
    );
}
