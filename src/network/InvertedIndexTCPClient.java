package network;

import java.io.*;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class InvertedIndexTCPClient {

    private static final String NOT_ALL_PARAMS_ENTERED_MESSAGE = "Not all needed parameters(words and results file paths) " +
            "were entered.";

    private static final String SUCCESSFUL_CONNECTION_MESSAGE = "Successfully connected to the server. Starting getting " +
            "documents from inverted index for words.\n";

    private static final String DOCS_FOR_WORD_GETTING_START_MESSAGE = "Starting getting documents which contain word %s...\n";

    private static final String DOCS_FOR_WORD_SUCCESSFUL_GET_MESSAGE = "Documents which contain word %s were received " +
            "successfully.\n%s : %s.\n";

    private static final String IS_PROCEEDING_NEEDED_MESSAGE = "Press N to proceed to the next word. Press E to exit.";

    private static final String NOT_CORRECT_INPUT_WAS_ENTERED = "Not correct input was entered.";

    private static final String SUCCESSFUL_FINISH_MESSAGE = "Documents for all words have been received successfully.\n" +
            "Finishing work...";

    private static final String EXCEPTION_MESSAGE = "Exception!!!\n";

    public static void main(String[] args) {
        final String wordsFilePath;
        final String resultsFilePath;
        String isProcToNextWordNeeded;
        try {
            wordsFilePath = args[0];
            resultsFilePath = args[1];
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println(NOT_ALL_PARAMS_ENTERED_MESSAGE);
            return;
        }
        try (Scanner consoleScanner = new Scanner(System.in);
             FileInputStream wordsFileFIS = new FileInputStream(wordsFilePath);
             Scanner wordsFileScanner = new Scanner(wordsFileFIS);
             FileWriter resultsFileFW = new FileWriter(resultsFilePath);
             Socket socket = new Socket("127.0.0.1", 4999);
             DataOutputStream serverDOS = new DataOutputStream(socket.getOutputStream());
             ObjectInputStream serverOIS = new ObjectInputStream(socket.getInputStream())) {

            System.out.println(SUCCESSFUL_CONNECTION_MESSAGE);
            String curWord;
            String curDocsForWordResponse;
            while (true) {
                try {
                    curWord = wordsFileScanner.nextLine();
                } catch (NoSuchElementException e) {
                    break;
                }
                System.out.println(String.format(DOCS_FOR_WORD_GETTING_START_MESSAGE, curWord));
                serverDOS.writeUTF(curWord);
                while ((curDocsForWordResponse = (String) serverOIS.readObject()) == null);
                resultsFileFW.write(String.format("%s : %s\n", curWord, curDocsForWordResponse));
                System.out.println(String.format(DOCS_FOR_WORD_SUCCESSFUL_GET_MESSAGE, curWord, curWord, curDocsForWordResponse));
                System.out.println();
                /*System.out.println(IS_PROCEEDING_NEEDED_MESSAGE);
                isProcToNextWordNeeded = consoleScanner.next();
                while (!(isProcToNextWordNeeded.equals("N") || isProcToNextWordNeeded.equals("E"))) {
                    System.out.println(NOT_CORRECT_INPUT_WAS_ENTERED);
                    System.out.println(IS_PROCEEDING_NEEDED_MESSAGE);
                    isProcToNextWordNeeded = consoleScanner.next();
                }
                if (isProcToNextWordNeeded.equals("E")) {
                    break;
                }*/
            }
            System.out.println(SUCCESSFUL_FINISH_MESSAGE);
        } catch (Exception e) {
            System.out.println(EXCEPTION_MESSAGE);
            e.printStackTrace();
        }
    }

}
