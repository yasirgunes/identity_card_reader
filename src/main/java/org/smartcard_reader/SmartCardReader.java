package org.smartcard_reader;

import javax.smartcardio.*;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
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

            // wait for 150 miliseconds to give the computer some time to read the card
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            CardChannel channel = card.getBasicChannel();

            List<String> citizenInfo = new ArrayList<>();
            // select the master file
            send_command(channel, "00A40000023F0000");


            // verify pin
//            String pin = "155882";
//            System.out.println("PIN: " + pin);
//            send_command(channel, "0020000106" + "313535383832");

            // select KD-PKCS
            send_command(channel, "00A40100023D0000");

            //select the file
            send_command(channel, "00A40200022F0400");

            // read the file
            String response1 = send_command(channel, "00B0000000");


            // select mf
            send_command(channel, "00A40000023F0000");

            // select TCKKEK folder
            send_command(channel, "00A40100023D1000");

            // select folder 2
            send_command(channel, "00A40100023D2000");

            // select elementary file
            send_command(channel, "00A40200022F1A00");

            // read the file
            String response2 = send_command(channel, "00B0000000");
            getCitizenInfo(response1, response2, citizenInfo);

            // select mf
            send_command(channel, "00A40000023F0000");

            // elementary file select
            send_command(channel, "00A40200022F5300");

            // read the file
            String cert = send_command(channel, "00B0000000");
            X509Certificate certificate = getCertificateFromString(cert);
            verifyCertificate(certificate);


//            System.out.println("TCKN: " + citizenInfo.get(0));
//            System.out.println("Name: " + citizenInfo.get(1));
//            System.out.println("Serial Number: " + citizenInfo.get(2));


            card.disconnect(false); // 'false' means no reset of the card
            System.out.println("Disconnected from card.");

        } catch (CardException e) {
            System.err.println("Error listing card readers: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
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

            System.out.println("SW1: " + Integer.toHexString(sw1));
            System.out.println("SW2: " + Integer.toHexString(sw2));
            System.out.println("Response: " + byteArrayToHex(responseData));
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

    public static void getCitizenInfo(String response1, String response2, List<String> citizenInfo) {
        String tckn = extractTCKN(response1);
        String name = extractName(response1);
        String serial_number = extractSerialNumber(response2);
        citizenInfo.add(tckn);
        citizenInfo.add(name);
        citizenInfo.add(serial_number);
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

    public static String extractSerialNumber(String text) {
        // Regular expression to match the serial number pattern
        Pattern serialPattern = Pattern.compile("[A-Z0-9]{9}");  // Assuming a serial number is exactly 9 alphanumeric characters
        Matcher matcher = serialPattern.matcher(text);
        if (matcher.find()) {
            return matcher.group().trim();
        }
        return "Serial number not found";
    }

    public static X509Certificate getCertificateFromString(String certStr) throws Exception {
        String cleanedCertStr = certStr.replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", ""); // Remove newlines and spaces

        byte[] certBytes = Base64.getDecoder().decode(cleanedCertStr);
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(certBytes));
    }

    public static void verifyCertificate(X509Certificate cert) throws Exception {
        cert.checkValidity(); // Checks if the certificate is valid (not expired)
        System.out.println("Certificate is valid.");
    }
    public static byte[] hashData(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(data);
    }

    public static byte[] signData(CardChannel channel, byte[] hash) throws Exception {
        // Example APDU command to sign data
        String commandStr = "00 2A 9E 9A"; // Example command header for a signing operation, adjust as necessary
        byte[] command = concatenate(hexStringToByteArray(commandStr), hash);
        CommandAPDU signCommand = new CommandAPDU(command);

        // Transmit the signing command to the smart card
        ResponseAPDU response = channel.transmit(signCommand);
        return response.getData(); // The signature
    }

    private static byte[] concatenate(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
