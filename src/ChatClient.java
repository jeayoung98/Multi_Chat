import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {
    public static void main(String[] args) {
        String hostName = "localhost"; // 서버가 실행 중인 호스트의 이름 또는 IP 주소
        int portNumber = 12356; // 서버와 동일한 포트 번호 사용

        try (Socket socket = new Socket(hostName, portNumber);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner stdIn = new Scanner(System.in)) {

            String serverResponse;
            String nickname;

            // 닉네임 입력과 중복 체크 반복
            while (true) {
                System.out.print("닉네임을 입력하세요: ");
                nickname = stdIn.nextLine();
                out.println(nickname); // 서버에 닉네임 전송

                serverResponse = in.readLine(); // 서버 응답 수신
                if ("OK".equals(serverResponse)) {
                    System.out.println("채팅에 연결되었습니다.");
                    break;
                } else {
                    System.out.println("닉네임이 이미 사용 중입니다. 다른 닉네임으로 다시 시도해주세요.");
                }
            }

            // 서버로부터 메시지를 읽어 화면에 출력하는 별도의 스레드
            Thread readThread = new Thread(new ServerMessageReader(in));
            readThread.start();

            // 사용자 입력 처리
            while (true) {
                String userInput = stdIn.nextLine();
                out.println(userInput); // 서버에 메시지 전송
                if ("/bye".equals(userInput)) {
                    break; // "/bye"를 입력하면 연결 종료
                }
            }
        } catch (IOException e) {
            System.out.println("서버에 연결할 수 없습니다: " + e.getMessage());
        }
    }
}