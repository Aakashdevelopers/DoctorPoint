package com.amstudio.drpoint.network;

import com.amstudio.drpoint.model.MedicalRecord;

import java.util.List;
import java.util.Map;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface SupabaseMedicalRecordService {

    @GET("rest/v1/medical_records?select=*&order=record_date.desc,created_at.desc")
    Call<List<MedicalRecord>> getMedicalRecordsForPatient(@Query("patient_id") String patientIdQuery);

    @POST("rest/v1/medical_records")
    Call<List<MedicalRecord>> createMedicalRecord(
            @Header("Prefer") String preferHeader,
            @Body MedicalRecord record
    );

    @DELETE("rest/v1/medical_records")
    Call<Void> deleteMedicalRecord(@Query("id") String idQuery);

    @POST("storage/v1/object/medical-records/{path}")
    Call<ResponseBody> uploadStorageFile(
            @Path(value = "path", encoded = true) String path,
            @Header("Content-Type") String contentType,
            @Body RequestBody fileBody
    );

    @DELETE("storage/v1/object/medical-records/{path}")
    Call<ResponseBody> deleteStorageFile(
            @Path(value = "path", encoded = true) String path
    );

    @POST("storage/v1/object/sign/medical-records/{path}")
    Call<Map<String, Object>> getSignedUrl(
            @Path(value = "path", encoded = true) String path,
            @Body Map<String, Object> body
    );
}
