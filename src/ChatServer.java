import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ChatServer {
    private ServerSocket serverSocket;
    private Map<String, ClientHandler> clients = new HashMap<>();
    private Map<Integer, ChatRoom> chatRooms = new HashMap<>();
    private int roomCounter = 0;

    public Map<Integer, ChatRoom> getChatRooms() {
        return chatRooms;
    }

    public Map<String, ClientHandler> getClients() {
        return clients;
    }

    public ChatServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("서버가 포트 " + port + "에서 시작되었습니다.");
    }

    public void start() {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler client = new ClientHandler(clientSocket, this);
                client.start();
            } catch (IOException e) {
                System.out.println("클라이언트 연결 에러: " + e.getMessage());
            }
        }
    }

    public synchronized boolean addClient(String nickname, ClientHandler handler) {
        if (!clients.containsKey(nickname)) {
            clients.put(nickname, handler);
            System.out.println(nickname + " 사용자가 연결되었습니다.");
            return true;
        } else {
            handler.sendMessage("닉네임이 이미 사용 중입니다. 다른 닉네임을 선택해주세요.");
            return false;
        }
    }

    public synchronized void removeClient(String nickname) {
        clients.remove(nickname);
        broadcast("로비: " + nickname + " 사용자가 연결을 끊었습니다.");
        System.out.println(nickname + " 사용자가 연결을 끊었습니다.");
    }

    public synchronized void createRoom(ClientHandler handler, String password) {
        ChatRoom newRoom = new ChatRoom(++roomCounter, password.isEmpty() ? null : password);
        chatRooms.put(roomCounter, newRoom);
        handler.sendMessage("방 번호 " + roomCounter + "가 생성되었습니다. " +
                (password.isEmpty() ? "비밀번호가 없습니다." : "비밀번호가 설정되었습니다.") +
                " /join " + roomCounter + (password.isEmpty() ? "" : " [password]") + "로 입장하세요.");
    }


    public synchronized void joinRoom(int roomId, ClientHandler handler, String password) {
        if (handler.getCurrentRoomId() != null) {
            exitRoom(handler.getCurrentRoomId(), handler);  // 기존 방을 떠나는 로직 추가
        }
        ChatRoom room = chatRooms.get(roomId);
        if (room != null) {
            if (room.addParticipant(handler, password)) {
                handler.setCurrentRoomId(roomId);
                System.out.println(handler.getClientsName() + "님이 방 " + roomId + "에 입장했습니다.");
            } else {
                handler.sendMessage("잘못된 비밀번호입니다.");
            }
        } else {
            handler.sendMessage("방 번호 " + roomId + "는 존재하지 않습니다.");
        }
    }

    public synchronized void exitRoom(int roomId, ClientHandler handler) {
        ChatRoom room = chatRooms.get(roomId);
        if (room != null) {
            room.removeParticipant(handler);
            handler.setCurrentRoomId(null);
            room.broadcast(handler.getClientsName() + "님이 방을 떠났습니다.");
            if (room.isEmpty()) {
                chatRooms.remove(roomId);
                System.out.println("방 번호 " + roomId + "가 삭제되었습니다.");
            }
        } else {
            handler.sendMessage("이미 방에서 나갔습니다.");
        }
    }

    public String listRooms() {
        if (chatRooms.isEmpty()) {
            return "현재 활성화된 채팅방이 없습니다.";
        } else {
            StringBuilder sb = new StringBuilder("활성화된 채팅방 목록:\n");
            chatRooms.forEach((id, room) -> sb.append("방 번호 : ").append(id).append(" || 비밀번호 여부 : " + (room.hasPassword()?"O":"X")).append("\n"));
            return sb.toString();
        }
    }

    public String listUsers() {
        StringBuilder sb = new StringBuilder("현재 유저 목록\n");
        clients.forEach((clientsName, handler) -> {
            Integer currentRoomId = handler.getCurrentRoomId();
            sb.append("유저 이름 : ").append(clientsName)
                    .append(", 현재 위치 : ").append(currentRoomId == null ? "로비" : currentRoomId + "번방")
                    .append("\n");
        });
        return sb.toString();
    }



    public void broadcast(String message) {
        clients.values().forEach(receiver -> {
            // 발신자 정보가 없으므로, 단순히 로비 메시지가 아니라면 발신자 이름을 메시지에서 추출해야 합니다.
            // 여기서는 예시로 "사용자명: 메시지" 형식으로 가정하고, 사용자명을 추출합니다.
            String senderName = message.split(":")[0];

            // 수신자(receiver)의 차단 목록을 확인하여 발신자(senderName)를 차단했는지 검사합니다.
            if (!receiver.getBlockWhisper().getOrDefault(senderName, false)) {
                receiver.sendMessage(message);
            }
        });
    }

    public static void main(String[] args) throws IOException {
        ChatServer server = new ChatServer(12356);
        server.start();
    }
}
