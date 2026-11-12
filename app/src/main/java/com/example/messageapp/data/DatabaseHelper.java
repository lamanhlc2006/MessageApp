package com.example.messageapp.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.messageapp.model.Contact;
import com.example.messageapp.model.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý CSDL SQLite gồm 2 bảng:
 *  - contacts(id, name, phone)
 *  - messages(id, contact_id, content, from_me, created_at)
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "message_app.db";
    private static final int DB_VERSION = 1;

    // Bảng contacts
    private static final String TB_CONTACT = "contacts";
    private static final String C_ID = "id";
    private static final String C_NAME = "name";
    private static final String C_PHONE = "phone";

    // Bảng messages
    private static final String TB_MESSAGE = "messages";
    private static final String M_ID = "id";
    private static final String M_CONTACT_ID = "contact_id";
    private static final String M_CONTENT = "content";
    private static final String M_FROM_ME = "from_me";
    private static final String M_CREATED_AT = "created_at";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TB_CONTACT + " ("
                + C_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + C_NAME + " TEXT NOT NULL, "
                + C_PHONE + " TEXT)");

        db.execSQL("CREATE TABLE " + TB_MESSAGE + " ("
                + M_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + M_CONTACT_ID + " INTEGER NOT NULL, "
                + M_CONTENT + " TEXT NOT NULL, "
                + M_FROM_ME + " INTEGER NOT NULL DEFAULT 1, "
                + M_CREATED_AT + " INTEGER NOT NULL)");

        seedData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TB_MESSAGE);
        db.execSQL("DROP TABLE IF EXISTS " + TB_CONTACT);
        onCreate(db);
    }

    /** Dữ liệu mẫu để lần đầu mở app đã có nội dung demo. */
    private void seedData(SQLiteDatabase db) {
        long now = System.currentTimeMillis();
        long an = insertContact(db, "Nguyễn Văn An", "0901234567");
        long binh = insertContact(db, "Trần Thị Bình", "0912345678");
        insertMessage(db, an, "Chào An, mai đi học không?", true, now - 3_600_000);
        insertMessage(db, an, "Có nha, 7h gặp ở cổng trường", false, now - 3_500_000);
        insertMessage(db, binh, "Bình ơi gửi mình bài tập Android với", true, now - 600_000);
    }

    // ===================== CONTACT: THÊM / SỬA / XÓA / ĐỌC =====================

    private long insertContact(SQLiteDatabase db, String name, String phone) {
        ContentValues v = new ContentValues();
        v.put(C_NAME, name);
        v.put(C_PHONE, phone);
        return db.insert(TB_CONTACT, null, v);
    }

    public long addContact(String name, String phone) {
        return insertContact(getWritableDatabase(), name, phone);
    }

    public int updateContact(long id, String name, String phone) {
        ContentValues v = new ContentValues();
        v.put(C_NAME, name);
        v.put(C_PHONE, phone);
        return getWritableDatabase().update(TB_CONTACT, v, C_ID + "=?",
                new String[]{String.valueOf(id)});
    }

    /** Xóa liên hệ và toàn bộ tin nhắn của liên hệ đó. */
    public void deleteContact(long id) {
        SQLiteDatabase db = getWritableDatabase();
        String[] args = {String.valueOf(id)};
        db.beginTransaction();
        try {
            db.delete(TB_MESSAGE, M_CONTACT_ID + "=?", args);
            db.delete(TB_CONTACT, C_ID + "=?", args);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public Contact getContact(long id) {
        try (Cursor c = getReadableDatabase().query(TB_CONTACT,
                new String[]{C_ID, C_NAME, C_PHONE}, C_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            if (c.moveToFirst()) {
                return new Contact(c.getLong(0), c.getString(1), c.getString(2));
            }
        }
        return null;
    }

    /**
     * Lấy danh sách liên hệ kèm tin nhắn cuối, sắp xếp mới nhất lên đầu.
     * @param keyword tìm theo tên (null / rỗng = lấy tất cả)
     */
    public List<Contact> getContacts(String keyword) {
        List<Contact> list = new ArrayList<>();
        String sql = "SELECT c." + C_ID + ", c." + C_NAME + ", c." + C_PHONE + ", "
                + "(SELECT m." + M_CONTENT + " FROM " + TB_MESSAGE + " m WHERE m." + M_CONTACT_ID
                + " = c." + C_ID + " ORDER BY m." + M_CREATED_AT + " DESC LIMIT 1) AS last_msg, "
                + "(SELECT MAX(m." + M_CREATED_AT + ") FROM " + TB_MESSAGE + " m WHERE m."
                + M_CONTACT_ID + " = c." + C_ID + ") AS last_time "
                + "FROM " + TB_CONTACT + " c "
                + "WHERE c." + C_NAME + " LIKE ? "
                + "ORDER BY IFNULL(last_time, 0) DESC, c." + C_ID + " DESC";
        String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
        try (Cursor c = getReadableDatabase().rawQuery(sql, new String[]{like})) {
            while (c.moveToNext()) {
                Contact contact = new Contact(c.getLong(0), c.getString(1), c.getString(2));
                contact.setLastMessage(c.isNull(3) ? null : c.getString(3));
                contact.setLastTime(c.isNull(4) ? 0 : c.getLong(4));
                list.add(contact);
            }
        }
        return list;
    }

    // ===================== MESSAGE: THÊM / SỬA / XÓA / ĐỌC =====================

    private long insertMessage(SQLiteDatabase db, long contactId, String content,
                               boolean fromMe, long time) {
        ContentValues v = new ContentValues();
        v.put(M_CONTACT_ID, contactId);
        v.put(M_CONTENT, content);
        v.put(M_FROM_ME, fromMe ? 1 : 0);
        v.put(M_CREATED_AT, time);
        return db.insert(TB_MESSAGE, null, v);
    }

    public long addMessage(long contactId, String content, boolean fromMe) {
        return insertMessage(getWritableDatabase(), contactId, content, fromMe,
                System.currentTimeMillis());
    }

    public int updateMessage(long id, String content) {
        ContentValues v = new ContentValues();
        v.put(M_CONTENT, content);
        return getWritableDatabase().update(TB_MESSAGE, v, M_ID + "=?",
                new String[]{String.valueOf(id)});
    }

    public int deleteMessage(long id) {
        return getWritableDatabase().delete(TB_MESSAGE, M_ID + "=?",
                new String[]{String.valueOf(id)});
    }

    public List<Message> getMessages(long contactId) {
        List<Message> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query(TB_MESSAGE,
                new String[]{M_ID, M_CONTACT_ID, M_CONTENT, M_FROM_ME, M_CREATED_AT},
                M_CONTACT_ID + "=?", new String[]{String.valueOf(contactId)},
                null, null, M_CREATED_AT + " ASC, " + M_ID + " ASC")) {
            while (c.moveToNext()) {
                list.add(new Message(c.getLong(0), c.getLong(1), c.getString(2),
                        c.getInt(3) == 1, c.getLong(4)));
            }
        }
        return list;
    }
}
