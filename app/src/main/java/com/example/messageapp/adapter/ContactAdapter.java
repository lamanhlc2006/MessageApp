package com.example.messageapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.messageapp.R;
import com.example.messageapp.Utils;
import com.example.messageapp.model.Contact;

import java.util.List;

/** Custom Adapter cho ListView danh sách cuộc trò chuyện. */
public class ContactAdapter extends BaseAdapter {

    private final Context context;
    private List<Contact> data;

    public ContactAdapter(Context context, List<Contact> data) {
        this.context = context;
        this.data = data;
    }

    public void setData(List<Contact> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() { return data.size(); }

    @Override
    public Contact getItem(int position) { return data.get(position); }

    @Override
    public long getItemId(int position) { return data.get(position).getId(); }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder h;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_contact, parent, false);
            h = new ViewHolder();
            h.tvAvatar = convertView.findViewById(R.id.tvAvatar);
            h.tvName = convertView.findViewById(R.id.tvName);
            h.tvTime = convertView.findViewById(R.id.tvTime);
            h.tvLastMessage = convertView.findViewById(R.id.tvLastMessage);
            h.tvPhone = convertView.findViewById(R.id.tvPhone);
            convertView.setTag(h);
        } else {
            h = (ViewHolder) convertView.getTag();
        }

        Contact c = getItem(position);
        h.tvAvatar.setText(Utils.initial(c.getName()));
        Utils.tintAvatar(h.tvAvatar, c.getName());
        h.tvName.setText(c.getName());
        h.tvTime.setText(Utils.shortTime(c.getLastTime()));
        h.tvLastMessage.setText(c.getLastMessage() != null
                ? c.getLastMessage() : context.getString(R.string.no_message_yet));
        h.tvPhone.setText(c.getPhone());
        h.tvPhone.setVisibility(c.getPhone() == null || c.getPhone().isEmpty()
                ? View.GONE : View.VISIBLE);
        return convertView;
    }

    /** ViewHolder giúp không phải findViewById lại mỗi lần cuộn. */
    private static class ViewHolder {
        TextView tvAvatar, tvName, tvTime, tvLastMessage, tvPhone;
    }
}
