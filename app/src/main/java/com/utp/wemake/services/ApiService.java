package com.utp.wemake.services;

import com.utp.wemake.dto.DashboardResponse;
import com.utp.wemake.dto.LeaderboardResponse;
import com.utp.wemake.dto.UserSummaryResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {
    @GET("dashboard/{board_id}")
    Call<DashboardResponse> getDashboardData(@Path("board_id") String boardId);

    @GET("leaderboard/{board_id}")
    Call<List<LeaderboardResponse>> getLeaderboard(@Path("board_id") String boardId);

    @GET("summary/{board_id}/{user_id}")
    Call<UserSummaryResponse> getUserSummary(
            @Path("board_id") String boardId,
            @Path("user_id") String userId
    );
}