package com.rockwonitglobal.jejudiver;

import com.rockwonitglobal.jejudiver.models.Model;
import com.rockwonitglobal.jejudiver.models.Result;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface APiRequest {
    @FormUrlEncoded
    @POST("apiDevice/setData")
    Call<Result> sendData(@Field("data") String data);

    @GET("apiDevice/setData")
    Call<Model> getAllData();
}
