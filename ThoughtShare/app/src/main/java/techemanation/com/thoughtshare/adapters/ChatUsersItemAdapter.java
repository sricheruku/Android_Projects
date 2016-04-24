package techemanation.com.thoughtshare.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;
import techemanation.com.thoughtshare.R;
import techemanation.com.thoughtshare.models.ChatUser;

/**
 * Created by Srinivas_Cheruku on 20/03/2016.
 */
public class ChatUsersItemAdapter extends ArrayAdapter<ChatUser>
{
    private Context contextName;
    private int resourceChatUserItem;
    private List<ChatUser> listChatUsers;

    public ChatUsersItemAdapter(Context contextName, int resourceChatUserItem, List<ChatUser> listChatUsers)
    {
        super(contextName, resourceChatUserItem, listChatUsers);
        this.contextName=contextName;
        this.resourceChatUserItem=resourceChatUserItem;
        this.listChatUsers=listChatUsers;
    }

    static class ViewHolder
    {
        TextView tvChatUserName;
        TextView tvCity;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater;
        View listItem= convertView;
        ViewHolder viewHolder=new ViewHolder();

        if (listItem == null) {

            inflater = (LayoutInflater) contextName.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            listItem = inflater.inflate(resourceChatUserItem, null);

            viewHolder.tvChatUserName=(TextView) listItem.findViewById(R.id.tvChatUserName);
            viewHolder.tvCity=(TextView) listItem.findViewById(R.id.tvCity);

            listItem.setTag(viewHolder);
        }

        viewHolder=(ViewHolder) listItem.getTag();
        ChatUser chatMessage = listChatUsers.get(position);
        viewHolder.tvChatUserName.setText(chatMessage.getChatUserName());
        viewHolder.tvCity.setText(chatMessage.getCity());

        return listItem;
    }

    @Override
    public int getCount() {
        return listChatUsers.size();
    }

    @Override
    public long getItemId(int position) {
        return listChatUsers.indexOf(getItem(position));
    }

    @Override
    public ChatUser getItem(int position) {
        return listChatUsers.get(position);
    }
}
