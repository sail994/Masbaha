package com.example.masbaha.data;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class PhraseDao_Impl implements PhraseDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<DhikrPhrase> __insertionAdapterOfDhikrPhrase;

  private final EntityInsertionAdapter<DhikrPhrase> __insertionAdapterOfDhikrPhrase_1;

  private final EntityDeletionOrUpdateAdapter<DhikrPhrase> __deletionAdapterOfDhikrPhrase;

  private final EntityDeletionOrUpdateAdapter<DhikrPhrase> __updateAdapterOfDhikrPhrase;

  private final SharedSQLiteStatement __preparedStmtOfIncrementCount;

  public PhraseDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDhikrPhrase = new EntityInsertionAdapter<DhikrPhrase>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `dhikr_phrases` (`id`,`text`,`count`,`targetCount`) VALUES (nullif(?, 0),?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DhikrPhrase entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getText());
        statement.bindLong(3, entity.getCount());
        statement.bindLong(4, entity.getTargetCount());
      }
    };
    this.__insertionAdapterOfDhikrPhrase_1 = new EntityInsertionAdapter<DhikrPhrase>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `dhikr_phrases` (`id`,`text`,`count`,`targetCount`) VALUES (nullif(?, 0),?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DhikrPhrase entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getText());
        statement.bindLong(3, entity.getCount());
        statement.bindLong(4, entity.getTargetCount());
      }
    };
    this.__deletionAdapterOfDhikrPhrase = new EntityDeletionOrUpdateAdapter<DhikrPhrase>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `dhikr_phrases` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DhikrPhrase entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfDhikrPhrase = new EntityDeletionOrUpdateAdapter<DhikrPhrase>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `dhikr_phrases` SET `id` = ?,`text` = ?,`count` = ?,`targetCount` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DhikrPhrase entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getText());
        statement.bindLong(3, entity.getCount());
        statement.bindLong(4, entity.getTargetCount());
        statement.bindLong(5, entity.getId());
      }
    };
    this.__preparedStmtOfIncrementCount = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE dhikr_phrases SET count = count + 1 WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public void insertPhrase(final DhikrPhrase phrase) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfDhikrPhrase.insert(phrase);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insertAll(final List<DhikrPhrase> phrases) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfDhikrPhrase_1.insert(phrases);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deletePhrase(final DhikrPhrase phrase) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfDhikrPhrase.handle(phrase);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void updatePhrase(final DhikrPhrase phrase) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfDhikrPhrase.handle(phrase);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void incrementCount(final int phraseId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfIncrementCount.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, phraseId);
    try {
      __db.beginTransaction();
      try {
        _stmt.executeUpdateDelete();
        __db.setTransactionSuccessful();
      } finally {
        __db.endTransaction();
      }
    } finally {
      __preparedStmtOfIncrementCount.release(_stmt);
    }
  }

  @Override
  public Flow<List<DhikrPhrase>> getAllPhrases() {
    final String _sql = "SELECT * FROM dhikr_phrases";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"dhikr_phrases"}, new Callable<List<DhikrPhrase>>() {
      @Override
      @NonNull
      public List<DhikrPhrase> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfText = CursorUtil.getColumnIndexOrThrow(_cursor, "text");
          final int _cursorIndexOfCount = CursorUtil.getColumnIndexOrThrow(_cursor, "count");
          final int _cursorIndexOfTargetCount = CursorUtil.getColumnIndexOrThrow(_cursor, "targetCount");
          final List<DhikrPhrase> _result = new ArrayList<DhikrPhrase>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DhikrPhrase _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpText;
            _tmpText = _cursor.getString(_cursorIndexOfText);
            final int _tmpCount;
            _tmpCount = _cursor.getInt(_cursorIndexOfCount);
            final int _tmpTargetCount;
            _tmpTargetCount = _cursor.getInt(_cursorIndexOfTargetCount);
            _item = new DhikrPhrase(_tmpId,_tmpText,_tmpCount,_tmpTargetCount);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
