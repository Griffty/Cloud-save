package org.griffty;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Logger {
    private static Logger instance;
    public static synchronized Logger getInstance() {
        if (instance == null){
            instance = new Logger();
        }
        return instance;
    }
    private boolean mirrorLogsToConsole = true;
    private final LinkedBlockingQueue<LogRecord> loggingQueue;
    private final String programFolder;
    private volatile LoggerThread loggerThread ;
    private Logger(){
        loggingQueue = new LinkedBlockingQueue<>();
        String homePath = System.getProperty("user.home");
        programFolder = homePath + File.separator + "GrifftyLogs" + File.separator + "GrifftyCloudSaver";
        File file = new File(programFolder);
        Date currentDate = new Date();
        if (!file.exists()){
            if (file.mkdirs()){
                saveImportantLogMessage(currentDate, "Logger", "New home directory folder created");
            }
        }
        loggerThread = new LoggerThread(loggingQueue, programFolder, currentDate, mirrorLogsToConsole);
        loggerThread.start();

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        long initialDelay = findDelayToMidnight();

        executor.scheduleAtFixedRate(() -> {
            loggerThread.Terminate();
            Date date = new Date();
            loggerThread = new LoggerThread(loggingQueue, programFolder, date, mirrorLogsToConsole);
            loggerThread.start();
        }, initialDelay, TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS);
    }

    private static long findDelayToMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = LocalDateTime.of(now.toLocalDate(), java.time.LocalTime.MAX).plusSeconds(1);
        return Duration.between(now, midnight).getSeconds();
    }


    public void saveImportantLogMessage(String sender, String message){
        loggingQueue.add(new LogRecord(new Date(), sender, message, true));
    }
    public void saveImportantLogMessage(Date date, String sender, String message){
        loggingQueue.add(new LogRecord(date, sender, message, true));
    }

    public void saveLogMessage(String sender, String message){
        loggingQueue.add(new LogRecord(new Date(), sender, message, false));
    }
    public void saveLogMessage(Date date, String sender, String message){
        loggingQueue.add(new LogRecord(date, sender, message, false));
    }

    public void terminate() {
        loggerThread.Terminate();
    }
}

record LogRecord(Date date, String sender, String message, boolean important) {
    private static final SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS");

    @Override
    public String toString() {
        return important ? ("-----\n" + formatter.format(date) + " [" + sender + "] " + message + "\n-----")
                : (formatter.format(date) + " [" + sender + "] " + message);
    }
}
class LoggerThread extends Thread{
    private final LinkedBlockingQueue<LogRecord> loggingQueue;
    private final String programFolder;
    private final Date currentDate;
    private final boolean mirrorLogsToConsole;

    LoggerThread(LinkedBlockingQueue<LogRecord> loggingQueue, String programFolder, Date currentDate, boolean mirrorLogsToConsole) {
        this.loggingQueue = loggingQueue;
        this.programFolder = programFolder;
        this.currentDate = currentDate;
        this.mirrorLogsToConsole = mirrorLogsToConsole;
    }

    @Override
    public void run() {
        Logger.getInstance().saveImportantLogMessage(new Date(), "Logger", "Logger initialized");
        while (!Thread.currentThread().isInterrupted()) {
            try (FileWriter writer = new FileWriter(programFolder + File.separator + "log@" + new SimpleDateFormat("dd_MM_yyy").format(currentDate) + ".txt", true)) {
                String s = loggingQueue.take().toString();
                if (mirrorLogsToConsole) {
                    System.out.println(s);
                }
                writer.write(s);
                writer.write("\n");
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                } else {
                    e.printStackTrace();
                    break;
                }
            }
        }
        Terminate();
    }
    public synchronized void Terminate(){
        if (!this.isInterrupted()) {
            this.interrupt();
        }
        try (FileWriter writer = new FileWriter(programFolder + File.separator + "log@" + new SimpleDateFormat("dd_MM_yyy").format(currentDate) + ".txt", true)) {
            writer.write("Logging Stopped");
            writer.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
