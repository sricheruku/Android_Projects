package techemanation.com.thoughtshare.adapters;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.util.List;
import techemanation.com.thoughtshare.R;
import techemanation.com.thoughtshare.models.ChatMessage;
import techemanation.com.thoughtshare.models.ChatParticipantType;

/**
 * Created by Srinivas_Cheruku on 06/03/2016.
 */
public class ChatListItemAdapter extends ArrayAdapter<ChatMessage>
{
    private Context contextName;
    private int resourceChatListItem;
    private List<ChatMessage> listChatMessages;

    public ChatListItemAdapter(Context contextName, int resourceChatListItem, List<ChatMessage> listChatMessages)
    {
        super(contextName, resourceChatListItem, listChatMessages);
        this.contextName=contextName;
        this.resourceChatListItem=resourceChatListItem;
        this.listChatMessages=listChatMessages;
    }

    static class ViewHolder
    {
        TextView tvChatMessage;
        TextView tvChatTime;
        RelativeLayout relativeLayoutChatList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater;
        View listItem= convertView;
        ViewHolder viewHolder=new ViewHolder();

        if (listItem == null) {

            inflater = (LayoutInflater) contextName.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            listItem = inflater.inflate(resourceChatListItem, null);

            viewHolder.tvChatMessage=(TextView) listItem.findViewById(R.id.tvChatMessage);
            viewHolder.tvChatTime=(TextView) listItem.findViewById(R.id.tvChatTime);
            viewHolder.relativeLayoutChatList = (RelativeLayout) listItem.findViewById(R.id.relativeLayoutChatList);

            listItem.setTag(viewHolder);
        }

        viewHolder=(ViewHolder) listItem.getTag();
        ChatMessage chatMessage = listChatMessages.get(position);

        if(chatMessage.getChatParticipantType().equals(ChatParticipantType.OTHER_CHAT_PARTNER))
        {
            viewHolder.relativeLayoutChatList.setBackgroundResource(R.drawable.bubble_yellow);
            viewHolder.relativeLayoutChatList.setGravity(Gravity.START);
        }
        else if(chatMessage.getChatParticipantType().equals(ChatParticipantType.CURRENT_USER))
        {
            viewHolder.relativeLayoutChatList.setBackgroundResource(R.drawable.bubble_green);
            viewHolder.relativeLayoutChatList.setGravity(Gravity.END);
        }

        viewHolder.tvChatMessage.setText(chatMessage.getMessage());
        viewHolder.tvChatTime.setText(chatMessage.getTime());

        return listItem;
    }

    @Override
    public int getCount() {
        return listChatMessages.size();
    }

    @Override
    public long getItemId(int position) {
        return listChatMessages.indexOf(getItem(position));
    }

    @Override
    public ChatMessage getItem(int position) {
        return listChatMessages.get(position);
    }
}
