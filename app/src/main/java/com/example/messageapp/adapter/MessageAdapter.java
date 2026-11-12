package com.example.messageapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.messageapp.R;
import com.example.messageapp.Utils;
import com.example.messageapp.model.Message;

import java.util.List;

/**
 * Custom Adapter cho ListView tin nhắn.
 * Dùng 2 kiểu view: tin mình gửi (bên phải) và tin nhận (bên trái).
 */
public class MessageAdapter extends BaseAdapter {

    private static final int TYPE_SENT = 0;
    private static final int TYPE_RECEIVED = 1;

    private final Context context;
    private final String contactName;
    private List<Message> data;

    public MessageAdapter(Context context, List<Message> data, String contactName) {
        this.context = context;
        this.data = data;
        this.contactName = contactName;
    }

    public void setData(List<Message> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() { return data.size(); }

    @Override
    public Message getItem(int position) { return data.get(position); }

    @Override
    public long getItemId(int position) { return data.get(position).getId(); }

    @Override
    public int getViewTypeCount() { return 2; }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).isFromMe() ? TYPE_SENT : TYPE_RECEIVED;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int type = getItemViewType(position);
        ViewHolder h;
        if (convertView == null) {
            int layout = type == TYPE_SENT ? R.layout.item_message_sent : R.layout.item_message_received;
            convertView = LayoutInflater.from(context).inflate(layout, parent, false);
            h = new ViewHolder();
            h.tvContent = convertView.findViewById(R.id.tvContent);
            h.tvTime = convertView.findViewById(R.id.tvTime);
            h.tvAvatar = convertView.findViewById(R.id.tvAvatar); // null với tin gửi
            convertView.setTag(h);
        } else {
            h = (ViewHolder) convertView.getTag();
        }

        Message m = getItem(position);
        h.tvContent.setText(m.getContent());
        h.tvTime.setText(Utils.fullTime(m.getCreatedAt()));
        if (h.tvAvatar != null) {
            h.tvAvatar.setText(Utils.initial(contactName));
            Utils.tintAvatar(h.tvAvatar, contactName);
        }
        return convertView;
    }

    private static class ViewHolder {
        TextView tvContent, tvTime, tvAvatar;
    }
}
