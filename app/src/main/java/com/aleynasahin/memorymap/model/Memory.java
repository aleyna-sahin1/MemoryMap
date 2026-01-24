package com.aleynasahin.memorymap.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Memory {

    @PrimaryKey(autoGenerate = true)
    public int memoryId;
    @ColumnInfo(name = "memoryTitle")
    public String memoryTitle;
    @ColumnInfo(name = "memoryNote")
    public String memoryNote;
    @ColumnInfo(name = "memoryLatitude")
    public Double memoryLatitude;
    @ColumnInfo(name = "memoryLongitude")
    public Double memoryLongitude;

    @ColumnInfo(name = "photoUri")
    public String photoUri;


    public Memory(String memoryTitle, String memoryNote, Double memoryLatitude, Double memoryLongitude, String photoUri) {
        this.memoryTitle = memoryTitle;
        this.memoryNote = memoryNote;
        this.memoryLatitude = memoryLatitude;
        this.memoryLongitude = memoryLongitude;
        this.photoUri = photoUri;

    }
}