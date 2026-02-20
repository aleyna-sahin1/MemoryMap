package com.aleynasahin.memorymap.model;

public class Memory {

    public String firestoreId; // Firestore'dan dönen ID
    public String memoryTitle;
    public String memoryNote;
    public Double memoryLatitude;
    public Double memoryLongitude;
    public String photoUri;

    public Memory() {}

    public Memory(String memoryTitle, String memoryNote, Double memoryLatitude, Double memoryLongitude, String photoUri) {
        this.memoryTitle = memoryTitle;
        this.memoryNote = memoryNote;
        this.memoryLatitude = memoryLatitude;
        this.memoryLongitude = memoryLongitude;
        this.photoUri = photoUri;
    }
}