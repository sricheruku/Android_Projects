package techemanation.com.thoughtshare.models;

/**
 * Created by Srinivas_Cheruku on 06/03/2016.
 */
public class ChatMessage
{
    private String message, time, chatParticipantType;

    public ChatMessage(String message, String time, String chatParticipantType)
    {
        this.message=message;
        this.time=time;
        this.chatParticipantType=chatParticipantType;
    }

    public String getMessage() {
        return message;
    }

    public String getTime() {
        return time;
    }

    public String getChatParticipantType() {
        return chatParticipantType;
    }
}

