package com.HHMS;

import com.HHMS.models.Model;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface APiRequest {
    @FormUrlEncoded
    @POST("apiDevice/setData")
    Call<String> sendData(@Field("data") String data);

    @GET("apiDevice/setData")
    Call<Model> getAllData();
}
