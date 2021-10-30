import java.io.*;
import java.net.*;
import java.nio.file.Files;

class SFTP_Client {
    private static String fileName = null;
    private static int fileLength = 0;
    private static String fileToSend = null;

    public static void main(String[] argv) throws Exception {

        //noinspection InfiniteLoopStatement
        while (true) {
            String userCommand;
            String sentence;
            StringBuilder text = new StringBuilder();
            int x;

            BufferedReader inFromUser =
                    new BufferedReader(new InputStreamReader(System.in));

            Socket clientSocket = new Socket("localhost", 6789);

            DataOutputStream outToServer =
                    new DataOutputStream(clientSocket.getOutputStream());

            OutputStream outputStream = clientSocket.getOutputStream();

            BufferedReader inFromServer =
                    new BufferedReader(new
                            InputStreamReader(clientSocket.getInputStream()));

            sentence = inFromUser.readLine();

            userCommand = sentence.substring(0, Math.min(sentence.length(), 4));

            outToServer.writeBytes(sentence + '\n');

            // Receiving file from server and writing into a new file
            if (userCommand.equals("SEND") && fileName != null) {
                byte[] fileFromServer = new byte[fileLength];
                for (int i=0; i<fileLength; i++) {
                    fileFromServer[i] = (byte) clientSocket.getInputStream().read();
                }
                try (FileOutputStream stream = new FileOutputStream(fileName)) {
                    stream.write(fileFromServer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println(fileName + " received");
                inFromServer.readLine();
                fileName = null;
            } else if (userCommand.equals("STOR")) {
                // Prepare to send a file
                try {
                    fileToSend = sentence.substring(9);
                    File file = new File(fileToSend);
                    if (!file.exists()) {
                        System.out.println("File name is invalid");
                    }
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }

            while (true) {
                x = inFromServer.read();
                if ((char) x == '\0' && text.length() > 0) break;
                if ((char) x != '\0') text.append((char) x);
            }

            System.out.println("FROM SERVER: " + text);

            if ("RETR".equals(userCommand) && text.substring(0, 1).equals("+")) {
                retrCmd(sentence, text);
            } else if ("SIZE".equals(userCommand)) {
                // Send file
                File path = new File(fileToSend);
                try {
                    byte[] fileContent = Files.readAllBytes(path.toPath());
                    outputStream.write(fileContent);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                fileToSend = null;

                while (true) {
                    x = inFromServer.read();
                    if ((char) x == '\0' && text.length() > 0) break;
                    if ((char) x != '\0') text.append((char) x);
                }

                System.out.println("FROM SERVER: " + text);
            }
            clientSocket.close();
        }
    }

    private static void retrCmd(String sentence, StringBuilder text) {
        try {
            fileName = sentence.substring(5);
            fileLength = Integer.parseInt(text.substring(0,1));
        } catch (NumberFormatException e) {
            fileName = null;
        }
    }
}


