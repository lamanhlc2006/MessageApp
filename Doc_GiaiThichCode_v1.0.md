# Giải thích code – MessageApp

> Ứng dụng nhắn tin cơ bản: **ListView + SQLite + AlertDialog + Intent**
> Tài liệu này giải thích từng file, từng đoạn code quan trọng và luồng chạy của app, để bạn hiểu và trả lời được khi cô hỏi.

---

## Mục lục

1. [Tổng quan & đối chiếu đề bài](#1-tổng-quan--đối-chiếu-đề-bài)
2. [Cấu trúc thư mục](#2-cấu-trúc-thư-mục)
3. [Luồng chạy của app](#3-luồng-chạy-của-app)
4. [Model: Contact & Message](#4-model-contact--message)
5. [CSDL: DatabaseHelper (SQLite + CRUD)](#5-csdl-databasehelper-sqlite--crud)
6. [Custom ListView: Adapter + ViewHolder](#6-custom-listview-adapter--viewholder)
7. [MainActivity: danh sách cuộc trò chuyện](#7-mainactivity-danh-sách-cuộc-trò-chuyện)
8. [ChatActivity: màn hình chat](#8-chatactivity-màn-hình-chat)
9. [Custom AlertDialog](#9-custom-alertdialog)
10. [Utils: hàm tiện ích](#10-utils-hàm-tiện-ích)
11. [Layout XML & Drawable](#11-layout-xml--drawable)
12. [AndroidManifest & Theme](#12-androidmanifest--theme)
13. [Câu hỏi cô có thể hỏi & cách trả lời](#13-câu-hỏi-cô-có-thể-hỏi--cách-trả-lời)

---

## 1. Tổng quan & đối chiếu đề bài

App có **2 màn hình**:

| Màn hình | Chức năng |
|---|---|
| `MainActivity` | Danh sách các cuộc trò chuyện (liên hệ). Thêm / sửa / xóa / tìm kiếm liên hệ. |
| `ChatActivity` | Khung chat với 1 liên hệ. Gửi (thêm) / sửa / xóa tin nhắn. |

| Yêu cầu của cô | Đã làm ở đâu |
|---|---|
| Dùng **ListView** + view cơ bản | `lvContacts`, `lvMessages`; TextView, EditText, Button, ImageButton, CheckBox |
| Kết nối **CSDL** | `DatabaseHelper` kế thừa `SQLiteOpenHelper`, 2 bảng `contacts`, `messages` |
| **Thêm / sửa / xóa** dữ liệu | Có đủ cho cả liên hệ và tin nhắn |
| Dùng **AlertDialog (hoặc Intent)** | Dùng **cả hai**: AlertDialog để thêm/sửa/xóa, Intent để chuyển sang màn chat |
| **Custom 1 ListView** | `ContactAdapter`, `MessageAdapter` (kế thừa `BaseAdapter`, layout dòng tự thiết kế) |
| **Custom 1 AlertDialog** | `dialog_contact.xml`, `dialog_message.xml` gắn vào AlertDialog bằng `setView()` |

---

## 2. Cấu trúc thư mục

```
app/src/main/
├── AndroidManifest.xml                 ← khai báo 2 Activity
├── java/com/example/messageapp/
│   ├── MainActivity.java               ← màn danh sách
│   ├── ChatActivity.java               ← màn chat
│   ├── Utils.java                      ← hàm tiện ích (avatar, định dạng giờ)
│   ├── model/
│   │   ├── Contact.java                ← đối tượng liên hệ
│   │   └── Message.java                ← đối tượng tin nhắn
│   ├── data/
│   │   └── DatabaseHelper.java         ← SQLite + CRUD
│   └── adapter/
│       ├── ContactAdapter.java         ← custom adapter cho ListView liên hệ
│       └── MessageAdapter.java         ← custom adapter cho ListView tin nhắn
└── res/
    ├── layout/
    │   ├── activity_main.xml           ← giao diện màn danh sách
    │   ├── activity_chat.xml           ← giao diện màn chat
    │   ├── item_contact.xml            ← 1 dòng liên hệ (custom ListView)
    │   ├── item_message_sent.xml       ← bong bóng tin mình gửi
    │   ├── item_message_received.xml   ← bong bóng tin nhận
    │   ├── dialog_contact.xml          ← custom dialog thêm/sửa liên hệ
    │   └── dialog_message.xml          ← custom dialog sửa/xóa tin nhắn
    ├── drawable/                       ← nền bo góc, avatar tròn, icon
    └── values/                         ← colors.xml, strings.xml, themes.xml
```

Code được chia theo kiểu **tách lớp**:
- **model**: chỉ chứa dữ liệu.
- **data**: chỉ làm việc với CSDL.
- **adapter**: chỉ lo hiển thị dữ liệu lên ListView.
- **Activity**: điều khiển giao diện, gọi `data` và `adapter`.

---

## 3. Luồng chạy của app

```
Mở app
  │
  ▼
MainActivity.onCreate()
  ├─ tạo DatabaseHelper (lần đầu → onCreate() tạo bảng + dữ liệu mẫu)
  ├─ gắn ContactAdapter vào ListView
  └─ onResume() → loadData() → db.getContacts() → adapter.setData() → ListView vẽ lại
  │
  ├─ Bấm nút +           → showContactDialog(null)    → db.addContact()
  ├─ Giữ lâu 1 dòng      → showContactOptions()       → Mở / Sửa / Xóa
  │                          ├─ Sửa → showContactDialog(contact) → db.updateContact()
  │                          └─ Xóa → confirmDeleteContact()     → db.deleteContact()
  ├─ Gõ ô tìm kiếm       → loadData() lọc theo tên (LIKE)
  └─ Bấm 1 dòng          → openChat() → Intent (kèm contact_id) ──┐
                                                                 ▼
                                                ChatActivity.onCreate()
                                                  ├─ lấy contact_id từ Intent
                                                  ├─ db.getContact() → hiện tên, avatar
                                                  └─ loadMessages() → db.getMessages()
                                                  │
                                                  ├─ Bấm Gửi        → db.addMessage()
                                                  ├─ Giữ lâu tin    → showMessageDialog()
                                                  │                    ├─ Lưu → db.updateMessage()
                                                  │                    └─ Xóa → db.deleteMessage()
                                                  └─ Bấm ← (back)   → finish()
                                                                 │
  ◄──────────── quay lại MainActivity.onResume() → loadData() ◄──┘
               (tin nhắn cuối được cập nhật trên danh sách)
```

**Ý chính:** mỗi khi dữ liệu thay đổi → gọi lại hàm `loadData()` / `loadMessages()` → đọc lại từ CSDL → `adapter.setData()` → `notifyDataSetChanged()` → ListView vẽ lại.

---

## 4. Model: Contact & Message

### `Contact.java`

```java
public class Contact {
    private final long id;
    private String name;
    private String phone;
    private String lastMessage;  // không có trong bảng contacts
    private long lastTime;       // không có trong bảng contacts
    ...
}
```

- Là một **POJO** (lớp Java thuần), mỗi object tương ứng **1 dòng** trong bảng `contacts`.
- `id` để `final` vì id do CSDL sinh ra, không bao giờ đổi.
- `lastMessage`, `lastTime` **không lưu trong bảng contacts**. Chúng được lấy bằng truy vấn con (subquery) từ bảng `messages`, chỉ để hiển thị "tin nhắn cuối" trên danh sách, giống Zalo/Messenger.
- Có getter/setter để các lớp khác đọc/ghi dữ liệu (tính **đóng gói** – encapsulation).

### `Message.java`

```java
public class Message {
    private final long id;
    private final long contactId;  // khóa ngoại: tin này thuộc liên hệ nào
    private String content;        // nội dung (được sửa nên không final)
    private final boolean fromMe;  // true = mình gửi, false = người kia gửi
    private final long createdAt;  // thời gian gửi (milliseconds)
}
```

- `contactId` nối tin nhắn với liên hệ (quan hệ **1 liên hệ – nhiều tin nhắn**).
- `fromMe` quyết định tin hiển thị **bên phải** (mình) hay **bên trái** (người kia).
- `createdAt` lưu dạng `long` (số mili-giây từ 1/1/1970) vì SQLite không có kiểu ngày giờ riêng; khi hiển thị mới đổi sang chuỗi `HH:mm dd/MM/yyyy`.

---

## 5. CSDL: DatabaseHelper (SQLite + CRUD)

File: `data/DatabaseHelper.java`

### 5.1. `SQLiteOpenHelper` là gì?

Là lớp có sẵn của Android giúp quản lý CSDL SQLite:
- Tự **tạo file CSDL** khi chưa có → gọi `onCreate()`.
- Tự **nâng cấp** khi đổi số phiên bản → gọi `onUpgrade()`.
- Cấp đối tượng `SQLiteDatabase` qua `getWritableDatabase()` (đọc + ghi) và `getReadableDatabase()` (đọc).

```java
private static final String DB_NAME = "message_app.db";
private static final int DB_VERSION = 1;

public DatabaseHelper(Context context) {
    super(context, DB_NAME, null, DB_VERSION);
}
```

- `DB_NAME`: tên file CSDL, nằm ở `/data/data/com.example.messageapp/databases/message_app.db`.
- `DB_VERSION`: tăng số này lên (vd 2) thì Android gọi `onUpgrade()`.
- Tên bảng/cột được khai báo thành **hằng số** (`TB_CONTACT`, `C_NAME`…) để tránh gõ sai chính tả ở nhiều chỗ.

### 5.2. Tạo bảng – `onCreate()`

```sql
CREATE TABLE contacts (
    id    INTEGER PRIMARY KEY AUTOINCREMENT,
    name  TEXT NOT NULL,
    phone TEXT
);

CREATE TABLE messages (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    contact_id INTEGER NOT NULL,         -- liên hệ sở hữu tin nhắn
    content    TEXT NOT NULL,
    from_me    INTEGER NOT NULL DEFAULT 1, -- SQLite không có BOOLEAN → dùng 1/0
    created_at INTEGER NOT NULL           -- thời gian (ms)
);
```

- `PRIMARY KEY AUTOINCREMENT`: id tự tăng, không cần tự đặt.
- `NOT NULL`: bắt buộc có giá trị.
- `onCreate()` **chỉ chạy 1 lần duy nhất** khi file CSDL chưa tồn tại. Sau đó gọi `seedData(db)` để chèn dữ liệu mẫu (2 liên hệ, 3 tin nhắn) cho dễ demo.

> ⚠️ Nếu bạn sửa cấu trúc bảng mà app đã cài trước đó → `onCreate()` không chạy lại. Cách xử lý: **gỡ app** rồi cài lại, hoặc tăng `DB_VERSION`.

### 5.3. `onUpgrade()`

```java
db.execSQL("DROP TABLE IF EXISTS messages");
db.execSQL("DROP TABLE IF EXISTS contacts");
onCreate(db);
```

Cách đơn giản nhất: xóa bảng cũ, tạo lại bảng mới (mất dữ liệu cũ – chấp nhận được với bài tập).

### 5.4. THÊM – `insert()`

```java
public long addContact(String name, String phone) {
    return insertContact(getWritableDatabase(), name, phone);
}

private long insertContact(SQLiteDatabase db, String name, String phone) {
    ContentValues v = new ContentValues();
    v.put(C_NAME, name);
    v.put(C_PHONE, phone);
    return db.insert(TB_CONTACT, null, v);
}
```

- `ContentValues` giống một `Map`: **tên cột → giá trị**.
- `db.insert(tênBảng, null, values)` trả về **id của dòng mới**, hoặc `-1` nếu lỗi.
- Tách `insertContact(db, ...)` riêng vì trong `onCreate()` phải dùng đúng đối tượng `db` được truyền vào (không được gọi `getWritableDatabase()` trong `onCreate`, sẽ bị lỗi gọi đệ quy).

`addMessage()` tương tự, thêm `from_me` (1/0) và `created_at = System.currentTimeMillis()` (thời điểm hiện tại).

### 5.5. SỬA – `update()`

```java
public int updateContact(long id, String name, String phone) {
    ContentValues v = new ContentValues();
    v.put(C_NAME, name);
    v.put(C_PHONE, phone);
    return getWritableDatabase().update(TB_CONTACT, v, C_ID + "=?",
            new String[]{String.valueOf(id)});
}
```

- Tương đương SQL: `UPDATE contacts SET name=?, phone=? WHERE id=?`
- Dấu `?` là **tham số**, giá trị thật truyền ở mảng `String[]`. Cách này **chống SQL Injection** (người dùng gõ ký tự `'` cũng không làm hỏng câu lệnh).
- Trả về **số dòng bị sửa**.

### 5.6. XÓA – `delete()` + Transaction

```java
public void deleteContact(long id) {
    SQLiteDatabase db = getWritableDatabase();
    String[] args = {String.valueOf(id)};
    db.beginTransaction();
    try {
        db.delete(TB_MESSAGE, M_CONTACT_ID + "=?", args); // xóa tin nhắn của liên hệ
        db.delete(TB_CONTACT, C_ID + "=?", args);         // xóa liên hệ
        db.setTransactionSuccessful();
    } finally {
        db.endTransaction();
    }
}
```

- Xóa liên hệ thì phải **xóa luôn tin nhắn của liên hệ đó**, nếu không sẽ còn tin nhắn "mồ côi" trong bảng `messages`.
- **Transaction** đảm bảo 2 lệnh xóa hoặc **cùng thành công**, hoặc **cùng không xảy ra** (nếu lỗi giữa chừng thì quay lui).
  - `beginTransaction()` → bắt đầu.
  - `setTransactionSuccessful()` → đánh dấu thành công.
  - `endTransaction()` trong `finally` → nếu chưa đánh dấu thành công thì rollback.

`deleteMessage(id)` đơn giản hơn: `DELETE FROM messages WHERE id=?`.

### 5.7. ĐỌC – `query()` / `rawQuery()` và `Cursor`

**Lấy 1 liên hệ theo id:**

```java
try (Cursor c = getReadableDatabase().query(TB_CONTACT,
        new String[]{C_ID, C_NAME, C_PHONE}, C_ID + "=?",
        new String[]{String.valueOf(id)}, null, null, null)) {
    if (c.moveToFirst()) {
        return new Contact(c.getLong(0), c.getString(1), c.getString(2));
    }
}
return null;
```

- `query(bảng, cácCột, điềuKiện, thamSố, groupBy, having, orderBy)`.
- **`Cursor`** là "con trỏ" duyệt qua kết quả, giống bảng kết quả trong SQL.
  - `moveToFirst()` → đến dòng đầu (trả `false` nếu không có dòng nào).
  - `moveToNext()` → đến dòng kế tiếp (dùng trong `while`).
  - `getLong(0)`, `getString(1)` → lấy cột thứ 0, 1… theo **thứ tự cột đã chọn**.
- `try (Cursor c = ...)` là **try-with-resources**: tự động `c.close()` khi xong → không rò rỉ bộ nhớ.

**Lấy danh sách liên hệ kèm tin nhắn cuối (`getContacts`)** – câu SQL đầy đủ:

```sql
SELECT c.id, c.name, c.phone,
       (SELECT m.content FROM messages m
         WHERE m.contact_id = c.id
         ORDER BY m.created_at DESC LIMIT 1)        AS last_msg,
       (SELECT MAX(m.created_at) FROM messages m
         WHERE m.contact_id = c.id)                 AS last_time
FROM contacts c
WHERE c.name LIKE ?
ORDER BY IFNULL(last_time, 0) DESC, c.id DESC
```

Giải thích:
- 2 **truy vấn con** lấy nội dung và thời gian của tin nhắn mới nhất của mỗi liên hệ.
- `WHERE c.name LIKE ?` với tham số `"%" + keyword + "%"` → **tìm kiếm** tên có chứa từ khóa. Khi ô tìm kiếm rỗng thì thành `%%` → lấy tất cả.
- `ORDER BY ... DESC` → cuộc trò chuyện có tin mới nhất lên đầu. `IFNULL(last_time, 0)`: liên hệ chưa có tin nhắn thì coi thời gian = 0 (xếp cuối).
- `c.isNull(3)` kiểm tra cột có `NULL` không (liên hệ chưa có tin nhắn) trước khi đọc.

**Lấy tin nhắn của 1 liên hệ (`getMessages`)**: `WHERE contact_id=? ORDER BY created_at ASC` → tin cũ ở trên, tin mới ở dưới, đúng kiểu khung chat.

---

## 6. Custom ListView: Adapter + ViewHolder

### 6.1. Adapter là gì?

ListView **không tự biết** vẽ dữ liệu. Nó hỏi **Adapter** – "cầu nối" giữa **dữ liệu (List)** và **giao diện (View)**:

```
List<Contact>  ──►  ContactAdapter  ──►  ListView
 (dữ liệu)          (biến 1 Contact        (hiển thị
                     thành 1 View dòng)     các dòng)
```

- `ArrayAdapter` mặc định chỉ hiện được **1 TextView** mỗi dòng.
- Muốn mỗi dòng có avatar, tên, giờ, tin cuối… → phải **tự viết adapter** kế thừa `BaseAdapter` và **tự thiết kế layout dòng** (`item_contact.xml`). Đây chính là **custom ListView**.

### 6.2. 4 hàm bắt buộc của `BaseAdapter`

```java
public int getCount()                  { return data.size(); }        // có bao nhiêu dòng
public Contact getItem(int position)   { return data.get(position); } // dữ liệu dòng thứ position
public long getItemId(int position)    { return data.get(position).getId(); } // id của dòng
public View getView(int position, View convertView, ViewGroup parent) { ... } // tạo View cho dòng
```

### 6.3. `getView()` và tái sử dụng View (ViewHolder)

```java
public View getView(int position, View convertView, ViewGroup parent) {
    ViewHolder h;
    if (convertView == null) {
        // (1) Chưa có View để dùng lại → inflate layout mới
        convertView = LayoutInflater.from(context).inflate(R.layout.item_contact, parent, false);
        h = new ViewHolder();
        h.tvAvatar = convertView.findViewById(R.id.tvAvatar);
        h.tvName   = convertView.findViewById(R.id.tvName);
        ...
        convertView.setTag(h);       // (2) cất holder vào View
    } else {
        h = (ViewHolder) convertView.getTag(); // (3) lấy lại holder, không findViewById nữa
    }

    // (4) Đổ dữ liệu vào các view
    Contact c = getItem(position);
    h.tvName.setText(c.getName());
    ...
    return convertView;
}
```

- **`inflate()`**: biến file XML layout thành đối tượng View trong Java.
- **`convertView`**: ListView chỉ tạo đủ số dòng vừa màn hình. Khi cuộn, dòng bị cuộn khuất được **gửi lại** qua `convertView` để dùng lại → tiết kiệm bộ nhớ, cuộn mượt.
- **ViewHolder**: `findViewById` khá tốn thời gian. Ta tìm 1 lần, cất vào holder, gắn vào View bằng `setTag()`; lần sau chỉ `getTag()` là có ngay.
- Bước (4) phải **luôn gán lại mọi giá trị** (kể cả `setVisibility`) vì View có thể đang chứa dữ liệu cũ của dòng khác.

### 6.4. `setData()` + `notifyDataSetChanged()`

```java
public void setData(List<Contact> data) {
    this.data = data;
    notifyDataSetChanged(); // báo ListView: dữ liệu đổi rồi, vẽ lại đi
}
```

### 6.5. `MessageAdapter` – 2 kiểu dòng trong 1 ListView

Tin mình gửi nằm **phải**, tin nhận nằm **trái** và có avatar → 2 layout khác nhau. Phải khai báo thêm 2 hàm:

```java
public int getViewTypeCount() { return 2; }  // có 2 kiểu dòng

public int getItemViewType(int position) {   // dòng này thuộc kiểu nào (0 hoặc 1)
    return getItem(position).isFromMe() ? TYPE_SENT : TYPE_RECEIVED;
}
```

Trong `getView()`:

```java
int layout = type == TYPE_SENT ? R.layout.item_message_sent : R.layout.item_message_received;
```

Nhờ 2 hàm trên, ListView biết **chỉ đưa lại `convertView` cùng kiểu** → không bị lấy nhầm bong bóng bên phải cho tin bên trái.

`h.tvAvatar` sẽ là `null` với layout tin gửi (vì `item_message_sent.xml` không có avatar) → nên có `if (h.tvAvatar != null)`.

---

## 7. MainActivity: danh sách cuộc trò chuyện

### 7.1. `onCreate()` – khởi tạo

```java
EdgeToEdge.enable(this);
setContentView(R.layout.activity_main);
ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
    return insets;
});
```

- `EdgeToEdge`: app vẽ tràn lên cả thanh trạng thái. Đoạn `WindowInsets` thêm **padding** bằng đúng chiều cao thanh trạng thái/thanh điều hướng để nội dung không bị che. (Code này có sẵn khi tạo project.)

```java
db = new DatabaseHelper(this);
adapter = new ContactAdapter(this, new ArrayList<>());
lvContacts.setAdapter(adapter);
lvContacts.setEmptyView(tvEmpty);
```

- `setEmptyView(tvEmpty)`: khi adapter **không có dòng nào**, ListView tự ẩn và hiện `tvEmpty` ("Chưa có cuộc trò chuyện nào").

### 7.2. Các sự kiện

```java
// Bấm 1 dòng → mở chat
lvContacts.setOnItemClickListener((parent, view, position, id) -> openChat(adapter.getItem(position)));

// Giữ lâu 1 dòng → menu
lvContacts.setOnItemLongClickListener((parent, view, position, id) -> {
    showContactOptions(adapter.getItem(position));
    return true;   // true = đã xử lý, không kích hoạt thêm sự kiện click thường
});

// Nút + → thêm mới
fabAdd.setOnClickListener(v -> showContactDialog(null));
```

- Cú pháp `(a, b) -> {...}` là **lambda** (Java 8+), viết gọn thay cho `new OnClickListener() {...}`.
- `position` là vị trí dòng được bấm → `adapter.getItem(position)` ra đúng `Contact`.

**Tìm kiếm:**

```java
edtSearch.addTextChangedListener(new TextWatcher() {
    ...
    @Override public void afterTextChanged(Editable s) { loadData(); }
});
```

Mỗi lần chữ trong ô tìm kiếm thay đổi → `loadData()` → truy vấn `LIKE` lại.

### 7.3. `onResume()` – vì sao load dữ liệu ở đây?

```java
@Override
protected void onResume() {
    super.onResume();
    loadData();
}
```

**Vòng đời Activity:** `onCreate → onStart → onResume` (đang hiển thị) … khi mở ChatActivity thì MainActivity bị `onPause/onStop`; khi bấm back quay lại thì **`onResume` được gọi lại**.
→ Đặt `loadData()` ở `onResume()` để khi quay về từ màn chat, "tin nhắn cuối" trên danh sách **tự cập nhật**.

### 7.4. Mở màn chat bằng **Intent**

```java
private void openChat(Contact contact) {
    Intent intent = new Intent(this, ChatActivity.class);
    intent.putExtra(ChatActivity.EXTRA_CONTACT_ID, contact.getId());
    startActivity(intent);
}
```

- `Intent(this, ChatActivity.class)`: **Intent tường minh** (explicit) – chỉ rõ Activity muốn mở.
- `putExtra(key, value)`: gửi kèm dữ liệu. Ở đây chỉ gửi **id**; ChatActivity sẽ tự đọc chi tiết từ CSDL (gọn hơn gửi cả object).
- `startActivity()`: mở màn hình mới, MainActivity nằm dưới trong back stack.

### 7.5. AlertDialog dạng danh sách (menu giữ lâu)

```java
String[] options = {"Mở cuộc trò chuyện", "Sửa liên hệ", "Xóa liên hệ"};
new AlertDialog.Builder(this)
        .setTitle(contact.getName())
        .setItems(options, (dialog, which) -> {
            if (which == 0) openChat(contact);
            else if (which == 1) showContactDialog(contact);
            else confirmDeleteContact(contact);
        })
        .show();
```

- `setItems()`: dialog hiện danh sách lựa chọn, `which` là vị trí mục được chọn (0, 1, 2).

### 7.6. AlertDialog xác nhận xóa (dialog chuẩn)

```java
new AlertDialog.Builder(this)
        .setTitle(R.string.confirm_delete_title)
        .setMessage(getString(R.string.confirm_delete_contact, contact.getName()))
        .setNegativeButton(R.string.btn_cancel, null)        // null = chỉ đóng dialog
        .setPositiveButton(R.string.btn_delete, (d, w) -> {  // bấm Xóa
            db.deleteContact(contact.getId());
            loadData();
            toast(R.string.msg_deleted);
        })
        .show();
```

- `getString(R.string.confirm_delete_contact, tên)`: chuỗi trong `strings.xml` là `Xóa "%1$s" và toàn bộ tin nhắn?` → `%1$s` được thay bằng tên liên hệ.
- Xóa là thao tác không hoàn tác được → **luôn hỏi xác nhận**.

`showContactDialog()` (custom dialog) giải thích ở [mục 9](#9-custom-alertdialog).

---

## 8. ChatActivity: màn hình chat

### 8.1. Nhận dữ liệu từ Intent

```java
contactId = getIntent().getLongExtra(EXTRA_CONTACT_ID, -1);
Contact contact = db.getContact(contactId);
if (contact == null) {
    finish();
    return;
}
```

- `getIntent()` lấy Intent đã mở Activity này; `getLongExtra(key, giáTrịMặcĐịnh)` lấy id (nếu không có thì `-1`).
- Nếu không tìm thấy liên hệ (vd đã bị xóa) → `finish()` đóng màn hình, tránh lỗi `NullPointerException`.
- `EXTRA_CONTACT_ID` là hằng số `public static final` dùng chung ở 2 Activity → không gõ sai key.

### 8.2. Đệm bàn phím

```java
Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
        | WindowInsetsCompat.Type.ime());
```

Khác MainActivity: cộng thêm `ime()` (bàn phím ảo) → khi bàn phím bật lên, layout được đẩy lên, **ô nhập tin không bị che**.

### 8.3. ListView tin nhắn kiểu chat

Trong `activity_chat.xml`:

```xml
android:stackFromBottom="true"
android:transcriptMode="alwaysScroll"
android:divider="@null"
```

- `stackFromBottom`: danh sách xếp từ **dưới lên** (tin mới nhất sát ô nhập).
- `transcriptMode="alwaysScroll"`: có tin mới → **tự cuộn xuống cuối**.
- `divider="@null"`: bỏ đường kẻ giữa các dòng.

### 8.4. Gửi tin nhắn = THÊM vào CSDL

```java
private void sendMessage() {
    String content = edtMessage.getText().toString().trim();
    if (content.isEmpty()) return;                        // không gửi tin rỗng
    db.addMessage(contactId, content, !cbAsOther.isChecked());
    edtMessage.setText("");                               // xóa ô nhập
    loadMessages();                                       // load lại ListView
}
```

- `trim()`: bỏ khoảng trắng đầu/cuối, tránh gửi tin toàn dấu cách.
- **CheckBox "Gửi như người kia"**: vì đề không yêu cầu SMS thật nên không có người nhắn lại. Tick checkbox → `fromMe = false` → tin hiện bên trái như tin nhận được → demo đủ cả 2 kiểu bong bóng.

### 8.5. Giữ lâu tin nhắn → custom dialog sửa/xóa

```java
lvMessages.setOnItemLongClickListener((parent, view, position, id) -> {
    showMessageDialog(adapter.getItem(position));
    return true;
});
```

Chi tiết `showMessageDialog()` ở mục 9.2.

### 8.6. `onDestroy()`

```java
db.close();
```

Đóng kết nối CSDL khi Activity bị hủy → giải phóng tài nguyên.

---

## 9. Custom AlertDialog

### 9.1. Khác gì AlertDialog thường?

| AlertDialog thường | Custom AlertDialog |
|---|---|
| Dùng `setTitle`, `setMessage`, `setPositiveButton`… | Tự thiết kế **layout XML riêng** rồi gắn bằng `setView(view)` |
| Giao diện mặc định của hệ thống | Giao diện tùy ý: header màu, avatar, ô nhập, nút riêng |
| Bấm nút là **tự đóng** dialog | Nút do mình viết → **tự quyết** khi nào đóng (dùng để kiểm tra dữ liệu) |

### 9.2. Các bước tạo custom dialog (`showContactDialog`)

**Bước 1 – Inflate layout dialog:**

```java
View view = LayoutInflater.from(this).inflate(R.layout.dialog_contact, null);
EditText edtName = view.findViewById(R.id.edtName);
Button btnSave  = view.findViewById(R.id.btnSave);
...
```

Lưu ý dùng `view.findViewById` (tìm trong layout dialog), **không phải** `findViewById` của Activity.

**Bước 2 – Dùng chung 1 dialog cho cả THÊM và SỬA:**

```java
boolean isEdit = contact != null;
tvTitle.setText(isEdit ? R.string.dialog_edit_contact : R.string.dialog_add_contact);
btnSave.setText(isEdit ? R.string.btn_save : R.string.btn_add);
btnDelete.setVisibility(isEdit ? View.VISIBLE : View.GONE);
if (isEdit) {
    edtName.setText(contact.getName());   // điền sẵn dữ liệu cũ
    edtPhone.setText(contact.getPhone());
}
```

- Truyền `null` → chế độ **Thêm**; truyền 1 contact → chế độ **Sửa** (điền sẵn dữ liệu, hiện nút Xóa).
- Toán tử `điềuKiện ? A : B` (toán tử 3 ngôi) chọn A nếu đúng, B nếu sai.

**Bước 3 – Avatar xem trước đổi theo tên đang gõ:**

```java
edtName.addTextChangedListener(new TextWatcher() {
    public void onTextChanged(...)      { tilName.setError(null); }               // gõ lại thì xóa lỗi
    public void afterTextChanged(Editable s) { updateAvatarPreview(tvAvatar, s.toString()); }
});
```

**Bước 4 – Tạo dialog từ view:**

```java
AlertDialog dialog = new AlertDialog.Builder(this).setView(view).create();
if (dialog.getWindow() != null) {
    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
}
```

- `setView(view)`: **điểm mấu chốt của custom dialog**.
- `create()` (chưa `show()`) để còn giữ biến `dialog` → gọi `dialog.dismiss()` trong các nút.
- Đặt nền cửa sổ trong suốt để thấy **góc bo tròn** của `bg_dialog.xml` (nếu không sẽ bị nền vuông trắng của hệ thống che).

**Bước 5 – Nút Lưu có kiểm tra dữ liệu (validate):**

```java
btnSave.setOnClickListener(v -> {
    String name  = edtName.getText().toString().trim();
    String phone = edtPhone.getText().toString().trim();

    boolean ok = true;
    if (name.isEmpty()) {
        tilName.setError(getString(R.string.err_name_required));
        ok = false;
    }
    if (!phone.isEmpty() && !phone.matches("0\\d{9,10}")) {
        tilPhone.setError(getString(R.string.err_phone_invalid));
        ok = false;
    } else {
        tilPhone.setError(null);
    }
    if (!ok) return;            // sai → KHÔNG đóng dialog, người dùng sửa tiếp

    if (isEdit) db.updateContact(contact.getId(), name, phone);
    else        db.addContact(name, phone);
    loadData();
    dialog.dismiss();           // đúng → lưu rồi mới đóng
});
```

- Tên **bắt buộc**; số điện thoại **không bắt buộc**, nhưng nếu nhập thì phải đúng định dạng.
- Regex `0\d{9,10}`: bắt đầu bằng `0`, theo sau là 9–10 chữ số (tổng 10–11 số). Trong Java phải viết `\\d` vì `\` trong chuỗi cần escape.
- `TextInputLayout.setError()`: hiện dòng chữ đỏ ngay dưới ô nhập (đẹp hơn Toast).
- Đây là lý do dùng **nút tự viết** thay vì `setPositiveButton`: nút mặc định luôn tự đóng dialog dù dữ liệu sai.

**Bước 6 – Nút Hủy / Xóa:**

```java
btnCancel.setOnClickListener(v -> dialog.dismiss());
btnDelete.setOnClickListener(v -> {
    dialog.dismiss();
    confirmDeleteContact(contact);   // mở tiếp dialog xác nhận
});
```

**Bước 7 – `dialog.show()`.**

### 9.3. `showMessageDialog()` (ChatActivity)

Cùng khuôn với trên, dùng `dialog_message.xml`:
- Hiện thời gian gửi: `"Bạn gửi lúc ..."` hoặc `"Nhận lúc ..."`.
- `edtContent.setSelection(length)`: đặt con trỏ ở **cuối** nội dung để sửa tiếp cho tiện.
- **Lưu**: rỗng → báo lỗi, không đóng; hợp lệ → `db.updateMessage()`.
- **Xóa**: đóng dialog sửa → mở AlertDialog xác nhận → `db.deleteMessage()`.

### 9.4. Tổng hợp các loại AlertDialog trong app

| Dialog | Loại | Mục đích |
|---|---|---|
| `dialog_contact.xml` | **Custom** (`setView`) | Thêm / sửa liên hệ |
| `dialog_message.xml` | **Custom** (`setView`) | Sửa / xóa tin nhắn |
| Menu giữ lâu liên hệ | Chuẩn (`setItems`) | Chọn Mở / Sửa / Xóa |
| Xác nhận xóa | Chuẩn (`setMessage` + 2 nút) | Hỏi trước khi xóa |

---

## 10. Utils: hàm tiện ích

File `Utils.java` – lớp `final`, constructor `private` → không tạo object, chỉ gọi hàm `static` như `Utils.initial(...)`.

### `initial(name)` – chữ cái avatar

```java
String[] parts = name.trim().split("\\s+");
return parts[parts.length - 1].substring(0, 1).toUpperCase(...);
```

- `split("\\s+")`: tách tên theo khoảng trắng.
- Lấy chữ đầu của **từ cuối** vì tên Việt Nam có tên gọi ở cuối: "Nguyễn Văn **A**n" → `A`.
- Tên rỗng → trả `"?"`.

### `tintAvatar(view, name)` – màu avatar theo tên

```java
int color = AVATAR_COLORS[Math.abs(name.hashCode()) % AVATAR_COLORS.length];
GradientDrawable bg = (GradientDrawable) view.getBackground().mutate();
bg.setColor(color);
```

- `hashCode()` của cùng 1 chuỗi luôn ra cùng số → **cùng tên luôn cùng màu**.
- `% length` (chia lấy dư) → chọn 1 trong 8 màu.
- `mutate()`: **rất quan trọng** – các View dùng chung 1 drawable `bg_avatar`; nếu không `mutate()` thì đổi màu 1 avatar sẽ làm đổi màu **tất cả** avatar. `mutate()` tạo bản riêng cho View đó.

### `shortTime(millis)` / `fullTime(millis)`

- `shortTime`: nếu cùng ngày hôm nay → `HH:mm` (vd `16:24`), khác ngày → `dd/MM` (vd `10/11`). Dùng trên danh sách liên hệ.
- `fullTime`: `HH:mm dd/MM/yyyy`. Dùng dưới mỗi tin nhắn.
- `SimpleDateFormat` đổi số mili-giây thành chuỗi ngày giờ theo mẫu.

---

## 11. Layout XML & Drawable

### Layout

| File | Thành phần chính |
|---|---|
| `activity_main.xml` | `FrameLayout` gốc → header (tiêu đề + ô tìm kiếm `EditText`) + `ListView` + `TextView` rỗng; `FloatingActionButton` nổi ở góc dưới phải (`layout_gravity="bottom|end"`). |
| `activity_chat.xml` | `LinearLayout` dọc: thanh tiêu đề (`ImageButton` back, avatar, tên, SĐT) → `ListView` (`layout_weight="1"` chiếm hết phần còn lại) → khung nhập (`CheckBox`, `EditText`, `ImageButton` gửi). |
| `item_contact.xml` | Dòng ngang: avatar tròn + cột (tên & giờ / tin cuối / SĐT). |
| `item_message_sent.xml` | `gravity="end"` → bong bóng nằm **phải**; `paddingStart="64dp"` để bong bóng không chiếm hết chiều ngang. |
| `item_message_received.xml` | Avatar nhỏ + bong bóng nằm **trái**; `paddingEnd="64dp"`. |
| `dialog_contact.xml` | Header gradient + avatar xem trước; 2 `TextInputLayout`; hàng nút Xóa / Hủy / Lưu. |
| `dialog_message.xml` | Header; dòng thời gian; ô sửa nội dung nhiều dòng; hàng nút. |

Một số thuộc tính hay gặp:
- `layout_width="0dp"` + `layout_weight="1"`: chiếm **toàn bộ phần còn trống** trong LinearLayout.
- `ellipsize="end"` + `maxLines="1"`: chữ dài quá thì hiện `...`.
- `<View layout_weight="1"/>` giữa các nút: "lò xo" đẩy nút Xóa sang trái, Hủy/Lưu sang phải.
- `inputType="phone"`: bàn phím số; `textCapWords`: tự viết hoa chữ đầu mỗi từ.
- `maxLength="50"`: giới hạn số ký tự nhập.

### Drawable (`res/drawable`)

| File | Tác dụng |
|---|---|
| `bg_avatar.xml` | `<shape android:shape="oval">` – hình tròn làm nền avatar. |
| `bg_bubble_sent.xml` / `bg_bubble_received.xml` | Hình chữ nhật bo góc 16dp, **riêng 1 góc 4dp** (góc phía người gửi) tạo hình bong bóng chat. |
| `bg_input.xml` | Nền xám bo tròn cho ô tìm kiếm / ô nhập tin. |
| `bg_dialog.xml` | Nền trắng bo 20dp cho dialog. |
| `bg_dialog_header.xml` | `<gradient>` chuyển màu xanh, chỉ bo 2 góc trên. |
| `ic_back.xml`, `ic_send.xml` | Icon vector (vẽ bằng `pathData`), không cần file ảnh PNG. |

### Values

- `colors.xml`: khai báo màu (`primary` xanh dương, màu bong bóng, màu chữ…). Đổi màu cả app chỉ cần sửa ở đây.
- `strings.xml`: mọi chữ hiển thị. Không viết cứng chữ trong code/XML → dễ sửa, dễ dịch đa ngôn ngữ.

---

## 12. AndroidManifest & Theme

```xml
<activity android:name=".MainActivity" android:exported="true" ...>
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
<activity
    android:name=".ChatActivity"
    android:exported="false"
    android:windowSoftInputMode="adjustResize" />
```

- **Mọi Activity phải khai báo trong Manifest**, nếu không `startActivity` sẽ crash (`ActivityNotFoundException`).
- `MAIN` + `LAUNCHER`: MainActivity là màn hình mở đầu tiên khi bấm icon app.
- `exported="false"`: ChatActivity chỉ được mở từ trong app, app khác không mở được.
- `adjustResize`: khi bàn phím hiện, co layout lại thay vì che.

**Theme** (`values/themes.xml` và `values-night/themes.xml`): dùng `Theme.Material3.Light.NoActionBar`
- `NoActionBar`: tự làm thanh tiêu đề trong layout.
- `Light` ở **cả 2 file** → app luôn giao diện sáng, kể cả khi điện thoại bật Dark mode (tránh chữ đen trên nền đen).

---

## 13. Câu hỏi cô có thể hỏi & cách trả lời

**Hỏi: Custom ListView của em ở đâu? Khác ListView thường thế nào?**
> `ContactAdapter` và `MessageAdapter` kế thừa `BaseAdapter`, mỗi dòng dùng layout tự thiết kế (`item_contact.xml`) có avatar, tên, giờ, tin cuối. ListView thường dùng `ArrayAdapter` chỉ hiện 1 TextView. `MessageAdapter` còn dùng 2 kiểu dòng nhờ `getViewTypeCount()` và `getItemViewType()`.

**Hỏi: ViewHolder / convertView để làm gì?**
> Để tái sử dụng View khi cuộn, không phải inflate và findViewById lại mỗi dòng → tiết kiệm bộ nhớ, cuộn mượt.

**Hỏi: Custom AlertDialog làm thế nào?**
> Thiết kế layout `dialog_contact.xml`, inflate bằng `LayoutInflater`, gắn vào `AlertDialog.Builder().setView(view)`. Các nút là Button trong layout nên em tự kiểm soát việc đóng dialog — nhập sai thì báo lỗi và giữ dialog lại.

**Hỏi: Dữ liệu lưu ở đâu? CSDL có mấy bảng?**
> SQLite, file `message_app.db`, quản lý bằng `DatabaseHelper extends SQLiteOpenHelper`. 2 bảng: `contacts` và `messages`, liên kết qua `messages.contact_id`.

**Hỏi: `onCreate` của DatabaseHelper chạy khi nào?**
> Chỉ 1 lần khi file CSDL chưa tồn tại. Đổi cấu trúc bảng thì phải tăng `DB_VERSION` (chạy `onUpgrade`) hoặc gỡ app cài lại.

**Hỏi: Vì sao dùng `?` trong câu lệnh SQL?**
> Truyền tham số an toàn, chống SQL Injection và không lỗi khi dữ liệu có dấu nháy `'`.

**Hỏi: Xóa liên hệ thì tin nhắn của liên hệ đó thế nào?**
> Xóa luôn trong cùng 1 transaction: xóa tin nhắn trước, rồi xóa liên hệ. Nếu lỗi giữa chừng thì rollback cả hai.

**Hỏi: Em truyền dữ liệu giữa 2 màn hình thế nào?**
> Dùng Intent tường minh, `putExtra("contact_id", id)`; bên ChatActivity lấy bằng `getIntent().getLongExtra(...)` rồi đọc chi tiết từ CSDL.

**Hỏi: Sao quay lại màn danh sách thì tin nhắn cuối tự cập nhật?**
> Vì em gọi `loadData()` trong `onResume()`, hàm này chạy lại mỗi khi Activity hiển thị trở lại.

**Hỏi: Sao danh sách tự cập nhật sau khi thêm/sửa/xóa?**
> Sau mỗi thao tác CSDL em gọi `loadData()` → đọc lại dữ liệu → `adapter.setData()` → `notifyDataSetChanged()` báo ListView vẽ lại.

**Hỏi: Không có SMS thì làm sao có tin nhắn đến?**
> Đề không yêu cầu SMS. Em thêm CheckBox "Gửi như người kia" để giả lập tin nhận (`from_me = 0`), hiện bên trái.

**Hỏi: `return true` trong `setOnItemLongClickListener` nghĩa là gì?**
> Báo sự kiện giữ lâu đã được xử lý, để không kích hoạt thêm sự kiện click thường.

**Hỏi: Tìm kiếm hoạt động thế nào?**
> `TextWatcher` theo dõi ô tìm kiếm, mỗi lần chữ thay đổi gọi `loadData()`, truy vấn `WHERE name LIKE '%từkhóa%'`.

---

*Cập nhật: 11/11/2026*
