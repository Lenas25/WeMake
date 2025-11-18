package com.utp.wemake.db;
import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.utp.wemake.models.Subtask;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Converters {
    private static final Gson gson = new Gson();

    // De un Timestamp (número largo) a un objeto Date
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    // De un objeto Date a un Timestamp para guardarlo
    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    // De un texto JSON a una List<String>
    @TypeConverter
    public static List<String> fromStringList(String value) {
        if (value == null) return Collections.emptyList();
        Type listType = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(value, listType);
    }

    // De una List<String> a un texto JSON
    @TypeConverter
    public static String fromListString(List<String> list) {
        return gson.toJson(list);
    }

    // De un texto JSON a una List<Subtask>
    @TypeConverter
    public static List<Subtask> fromSubtaskList(String value) {
        if (value == null) return Collections.emptyList();
        Type listType = new TypeToken<List<Subtask>>() {}.getType();
        return gson.fromJson(value, listType);
    }

    // De una List<Subtask> a un texto JSON
    @TypeConverter
    public static String fromListSubtask(List<Subtask> list) {
        return gson.toJson(list);
    }
}