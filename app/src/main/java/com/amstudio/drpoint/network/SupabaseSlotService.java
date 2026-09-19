package com.amstudio.drpoint.network;

import com.amstudio.drpoint.model.DoctorSlot;
import com.amstudio.drpoint.network.model.BookAppointmentRpcRequest;
import com.amstudio.drpoint.network.model.BookAppointmentRpcResponse;
import com.amstudio.drpoint.network.model.CancelAppointmentRpcRequest;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SupabaseSlotService {

    @GET("rest/v1/doctor_slots?select=*&status=eq.available&order=slot_date.asc,start_time.asc")
    Call<List<DoctorSlot>> getAvailableSlots(
            @Query("doctor_id") String doctorIdQuery,
            @Query("slot_date") String slotDateQuery
    );

    @GET("rest/v1/doctor_slots?select=*&status=eq.available&order=slot_date.asc,start_time.asc")
    Call<List<DoctorSlot>> getAllFutureAvailableSlots(
            @Query("slot_date") String slotDateQuery
    );

    @GET("rest/v1/doctor_slots?select=*&order=slot_date.asc,start_time.asc")
    Call<List<DoctorSlot>> getAllFutureDoctorSlots(
            @Query(value = "doctor_id", encoded = true) String doctorIdQuery,
            @Query(value = "slot_date", encoded = true) String slotDateQuery
    );

    @GET("rest/v1/doctor_slots?select=*&order=slot_date.asc,start_time.asc")
    Call<List<DoctorSlot>> getDoctorAllSlots(
            @Query(value = "doctor_id", encoded = true) String doctorIdQuery,
            @Query(value = "slot_date", encoded = true) String slotDateQuery
    );

    @GET("rest/v1/doctor_slots?select=*&order=slot_date.asc,start_time.asc")
    Call<List<DoctorSlot>> getAllSlots();

    @GET("rest/v1/doctor_schedules?select=*&order=slot_date.asc")
    Call<List<DoctorSlot>> getDoctorSchedules(
            @Query(value = "doctor_id", encoded = true) String doctorIdQuery,
            @Query(value = "slot_date", encoded = true) String slotDateQuery
    );

    @GET("rest/v1/doctor_schedules?select=*&order=slot_date.asc")
    Call<List<DoctorSlot>> getAllDoctorSchedules();

    @POST("rest/v1/doctor_slots")
    Call<List<DoctorSlot>> createSlot(
            @Header("Prefer") String preferHeader,
            @Body DoctorSlot slot
    );

    @POST("rest/v1/rpc/book_appointment")
    Call<BookAppointmentRpcResponse> bookAppointment(
            @Body BookAppointmentRpcRequest request
    );

    @POST("rest/v1/rpc/cancel_appointment")
    Call<Map<String, Object>> cancelAppointment(
            @Body CancelAppointmentRpcRequest request
    );
}
