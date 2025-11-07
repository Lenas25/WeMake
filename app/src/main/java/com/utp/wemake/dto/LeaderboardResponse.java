package com.utp.wemake.dto;

import com.google.gson.annotations.SerializedName;

public class LeaderboardResponse {
    @SerializedName("rank")
    public int rank;
    @SerializedName("name")
    public String name;
    @SerializedName("photoUrl")
    public String photoUrl;
    @SerializedName("points")
    public int points;
}
