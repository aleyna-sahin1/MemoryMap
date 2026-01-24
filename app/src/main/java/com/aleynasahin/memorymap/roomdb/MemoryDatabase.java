package com.aleynasahin.memorymap.roomdb;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.aleynasahin.memorymap.model.Memory;

@Database(entities = {Memory.class}, version = 2)
public abstract class MemoryDatabase extends RoomDatabase {
    public abstract MemoryDao memoryDao();
}
