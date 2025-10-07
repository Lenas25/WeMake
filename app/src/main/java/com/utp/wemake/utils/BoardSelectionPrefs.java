package com.utp.wemake.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class BoardSelectionPrefs {
    private static final String PREFS_NAME = "AppBoardPrefs";
    private static final String KEY_SELECTED_BOARD_ID = "selectedBoardId";
    private final SharedPreferences sharedPreferences;

    public BoardSelectionPrefs(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveSelectedBoardId(String boardId) {
        sharedPreferences.edit().putString(KEY_SELECTED_BOARD_ID, boardId).apply();
    }

    public String getSelectedBoardId() {
        return sharedPreferences.getString(KEY_SELECTED_BOARD_ID, null);
    }



}