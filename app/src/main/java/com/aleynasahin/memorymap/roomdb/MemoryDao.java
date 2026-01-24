package com.aleynasahin.memorymap.roomdb;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.aleynasahin.memorymap.model.Memory;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface MemoryDao {

    @Query("SELECT * FROM Memory")
    Flowable<List<Memory>> getAllMemories();

    @Insert
    Completable insert(Memory memory);

    @Delete
    Completable delete(Memory memory);

    @Query("SELECT * FROM Memory WHERE memoryTitle = :title AND memoryNote = :note LIMIT 1")
    Single<Memory> getMemoryByTitleAndNote(String title, String note);
    @Query(
            "SELECT * FROM Memory " +
                    "WHERE memoryTitle LIKE '%' || :query || '%' " +
                    "OR memoryNote LIKE '%' || :query || '%'"
    )
    Flowable<List<Memory>> searchMemories(String query);

}
