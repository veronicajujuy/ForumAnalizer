package com.verovaldez.discord.mavenProyect;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ForumExporter {
    private final JDA jda;

    public ForumExporter(JDA jda) {
        this.jda = jda;
    }

    public void exportForumToCSV(String forumChannelId, String threadsCSVPath, String messagesCSVPath ) throws IOException, InterruptedException {
        Files.createDirectories(Path.of("out"));

        CsvWriter threadsCsv = new CsvWriter(threadsCSVPath,
                List.of("thread_id", "thread_name", "author_id", "author_name", "created_at","archived"));
        CsvWriter messagesCsv = new CsvWriter(messagesCSVPath,
                List.of("thread_id", "message_id", "author_id", "author_name", "timestamp", "content"));

        ForumChannel forum = jda.getForumChannelById(forumChannelId);
        if (forum == null) {
            throw new IllegalArgumentException("No se encontró el canal de foro con ID: " + forumChannelId);
        }

        // threads activos
        List<ThreadChannel> activos = forum.getThreadChannels();
        // theadas archivados publicos
        List<ThreadChannel> archivados = forum.retrieveArchivedPublicThreadChannels().complete();

        Map<String, ThreadChannel> allThreads = new LinkedHashMap<>();
        activos.forEach(t -> allThreads.put(t.getId(), t));
        archivados.forEach(t -> allThreads.put(t.getId(), t));

        System.out.println("Threads encontrados: "+ allThreads.size());

        for (ThreadChannel th: allThreads.values()){
            threadsCsv.writeRow(List.of(
                    th.getId(),
                    th.getName(),
                    th.getOwnerId() != null? th.getOwnerId() : "",
                    th.getTimeCreated() !=null ? th.getTimeCreated().toString() : "",
                    String.valueOf(th.isArchived())
            ));
            List<Message> messages = fetchAllMessages(th);
            System.out.printf("   • %s → %d mensajes%n", th.getName(), messages.size());

            for (Message m : messages) {
                messagesCsv.writeRow(List.of(
                        th.getId(),
                        m.getId(),
                        m.getAuthor() != null ? m.getAuthor().getId() : "",
                        m.getTimeCreated().toString(),
                        Util.escapeCsv(m.getContentRaw())
                ));
            }

            Thread.sleep(300); // rate limit friendly
        }

        threadsCsv.close();
        messagesCsv.close();
    }

    private List<Message> fetchAllMessages(MessageChannel channel) {
        List<Message> all = new ArrayList<>();
        String before = null;

        while (true) {
            MessageHistory.MessageRetrieveAction action =
                    (before == null)
                            ? MessageHistory.getHistoryFromBeginning(channel)
                            : MessageHistory.getHistoryBefore(channel, before);

            List<Message> batch = action.limit(100).complete().getRetrievedHistory();
            if (batch.isEmpty()) break;

            all.addAll(batch);
            before = batch.get(batch.size() - 1).getId();
        }
        return all;
    }
}
