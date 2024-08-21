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
            System.out.print("Connected to CONTACT: ");
            CardTerminal contactless = terminals.get(CONTACT);
            System.out.println(contactless.getName());

            System.out.println("Waiting for a card to be inserted...");
            contactless.waitForCardPresent(0);
            System.out.println("Card inserted. Connecting to card...");

            Card card = contactless.connect("T=1");
            System.out.println("Connected to card: " + card);

            // get the ATR
            ATR atr = card.getATR();
            System.out.println("ATR: " + byteArrayToHex(atr.getBytes()));

            CardChannel channel = card.getBasicChannel();


            // select the master file
            send_command(channel, "00A40000023F0000");


            // verify pin
            String pin = "155882";
            System.out.println("PIN: " + pin);
            send_command(channel, "0020000106" + "313535383832");

            // select file
            send_command(channel, "00A40100023D1000");

            // list dir
            send_command(channel, "801D000100");

            // select file
            send_command(channel, "00A40100023D3000");

            // list dir
            send_command(channel, "801D000000");

//            for(int i = 1; i < 40; i++) {
//                send_command(channel, "801D00"+ byteToHex((byte)i) +"00");
//            }

            // select file
            send_command(channel, "00A40200022F1200");


            // select file
            // send_command(channel, "00A40200023D3000");

            // read binary file
             send_command(channel, "00B0000000");


            card.disconnect(false); // 'false' means no reset of the card
            System.out.println("Disconnected from card.");

        } catch (CardException e) {
            System.err.println("Error listing card readers: " + e.getMessage());
        }
    }

    private static String byteArrayToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

    public static byte[] convertStringToByteArray(String commandStr) {
        int len = commandStr.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(commandStr.charAt(i), 16) << 4)
                    + Character.digit(commandStr.charAt(i+1), 16));
        }
        return data;
    }

    public static void printByteArray(byte[] byteArray) {
        System.out.println("byte[] command = new byte[] {");
        for (byte b : byteArray) {
            System.out.printf("    (byte) 0x%02X, // %s\n", b, getDescription(b));
        }
        System.out.println("};");
    }

    private static String getDescription(byte b) {
        // Add descriptions based on the byte value if needed
        // For now, return an empty string
        return "";
    }

    private static void transmitCommand(CardChannel channel, CommandAPDU commandAPDU) {
        try {
            ResponseAPDU responseAPDU = channel.transmit(commandAPDU);

            byte[] responseData = responseAPDU.getData();
            int sw1 = responseAPDU.getSW1();
            int sw2 = responseAPDU.getSW2();

            System.out.println("Response: " + byteArrayToHex(responseData));
            System.out.println("SW1: " + Integer.toHexString(sw1));
            System.out.println("SW2: " + Integer.toHexString(sw2));
        } catch (CardException e) {
            System.err.println("Error sending command: " + e.getMessage());
        }
    }

    public static void send_command(CardChannel channel, String commandStr) {
        System.out.println();
        byte[] command = convertStringToByteArray(commandStr);
        System.out.println("Sending command: " + byteArrayToHex(command));

        CommandAPDU commandAPDU = new CommandAPDU(command);
        transmitCommand(channel, commandAPDU);
        System.out.println();
    }

    public static void send_command(CardChannel channel, byte cla, byte ins, byte p1, byte p2, byte [] data) {
        System.out.println();
        System.out.println("Sending command: " + byteToHex(cla) + " " + byteToHex(ins) + " " + byteToHex(p1) + " " + byteToHex(p2) + " " + byteArrayToHex(data));
        CommandAPDU commandAPDU = new CommandAPDU(cla, ins, p1, p2, data);

        transmitCommand(channel, commandAPDU);
    }

    public static String byteToHex(byte b) {
        return String.format("%02X", b);
    }

    public static void send_APDU_command(CardChannel channel, CommandAPDU commandAPDU) {
        System.out.println();
        System.out.println("Sending command: " + commandAPDU.toString());
        transmitCommand(channel, commandAPDU);
        System.out.println();
    }
}
