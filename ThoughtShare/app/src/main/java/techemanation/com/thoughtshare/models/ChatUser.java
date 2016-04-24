package techemanation.com.thoughtshare.models;

/**
 * Created by Srinivas_Cheruku on 20/03/2016.
 */
public class ChatUser
{
    private String chatUserName;
    private String city;

    public ChatUser(String chatUserName, String city) {
        this.chatUserName = chatUserName;
        this.city = city;
    }

    public String getChatUserName() {
        return chatUserName;
    }

    public String getCity() {
        return city;
    }
}
