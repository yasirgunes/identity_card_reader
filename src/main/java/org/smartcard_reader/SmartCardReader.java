package org.smartcard_reader;

import javax.smartcardio.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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

            // select KD-PKCS
            send_command(channel, "00A40100023D0000");

            //select the file
            send_command(channel, "00A40200022F0400");

            // read the file
            String response = send_command(channel, "00B0000000");
            System.out.println("Response: " + response);

            // get the citizen info
//            String [] citizenInfo = new SmartCardReader().getCitizenInfo(response);
//            System.out.println("TCKN: " + citizenInfo[0]);
//            System.out.println("Name: " + citizenInfo[1]);


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

    private static String transmitCommand(CardChannel channel, CommandAPDU commandAPDU) {
        byte[] responseData;
        String response_in_UTF8 = "";
        try {
            ResponseAPDU responseAPDU = channel.transmit(commandAPDU);

            responseData = responseAPDU.getData();
            int sw1 = responseAPDU.getSW1();
            int sw2 = responseAPDU.getSW2();

            System.out.println("Response: " + byteArrayToHex(responseData));
            System.out.println("SW1: " + Integer.toHexString(sw1));
            System.out.println("SW2: " + Integer.toHexString(sw2));
            response_in_UTF8 = new String(responseData);
        } catch (CardException e) {
            System.err.println("Error sending command: " + e.getMessage());
        }
        return response_in_UTF8;
    }

    public static String send_command(CardChannel channel, String commandStr) {
        System.out.println();
        byte[] command = convertStringToByteArray(commandStr);
        System.out.println("Sending command: " + byteArrayToHex(command));

        CommandAPDU commandAPDU = new CommandAPDU(command);
        System.out.println();
        return transmitCommand(channel, commandAPDU);
    }

    public static String send_command(CardChannel channel, byte cla, byte ins, byte p1, byte p2, byte [] data) {
        System.out.println();
        System.out.println("Sending command: " + byteToHex(cla) + " " + byteToHex(ins) + " " + byteToHex(p1) + " " + byteToHex(p2) + " " + byteArrayToHex(data));
        CommandAPDU commandAPDU = new CommandAPDU(cla, ins, p1, p2, data);

        return transmitCommand(channel, commandAPDU);
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

    public String [] getCitizenInfo(String responseInUTF8) {
        String tckn = extractTCKN(responseInUTF8);
        String name = extractName(responseInUTF8);

        return new String[]{tckn, name};
    }
    // Method to extract TCKN
    public static String extractTCKN(String text) {
        Pattern tcknPattern = Pattern.compile("\\d{11}");
        Matcher matcher = tcknPattern.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return "TCKN not found";
    }

    // Method to extract Name
    public static String extractName(String text) {
        // Split the input text by "TR1604"
        String[] parts = text.split("TR1604");
        // Regular expression to match the name pattern
        Pattern namePattern = Pattern.compile("([A-ZÇĞİÖŞÜ]+\\s){1,4}[A-ZÇĞİÖŞÜ]+");
        Matcher matcher = namePattern.matcher(parts[0]);
        if (matcher.find()) {
            return matcher.group().trim();
        }
        return "Name not found";
    }
}
