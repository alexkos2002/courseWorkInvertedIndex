package network;

import index.ParallelBoostedWordTextFileInvertedIndex;
import index.SequentialWordTextFileInvertedIndex;
import index.building.strategy.ParallelWordTextFileInvertedIndexBuildingStrategy;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
public class InvertedIndexTCPServer {

    private static final String NOT_ALL_PARAMS_ENTERED_MESSAGE = "Not all needed parameters(file) " +
            "were entered.\n";

    private static final String SUCCESSFUL_CONNECTION_MESSAGE = "Client %s has been successfully connected. Starting getting " +
            "documents from inverted index for words.\n";



    private static final String DOCS_FOR_WORD_SUCCESSFUL_SENT_MESSAGE = "Documents which contain word %s were got and " +
            "sent to the client %s successfully.\n";

    private static final String SERVER_SUCCESSFUL_FINISH_MESSAGE = "Document lists for all clients have been sent successfully.\n" +
            "Finishing work...";

    private static final String CLIENT_HANDLER_SUCCESSFUL_FINISH_MESSAGE = "Document lists for the client %s have been " +
            "sent successfully.\nFinishing work...";

    private static final String CLIENT_CONNECTION_FINISH_MESSAGE = "Client %s has been disconnected.\n";

    private static final String EXCEPTION_MESSAGE = "Exception!!!\n";

    private ParallelBoostedWordTextFileInvertedIndex invertedIndex;

    private List<ClientHandler> clientHandlers;

    public InvertedIndexTCPServer(ParallelBoostedWordTextFileInvertedIndex invertedIndex) {
        this.invertedIndex = invertedIndex;
        this.clientHandlers = new ArrayList<>();
    }

    public static void main(String[] args) {
        final String filesToIndexDirectoryPath;
        try {
            filesToIndexDirectoryPath = args[0];
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println(NOT_ALL_PARAMS_ENTERED_MESSAGE);
            return;
        }
        ParallelBoostedWordTextFileInvertedIndex invertedIndex = new ParallelBoostedWordTextFileInvertedIndex();
        //SequentialWordTextFileInvertedIndex invertedIndex = new SequentialWordTextFileInvertedIndex();
        long startTime = System.currentTimeMillis();
        invertedIndex.build(new ParallelWordTextFileInvertedIndexBuildingStrategy(4), filesToIndexDirectoryPath);
        System.out.println("Time elapsed: " + (System.currentTimeMillis() - startTime));
        InvertedIndexTCPServer server = new InvertedIndexTCPServer(invertedIndex);
        server.initAndStart();
    }

    public void initAndStart() {
        try (ServerSocket serverSocket = new ServerSocket(4999)) {
            int clientHandlerCurId = 0;
            while (true) {
                Socket clientConnectionSocket = serverSocket.accept();
                ClientHandler curClientHandler = new ClientHandler(clientHandlerCurId, clientConnectionSocket, invertedIndex);
                clientHandlers.add(curClientHandler);
                curClientHandler.start();
                System.out.println(String.format(SUCCESSFUL_CONNECTION_MESSAGE, clientConnectionSocket.getInetAddress() +
                        ":" + clientConnectionSocket.getPort()));
                clientHandlerCurId++;
            }
        } catch (IOException e) {
            System.out.println(EXCEPTION_MESSAGE);
            e.printStackTrace();
        }
        System.out.println(SERVER_SUCCESSFUL_FINISH_MESSAGE);
    }

    class ClientHandler extends Thread {

        private int id;
        private Socket clientConnectionSocket;
        private ParallelBoostedWordTextFileInvertedIndex invertedIndex;

        public ClientHandler(int id, Socket clientConnectionSocket, ParallelBoostedWordTextFileInvertedIndex invertedIndex) {
            this.id = id;
            this.clientConnectionSocket = clientConnectionSocket;
            this.invertedIndex = invertedIndex;
        }

        @Override
        public void run() {
            try (ObjectOutputStream clientOOS = new ObjectOutputStream(clientConnectionSocket.getOutputStream());
                 DataInputStream clientDIS = new DataInputStream(clientConnectionSocket.getInputStream())) {
                String curWord;
                while (true) {
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    while ((curWord = clientDIS.readUTF()) == null);
                    String docListStringResponse = invertedIndex.getSourcesList(curWord).toString();
                    clientOOS.writeObject(docListStringResponse);
                    System.out.println(String.format(DOCS_FOR_WORD_SUCCESSFUL_SENT_MESSAGE, curWord,
                            clientConnectionSocket.getInetAddress() + ":" + clientConnectionSocket.getPort()));
                }
            } catch (EOFException | SocketException e) { //end of data streams on client connection socket means closing of this socket from the client side
                System.out.println(String.format(CLIENT_CONNECTION_FINISH_MESSAGE, clientConnectionSocket.getInetAddress() +
                        ":" + clientConnectionSocket.getPort()));
            } catch (IOException e) {
                System.out.println("LALALALA");
                System.out.println(EXCEPTION_MESSAGE);
                e.printStackTrace();
            }
            System.out.println(String.format(CLIENT_HANDLER_SUCCESSFUL_FINISH_MESSAGE, clientConnectionSocket.getInetAddress() +
                    ":" + clientConnectionSocket.getPort()));
        }

    }

}
