package org.griffty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Info {
    static {
        try {
            DISCORD_KEY = readFileContent("C:\\Users\\Griffty\\Desktop\\my\\work\\coding\\Java\\DiscrodFileSaver\\src\\main\\java\\DiscordToken.txt");
            DISCORD_SAVE_SERVER_ID = readFileContent("C:\\Users\\Griffty\\Desktop\\my\\work\\coding\\Java\\DiscrodFileSaver\\src\\main\\java\\DiscordServerID.txt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static final String DISCORD_KEY;
    public static final String DISCORD_SAVE_SERVER_ID;
    private static String readFileContent(String filePath) throws IOException {
        return Files.readString(Paths.get(filePath)).trim();
    }
}
