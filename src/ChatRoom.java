import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class ChatRoom {
    private int roomId;
    private String password;
    private Set<ClientHandler> participants = new HashSet<>();
    private File chatLog;

    public ChatRoom(int roomId,String password) {
        this.roomId = roomId;
        this.password = password;
        this.chatLog = new File("ChatRoom_" + roomId + ".txt");
        if (!chatLog.exists()) {
            try {
                chatLog.createNewFile();
            } catch (IOException e) {
                System.out.println("파일을 생성할 수 없습니다." + e.getMessage());
            }
        }
    }

    public boolean addParticipant(ClientHandler participant, String password) {
        if (this.password == null || this.password.isEmpty() || this.password.equals(password)) {
            participants.add(participant);
            broadcast("방 "+roomId + "에 "+ participant.getClientsName() + "님이 입장했습니다.");
            return true;
        }
        participant.sendMessage("잘못된 비밀번호입니다.");
        return false;
    }

    public void removeParticipant(ClientHandler participant) {
        participants.remove(participant);
        if (isEmpty()) {
            chatLog.delete();  // 채팅방이 비었을 때 로그 파일 삭제
        }
    }

    public void broadcast(String message) {
        participants.forEach(participant ->{
                    String senderName = message.split(":")[0];
                    if (!participant.getBlockWhisper().getOrDefault(senderName, false)) {
                        participant.sendMessage(message);
                    }
                });
        appendLog(message);
    }

    public boolean isEmpty() {
        return participants.isEmpty();
    }
    private void appendLog(String message) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(chatLog, true))) {
            writer.write(message + "\n");
        } catch (IOException e) {
            System.out.println("로그 파일에 기록할 수 없습니다: " + e.getMessage());
        }
    }
}
