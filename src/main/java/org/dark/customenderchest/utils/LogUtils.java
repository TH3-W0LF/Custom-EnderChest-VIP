package org.dark.customenderchest.utils;

import org.dark.customenderchest.CustomEnderChest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogUtils {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static File logFile;

    public static void init(CustomEnderChest plugin) {
        logFile = new File(plugin.getDataFolder(), "admin_logs.txt");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create log file!");
            }
        }
    }

    public static void log(String message) {
        if (logFile == null) return;
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            String logEntry = "[" + LocalDateTime.now().format(DATE_FORMAT) + "] " + message;
            writer.write(logEntry);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

