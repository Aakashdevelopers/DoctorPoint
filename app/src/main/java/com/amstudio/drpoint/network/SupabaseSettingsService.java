package com.amstudio.drpoint.network;

import com.amstudio.drpoint.model.CustomerCareSettings;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface SupabaseSettingsService {

    @GET("rest/v1/customer_care_settings?select=*&order=id.desc&limit=1")
    Call<List<CustomerCareSettings>> getCustomerCareSettings();

    @POST("rest/v1/customer_care_settings")
    Call<Void> updateCustomerCareSettings(
            @Header("Prefer") String preferHeader,
            @Body Map<String, Object> body
    );
}
