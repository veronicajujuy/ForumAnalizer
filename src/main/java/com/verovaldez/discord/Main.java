package com.verovaldez.discord;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {

        Dotenv dotenv = Dotenv.load();
        String token = dotenv.get("DISCORD_TOKEN");
        String forumChannelId = dotenv.get("FORUM_CHANNEL_ID");

        JDA jda = JDABuilder.createDefault(
                token,
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.MESSAGE_CONTENT
        ).build();

        try {
            jda.awaitReady();
            System.out.println("Conectado como " + jda.getSelfUser().getAsTag());
            // Listar servidores donde está el bot
            jda.getGuilds().forEach(guild ->
                    System.out.println("📡 Servidor: " + guild.getName() + " | ID: " + guild.getId())
            );
            ForumExporter exporter = new ForumExporter(jda);
            exporter.exportForumToCSV(forumChannelId,
                    "out/threads.csv",
                    "out/messages.csv"
            );
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (jda != null) {
                // Cerrar JDA para que el proceso JVM termine cuando ya no se necesite
                jda.shutdown();
            }
        }


    }
}