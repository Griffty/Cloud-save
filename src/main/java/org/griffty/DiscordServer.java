package org.griffty;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.FileUpload;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

public class DiscordServer {
    private static DiscordServer instance;
    public static DiscordServer getInstance() {
        if (instance == null){
            instance = new DiscordServer();
        }

        return instance;
    }
    private final JDA api;
    private final Guild saveGuild;
    private final TextChannel saveChannel;
    private DiscordServer(){
        Logger.getInstance().saveLogMessage(new Date(), "DiscordServer", "Initializing JDA instance");
        api = JDABuilder.createDefault(Info.DISCORD_KEY).build();
        try {
            api.awaitReady();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        saveGuild = api.getGuildById(Info.DISCORD_SAVE_SERVER_ID);
        if (saveGuild == null){
            throw new RuntimeException("Cannot get Save Guild from provided ID");
        }
        saveChannel = saveGuild.getTextChannelsByName("file_save", true).get(0);

        Logger.getInstance().saveLogMessage(new Date(), "DiscordServer", "Save guild: " + saveGuild.getName() + " " + saveGuild.getId() +
                "; Save channel: " + saveChannel.getName() + " " + saveChannel.getId());
        Logger.getInstance().saveLogMessage(new Date(), "DiscordServer", "JDA finished initializing");
    }
    public CompletableFuture<Void> uploadFile(byte[] file, String name){
        Logger.getInstance().saveLogMessage("DiscordServer", "Uploading file " + name + "; size: " + file.length);
        CompletableFuture<Void> future = new CompletableFuture<>();
        saveChannel.sendFiles(FileUpload.fromData(file, name)).queue(
                (message) -> {
                    Logger.getInstance().saveLogMessage("DiscordServer", "Finished uploading " + name + "; size: " + file.length);
                    future.complete(null);
                }
        );
        return future;
    }

    public void terminate() {
        Logger.getInstance().saveLogMessage("DiscordServer", "Shutting down JDA ");
        api.shutdown();
    }
}
