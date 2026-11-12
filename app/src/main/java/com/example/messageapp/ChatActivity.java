package com.example.messageapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.messageapp.adapter.MessageAdapter;
import com.example.messageapp.data.DatabaseHelper;
import com.example.messageapp.model.Contact;
import com.example.messageapp.model.Message;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;

/**
 * Màn hình chat, được mở từ MainActivity bằng Intent (truyền contact id).
 *  - Gửi tin          -> THÊM vào bảng messages
 *  - Giữ lâu tin nhắn -> Custom AlertDialog SỬA / XÓA
 */
public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_CONTACT_ID = "contact_id";

    private DatabaseHelper db;
    private MessageAdapter adapter;
    private long contactId;

    private EditText edtMessage;
    private CheckBox cbAsOther;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);
        // Đệm cả thanh hệ thống và bàn phím để khung nhập không bị che
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        db = new DatabaseHelper(this);

        // Nhận dữ liệu từ Intent
        contactId = getIntent().getLongExtra(EXTRA_CONTACT_ID, -1);
        Contact contact = db.getContact(contactId);
        if (contact == null) {
            finish();
            return;
        }

        ImageButton btnBack = findViewById(R.id.btnBack);
        TextView tvAvatar = findViewById(R.id.tvAvatar);
        TextView tvName = findViewById(R.id.tvName);
        TextView tvPhone = findViewById(R.id.tvPhone);
        ListView lvMessages = findViewById(R.id.lvMessages);
        TextView tvEmpty = findViewById(R.id.tvEmpty);
        ImageButton btnSend = findViewById(R.id.btnSend);
        edtMessage = findViewById(R.id.edtMessage);
        cbAsOther = findViewById(R.id.cbAsOther);

        tvAvatar.setText(Utils.initial(contact.getName()));
        Utils.tintAvatar(tvAvatar, contact.getName());
        tvName.setText(contact.getName());
        tvPhone.setText(contact.getPhone());
        tvPhone.setVisibility(contact.getPhone() == null || contact.getPhone().isEmpty()
                ? View.GONE : View.VISIBLE);

        adapter = new MessageAdapter(this, new ArrayList<>(), contact.getName());
        lvMessages.setAdapter(adapter);
        lvMessages.setEmptyView(tvEmpty);

        lvMessages.setOnItemLongClickListener((parent, view, position, id) -> {
            showMessageDialog(adapter.getItem(position));
            return true;
        });

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());

        loadMessages();
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }

    private void loadMessages() {
        adapter.setData(db.getMessages(contactId));
    }

    private void sendMessage() {
        String content = edtMessage.getText().toString().trim();
        if (content.isEmpty()) return;
        // Checkbox cho phép giả lập tin nhắn đến từ người kia
        db.addMessage(contactId, content, !cbAsOther.isChecked());
        edtMessage.setText("");
        loadMessages();
    }

    /** CUSTOM AlertDialog dùng layout dialog_message.xml để sửa / xóa tin nhắn. */
    private void showMessageDialog(Message message) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_message, null);
        TextView tvSentAt = view.findViewById(R.id.tvSentAt);
        TextInputLayout tilContent = view.findViewById(R.id.tilContent);
        EditText edtContent = view.findViewById(R.id.edtContent);
        Button btnDelete = view.findViewById(R.id.btnDelete);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnSave = view.findViewById(R.id.btnSave);

        tvSentAt.setText((message.isFromMe() ? "Bạn gửi lúc " : "Nhận lúc ")
                + Utils.fullTime(message.getCreatedAt()));
        edtContent.setText(message.getContent());
        edtContent.setSelection(edtContent.getText().length());

        AlertDialog dialog = new AlertDialog.Builder(this).setView(view).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String content = edtContent.getText().toString().trim();
            if (content.isEmpty()) {
                tilContent.setError(getString(R.string.err_message_required));
                return;
            }
            db.updateMessage(message.getId(), content);
            loadMessages();
            Toast.makeText(this, R.string.msg_updated, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            new AlertDialog.Builder(this)
                    .setTitle(R.string.confirm_delete_title)
                    .setMessage(R.string.confirm_delete_message)
                    .setNegativeButton(R.string.btn_cancel, null)
                    .setPositiveButton(R.string.btn_delete, (d, w) -> {
                        db.deleteMessage(message.getId());
                        loadMessages();
                        Toast.makeText(this, R.string.msg_deleted, Toast.LENGTH_SHORT).show();
                    })
                    .show();
        });

        dialog.show();
    }
}
