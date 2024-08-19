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

            Card card = contactless.connect("T=0");
            System.out.println("Connected to card: " + card);

            // get the ATR
            ATR atr = card.getATR();
            System.out.println("ATR: " + byteArrayToHex(atr.getBytes()));

            CardChannel channel = card.getBasicChannel();

            byte[] command = new byte[] {
                    (byte) 0x00, // CLA (Class of instruction)
                    (byte) 0xA4, // INS (Instruction)
                    (byte) 0x04, // P1  (Parameter 1)
                    (byte) 0x00, // P2  (Parameter 2)
                    (byte) 0x02, // Lc  (Length of data)
                    (byte) 0x3F, (byte) 0x00  // Data (Select MF command)
            };

            CommandAPDU commandAPDU = new CommandAPDU(command);
            ResponseAPDU responseAPDU = channel.transmit(commandAPDU);

            byte[] responseData = responseAPDU.getData();
            int sw1 = responseAPDU.getSW1();
            int sw2 = responseAPDU.getSW2();

            System.out.println("Response: " + byteArrayToHex(responseData));
            System.out.println("SW1: " + Integer.toHexString(sw1));
            System.out.println("SW2: " + Integer.toHexString(sw2));

            card.disconnect(false); // 'false' means no reset of the card
            System.out.println("Disconnected from card.");


        } catch (CardException e) {
            System.err.println("Error listing card readers: " + e.getMessage());
        }
    }

    private static String byteArrayToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

}
