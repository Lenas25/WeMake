package com.utp.wemake.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.utp.wemake.database.dao.SubtaskDao;
import com.utp.wemake.database.dao.TaskDao;
import com.utp.wemake.database.entities.SubtaskEntity;
import com.utp.wemake.database.entities.TaskEntity;

@Database(
        entities = {TaskEntity.class, SubtaskEntity.class},
        version = 2,
        exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract TaskDao taskDao();
    public abstract SubtaskDao subtaskDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "wemake_database"
                            )
                            .fallbackToDestructiveMigration() // En desarrollo, elimina en producción
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}