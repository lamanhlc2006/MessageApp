package com.example.messageapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.messageapp.adapter.ContactAdapter;
import com.example.messageapp.data.DatabaseHelper;
import com.example.messageapp.model.Contact;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;

/**
 * Màn hình danh sách cuộc trò chuyện.
 *  - Bấm 1 dòng      -> mở ChatActivity bằng Intent
 *  - Giữ lâu 1 dòng  -> AlertDialog chọn Mở / Sửa / Xóa
 *  - Nút +           -> Custom AlertDialog thêm liên hệ
 */
public class MainActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private ContactAdapter adapter;
    private EditText edtSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        db = new DatabaseHelper(this);

        ListView lvContacts = findViewById(R.id.lvContacts);
        TextView tvEmpty = findViewById(R.id.tvEmpty);
        edtSearch = findViewById(R.id.edtSearch);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);

        adapter = new ContactAdapter(this, new ArrayList<>());
        lvContacts.setAdapter(adapter);
        lvContacts.setEmptyView(tvEmpty);

        // Click: mở màn chat bằng Intent
        lvContacts.setOnItemClickListener((parent, view, position, id) -> openChat(adapter.getItem(position)));

        // Giữ lâu: menu thao tác
        lvContacts.setOnItemLongClickListener((parent, view, position, id) -> {
            showContactOptions(adapter.getItem(position));
            return true;
        });

        fabAdd.setOnClickListener(v -> showContactDialog(null));

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { loadData(); }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Quay lại từ màn chat thì cập nhật tin nhắn cuối
        loadData();
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }

    private void loadData() {
        adapter.setData(db.getContacts(edtSearch.getText().toString()));
    }

    private void openChat(Contact contact) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_CONTACT_ID, contact.getId());
        startActivity(intent);
    }

    /** AlertDialog dạng danh sách lựa chọn. */
    private void showContactOptions(Contact contact) {
        String[] options = {"Mở cuộc trò chuyện", "Sửa liên hệ", "Xóa liên hệ"};
        new AlertDialog.Builder(this)
                .setTitle(contact.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) openChat(contact);
                    else if (which == 1) showContactDialog(contact);
                    else confirmDeleteContact(contact);
                })
                .show();
    }

    /**
     * CUSTOM AlertDialog dùng layout dialog_contact.xml.
     * @param contact null = thêm mới, khác null = sửa
     */
    private void showContactDialog(Contact contact) {
        boolean isEdit = contact != null;
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_contact, null);

        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        TextView tvAvatar = view.findViewById(R.id.tvAvatarPreview);
        TextInputLayout tilName = view.findViewById(R.id.tilName);
        TextInputLayout tilPhone = view.findViewById(R.id.tilPhone);
        EditText edtName = view.findViewById(R.id.edtName);
        EditText edtPhone = view.findViewById(R.id.edtPhone);
        Button btnDelete = view.findViewById(R.id.btnDelete);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnSave = view.findViewById(R.id.btnSave);

        tvTitle.setText(isEdit ? R.string.dialog_edit_contact : R.string.dialog_add_contact);
        btnSave.setText(isEdit ? R.string.btn_save : R.string.btn_add);
        btnDelete.setVisibility(isEdit ? View.VISIBLE : View.GONE);
        if (isEdit) {
            edtName.setText(contact.getName());
            edtPhone.setText(contact.getPhone());
        }
        updateAvatarPreview(tvAvatar, edtName.getText().toString());

        // Avatar xem trước thay đổi theo tên đang gõ
        edtName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilName.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {
                updateAvatarPreview(tvAvatar, s.toString());
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this).setView(view).create();
        // Nền trong suốt để thấy được góc bo của layout custom
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            confirmDeleteContact(contact);
        });

        btnSave.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            String phone = edtPhone.getText().toString().trim();

            // Kiểm tra dữ liệu, lỗi thì không đóng dialog
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
            if (!ok) return;

            if (isEdit) {
                db.updateContact(contact.getId(), name, phone);
                toast(R.string.msg_updated);
            } else {
                db.addContact(name, phone);
                toast(R.string.msg_added);
            }
            loadData();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateAvatarPreview(TextView tvAvatar, String name) {
        tvAvatar.setText(Utils.initial(name));
        Utils.tintAvatar(tvAvatar, name);
    }

    private void confirmDeleteContact(Contact contact) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete_title)
                .setMessage(getString(R.string.confirm_delete_contact, contact.getName()))
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_delete, (d, w) -> {
                    db.deleteContact(contact.getId());
                    loadData();
                    toast(R.string.msg_deleted);
                })
                .show();
    }

    private void toast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }
}
