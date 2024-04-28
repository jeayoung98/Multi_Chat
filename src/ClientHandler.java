import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class ClientHandler extends Thread {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ChatServer server;
    private String clientsName;
    private Integer currentRoomId = null;

    private Map<String, Boolean> blockWhisper = new HashMap<>();

    public String getClientsName() {
        return clientsName;
    }

    public Integer getCurrentRoomId() {
        return currentRoomId;
    }

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.out.println("Error handling client: " + e.getMessage());
            closeConnection();
        }
    }

    public void run() {
        try {
            clientsName = in.readLine(); // 클라이언트로부터 닉네임을 받음
            if (server.addClient(clientsName, this)) { // 닉네임을 서버에 추가하고 중복 검사
                out.println("OK");
                server.broadcast("로비: " + clientsName + " 사용자가 연결되었습니다.");
                out.println("명령어 모음 : /help");
                handleClient(); // 닉네임이 유효하면 클라이언트 처리 시작
            } else {
                closeConnection(); // 중복 닉네임인 경우 연결 종료
            }
        } catch (IOException e) {
            System.out.println(clientsName + "의 연결이 끊어졌습니다.");
            if (clientsName != null) {
                server.removeClient(clientsName);
            }
        } finally {
            closeConnection(); // 모든 처리 후 연결 종료
        }
    }
    private void handleClient() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("/")) {
                    command(line); // 명령어 처리
                } else {
                    // 로비 또는 채팅방에서 메시지 처리
                    if (currentRoomId == null) {
                        server.broadcast(clientsName + ": " + line);
                    } else {
                        server.getChatRooms().get(currentRoomId).broadcast(clientsName + ": " + line);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(clientsName + "의 연결이 끊어졌습니다.");
        } finally {
            server.removeClient(clientsName); // 클라이언트 연결 종료 시 클라이언트 목록에서 제거
            closeConnection();
        }
    }

    private void command(String command) {
        String[] parts = command.split(" ", 3);
        switch (parts[0]) {
            case "/r":
                if (parts.length > 2) {
                    whisper(parts[1], parts[2]); // 귓속말 명령어 처리
                } else {
                    sendMessage("귓속말 형식이 잘못되었습니다. '/r 닉네임 메시지' 형식으로 입력해주세요.");
                }
                break;
            case "/join":
                if (parts.length > 1) {
                    try {
                        int roomId = Integer.parseInt(parts[1]);
                        String roomPassword = (parts.length > 2) ? parts[2] : "";  // 비밀번호가 없는 경우 빈 문자열로 처리
                        server.joinRoom(roomId, this, roomPassword);
                    } catch (NumberFormatException e) {
                        sendMessage("잘못된 방 번호 형식입니다.");
                    }
                } else {
                    sendMessage("'/join [roomID] [password]' 형식으로 입력해주세요");
                }
                break;
            case "/exit":
                if (currentRoomId != null) {
                    server.exitRoom(currentRoomId, this);
                    currentRoomId = null;
                    sendMessage("채팅방을 나갔습니다. 로비로 이동합니다.");
                } else {
                    sendMessage("채팅방이 없습니다.");
                }
                break;
            case "/create":
                // 비밀번호를 입력하지 않은 경우 빈 문자열로 처리
                String password = parts.length > 1 ? parts[1] : "";
                server.createRoom(this, password);
                break;
            case "/help":
                sendMessage("/r : 귓속말\n/block : 귓속말 차단\n/blockAll : 모든 채팅 차단\n/unblock : 차단풀기\n/join : 채팅방 입장\n/create : 채팅방 생성\n/exit : 채팅방 퇴장");
                break;
            case "/block":
                blockUser(parts);
                break;
            case "/blockAll":
                blockUserAll(parts);
                break;
            case "/unblock":
                unblockUser(parts);
                break;
            default:
                sendMessage("알 수 없는 명령어: " + parts[0]);
                break;
        }
    }

    private void whisper(String name, String message) {
        ClientHandler receiver = server.getClients().get(name);
        if (receiver != null) {
            Boolean isBlocked = receiver.blockWhisper.get(clientsName);
            if (isBlocked == null || !isBlocked) {
                receiver.sendMessage(clientsName + "님의 귓속말: " + message);
                sendMessage("귓속말을 " + name + "님에게 전송했습니다.");
            } else {
                sendMessage(name + "님이 " + clientsName + "님의 귓속말을 차단하셨습니다.");
            }
        } else {
            sendMessage(name + "님을 찾을 수 없습니다.");
        }
    }


    private void blockUser(String[] parts) {
        if (parts.length < 2) {
            sendMessage("차단할 사용자의 이름을 입력해주세요. '/block 사용자이름'");
            return;
        }
        String userBlock = parts[1];
        blockWhisper.put(userBlock, false);
        sendMessage(userBlock + "님의 귓속말을 차단하였습니다.");
    }

    private void blockUserAll(String[] parts) {
        if (parts.length < 2) {
            sendMessage("차단할 사용자의 이름을 입력해주세요. '/blockAll 사용자이름'");
            return;
        }
        String userBlock = parts[1];
        blockWhisper.put(userBlock, true);
        sendMessage(userBlock + "님의 채팅을 모두 차단하였습니다.");
    }

    private void unblockUser(String[] parts) {
        if (parts.length < 2) {
            sendMessage("차단 해제할 사용자의 이름을 입력해주세요. '/unblock 사용자이름'");
            return;
        }
        String userUnblock = parts[1];
        if (blockWhisper.containsKey(userUnblock)) {
            blockWhisper.remove(userUnblock);
            sendMessage(userUnblock + "님의 귓속말 차단을 해제하였습니다.");
        } else {
            sendMessage(userUnblock + "님은 차단되지 않았습니다.");
        }
    }



    public void sendMessage(String message) {
        out.println(message);
    }

    private void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.out.println("버그 ㄷㄷ: " + e.getMessage());
        }
    }

    public void setCurrentRoomId(Integer roomId) {
        this.currentRoomId = roomId;
    }
}
