package org.smartcard_reader;

import javax.smartcardio.*;
import java.util.Arrays;
import java.util.List;


public class SmartCardReader {

    private static final int CONTACT = 0;
    private static final int CONTACTLESS = 1;

    public static void main(String[] args) {
        try {
            // Get the default terminal factory
            TerminalFactory factory = TerminalFactory.getDefault();
            // Get the list of available terminals
            List<CardTerminal> terminals = factory.terminals().list();

            // Check if there are any terminals available
            if (terminals.isEmpty()) {
                System.out.println("No card readers found.");
            } else {
                // Print the names of the available terminals
                System.out.println("Available card readers:");
                for (CardTerminal terminal : terminals) {
                    System.out.println(terminal.getName());
                }

            }
            System.out.print("This is the contactless: ");
            CardTerminal contactless = terminals.get(CONTACTLESS);
            System.out.println(contactless.getName());

            System.out.println("Waiting for a card to be inserted...");
            contactless.waitForCardPresent(0);
            System.out.println("Card inserted. Connecting to card...");

            Card card = contactless.connect("*");
            System.out.println("Connected to card: " + card);

            CardChannel channel = card.getBasicChannel();
            byte[] command = new byte[] {
                    0x00, (byte) 0xA4, 0x04, 0x00, 0x02, 0x3F, 0x00
            };

            CommandAPDU commandAPDU = new CommandAPDU(command);
            ResponseAPDU responseAPDU = channel.transmit(commandAPDU);

            byte[] responseData = responseAPDU.getData();
            System.out.println("Response: " + Arrays.toString(responseData));

            card.disconnect(false); // 'false' means no reset of the card
            System.out.println("Disconnected from card.");


        } catch (CardException e) {
            System.err.println("Error listing card readers: " + e.getMessage());
        }
    }

    private static String byteArrayToHex(byte[] byteArray) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : byteArray) {
            hexString.append(String.format("%02X ", b));
        }
        return hexString.toString();
    }

}
