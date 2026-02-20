package com.aleynasahin.memorymap.Firestore;

import android.net.Uri;
import android.util.Log;

import com.aleynasahin.memorymap.model.Memory;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FirestoreRepository {

    private static final String TAG = "FirestoreRepository";

    private final CollectionReference memoriesRef;
    private final FirebaseStorage storage;

    public FirestoreRepository() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        memoriesRef = firestore.collection("users").document(uid).collection("memories");
        storage = FirebaseStorage.getInstance();
    }

    public void getAllMemories(OnSuccessListener<List<Memory>> onSuccess,
                               OnFailureListener onFailure) {
        memoriesRef.get()
                .addOnSuccessListener(snapshot -> {
                    List<Memory> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        list.add(documentToMemory(doc));
                    }
                    onSuccess.onSuccess(list);
                })
                .addOnFailureListener(onFailure);
    }


    public void searchMemories(String query,
                               OnSuccessListener<List<Memory>> onSuccess,
                               OnFailureListener onFailure) {
        String queryLower = query.toLowerCase();
        memoriesRef.get()
                .addOnSuccessListener(snapshot -> {
                    List<Memory> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String title = doc.getString("memoryTitle");
                        if (title != null && title.toLowerCase().contains(queryLower)) {
                            list.add(documentToMemory(doc));
                        }
                    }
                    onSuccess.onSuccess(list);
                })
                .addOnFailureListener(onFailure);
    }


    public void getMemoryByTitleAndNote(String title, String note,
                                        OnSuccessListener<Memory> onSuccess,
                                        OnFailureListener onFailure) {
        memoriesRef
                .whereEqualTo("memoryTitle", title)
                .whereEqualTo("memoryNote", note)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        onSuccess.onSuccess(documentToMemory(snapshot.getDocuments().get(0)));
                    } else {
                        onFailure.onFailure(new Exception("Anı bulunamadı"));
                    }
                })
                .addOnFailureListener(onFailure);
    }


    public void saveMemory(Memory memory,
                           OnSuccessListener<Void> onSuccess,
                           OnFailureListener onFailure) {
        memoriesRef.add(memoryToMap(memory))
                .addOnSuccessListener(ref -> onSuccess.onSuccess(null))
                .addOnFailureListener(onFailure);
    }


    public void saveMemoryWithPhoto(Memory memory,
                                    Uri photoUri,
                                    OnSuccessListener<Void> onSuccess,
                                    OnFailureListener onFailure) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String fileName = "memories/" + uid + "/" + UUID.randomUUID().toString();
        StorageReference ref = storage.getReference(fileName);

        ref.putFile(photoUri)
                .addOnSuccessListener(task ->
                        ref.getDownloadUrl().addOnSuccessListener(downloadUrl -> {
                            memory.photoUri = downloadUrl.toString();
                            saveMemory(memory, onSuccess, onFailure);
                        })
                )
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Fotoğraf yüklenemedi, fotoğrafsız kaydediliyor", e);

                    memory.photoUri = "";
                    saveMemory(memory, onSuccess, onFailure);
                });
    }


    public void deleteMemory(String firestoreId,
                             OnSuccessListener<Void> onSuccess,
                             OnFailureListener onFailure) {
        memoriesRef.document(firestoreId)
                .delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }



    private Memory documentToMemory(DocumentSnapshot doc) {
        Memory memory = new Memory(
                doc.getString("memoryTitle"),
                doc.getString("memoryNote"),
                doc.getDouble("memoryLatitude"),
                doc.getDouble("memoryLongitude"),
                doc.getString("photoUri")
        );
        memory.firestoreId = doc.getId();
        return memory;
    }

    private Map<String, Object> memoryToMap(Memory memory) {
        Map<String, Object> data = new HashMap<>();
        data.put("memoryTitle", memory.memoryTitle);
        data.put("memoryNote", memory.memoryNote);
        data.put("memoryLatitude", memory.memoryLatitude);
        data.put("memoryLongitude", memory.memoryLongitude);
        data.put("photoUri", memory.photoUri != null ? memory.photoUri : "");
        return data;
    }
}