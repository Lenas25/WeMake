package com.utp.wemake.services;

import com.utp.wemake.dto.DashboardResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {
    @GET("dashboard/{board_id}")
    Call<DashboardResponse> getDashboardData(@Path("board_id") String boardId);
}