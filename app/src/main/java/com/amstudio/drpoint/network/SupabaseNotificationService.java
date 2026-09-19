package com.amstudio.drpoint.network;

import com.amstudio.drpoint.model.NotificationItem;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.Query;

public interface SupabaseNotificationService {

    @GET("rest/v1/notifications?select=*&order=created_at.desc")
    Call<List<NotificationItem>> getNotificationsForPatient(@Query("patient_id") String patientIdQuery);

    @PATCH("rest/v1/notifications")
    Call<Void> markNotificationAsRead(
            @Query("id") String idQuery,
            @Body Map<String, Object> updateMap
    );
}
