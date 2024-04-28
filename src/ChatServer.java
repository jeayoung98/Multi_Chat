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

    public void broadcast(String message) {
        clients.values().forEach(client -> client.sendMessage(message));
    }

    public static void main(String[] args) throws IOException {
        ChatServer server = new ChatServer(12356);
        server.start();
    }
}
