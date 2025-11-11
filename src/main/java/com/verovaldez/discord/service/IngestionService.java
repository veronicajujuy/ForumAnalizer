package com.verovaldez.discord.service;

import com.verovaldez.discord.model.DiscordMessage;
import com.verovaldez.discord.model.TypeChannel;
import com.verovaldez.discord.repository.DiscordMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {
    private final JDA jda;
    private final DiscordMessageRepository repository;

    // Configuración de rate limiting más conservadora
    private static final int BATCH_SIZE = 50; // Reducido de 100 a 50
    private static final long BASE_DELAY_MS = 500; // Aumentado de 250 a 500ms
    private static final long MAX_RETRY_DELAY_MS = 8000; // 8 segundos máximo
    private static final int MAX_RETRIES = 3;

    public Map<String, Object> ingestForum(String forumId) {
        log.info("🚀 Iniciando ingesta del foro ID: {}", forumId);

        var channel = jda.getForumChannelById(forumId);

        if (channel instanceof ForumChannel forum) {
            return processForum(forum, forumId);
        } else {
            // Intentar como canal de texto
            var textChannel = jda.getTextChannelById(forumId);
            if (textChannel != null) {
                return processTextChannel(textChannel);
            } else {
                throw new IllegalArgumentException("El ID no corresponde a un canal de texto o foro válido.");
            }
        }
    }

    private Map<String, Object> processForum(ForumChannel forum, String forumId) {
        log.info("chequeando que llega por el foro '{} {}' nombre '{}' canal {} ", forum, forumId, forum.getName(), forum.getGuild().getName());
        List<ThreadChannel> activos = forum.getThreadChannels();
        List<ThreadChannel> archivados = forum.retrieveArchivedPublicThreadChannels().complete();

        Map<String, ThreadChannel> allThreads = new LinkedHashMap<>();
        activos.forEach(t -> allThreads.put(t.getId(), t));
        archivados.forEach(t -> allThreads.put(t.getId(), t));

        log.info("📊 Threads encontrados: {} activos, {} archivados, {} total",
                activos.size(), archivados.size(), allThreads.size());
        log.info("📋 Foro encontrado: '{}' en servidor: {}", forum.getName(), forum.getGuild().getName());

        int saved = 0;
        int threadCount = 0;
        List<DiscordMessage> batch = new ArrayList<>();

        for(ThreadChannel th: allThreads.values()){
            threadCount++;
            log.info("🔄 Procesando thread {}/{}: '{}' (ID: {})",
                    threadCount, allThreads.size(), th.getName(), th.getId());

            List<Message> msgs = fetchAllMessagesWithRetry(th);
            log.info("📨 Obtenidos {} mensajes del thread '{}'", msgs.size(), th.getName());

            for(Message m: msgs){
                DiscordMessage dm = new DiscordMessage();
                dm.setId(m.getId());
                dm.setGuildId(m.getGuild().getId());
                dm.setGuildName(forum.getGuild().getName());
                dm.setForumId(forum.getId());
                dm.setForumName(forum.getName());
                dm.setThreadId(th.getId());
                dm.setThreadName(th.getName());
                dm.setAuthorId(m.getAuthor().getId());
                dm.setAuthorName(m.getAuthor().getName());
                dm.setContent(m.getContentRaw());
                dm.setTypeChannel(TypeChannel.FORUM);
                dm.setCreatedAt(m.getTimeCreated());

                batch.add(dm);
                saved++;

                // Guardar en lotes para mejor rendimiento
                if (batch.size() >= 100) {
                    repository.saveAll(batch);
                    log.debug("💾 Guardado lote de {} mensajes", batch.size());
                    batch.clear();
                }
            }

            log.info("✅ Thread '{}' completado. Mensajes procesados hasta ahora: {}", th.getName(), saved);
            smartSleep(BASE_DELAY_MS);
        }

        // Guardar mensajes restantes
        if (!batch.isEmpty()) {
            repository.saveAll(batch);
            log.debug("💾 Guardado lote final de {} mensajes", batch.size());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("threads_processed", allThreads.size());
        result.put("messages_saved", saved);

        log.info("🎉 Ingesta completada. Threads: {}, Mensajes: {}", allThreads.size(), saved);
        return result;
    }

    private Map<String, Object> processTextChannel(TextChannel channel) {
        log.info("📋 Canal de texto encontrado: '{}' en servidor: {}", channel.getName(), channel.getGuild().getName());

        List<Message> messages = fetchAllMessagesFromTextChannel(channel);
        List<DiscordMessage> batch = new ArrayList<>();
        int saved = 0;

        for (Message m : messages) {
            DiscordMessage dm = getDiscordMessage(channel, m);

            batch.add(dm);
            saved++;

            // Guardar en lotes
            if (batch.size() >= 100) {
                repository.saveAll(batch);
                log.debug("💾 Guardado lote de {} mensajes", batch.size());
                batch.clear();
            }
        }

        // Guardar mensajes restantes
        if (!batch.isEmpty()) {
            repository.saveAll(batch);
            log.debug("💾 Guardado lote final de {} mensajes", batch.size());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("messages_saved", saved);
        result.put("channel_name", channel.getName());

        log.info("🎉 Ingesta de canal completada. Mensajes: {}", saved);
        return result;
    }

    @NotNull
    private static DiscordMessage getDiscordMessage(TextChannel channel, Message m) {
        DiscordMessage dm = new DiscordMessage();
        dm.setId(m.getId());
        dm.setGuildId(channel.getGuild().getId());
        dm.setGuildName(channel.getGuild().getName());
        dm.setForumId(channel.getId());      // el "forumId" se usa genéricamente como "channelId"
        dm.setForumName(channel.getName());
        dm.setThreadId(null);
        dm.setThreadName(channel.getName());
        dm.setAuthorId(m.getAuthor().getId());
        dm.setAuthorName(m.getAuthor().getName());
        dm.setContent(m.getContentRaw());
        dm.setTypeChannel(TypeChannel.TEXT);
        dm.setCreatedAt(m.getTimeCreated());
        return dm;
    }

    private List<Message> fetchAllMessagesWithRetry(ThreadChannel thread) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return fetchAllMessages(thread);
            } catch (Exception e) {
                log.error("❌ Error en intento {}/{} para thread '{}': {}",
                        attempt, MAX_RETRIES, thread.getName(), e.getMessage());
                if (attempt == MAX_RETRIES) {
                    log.error("💥 Falló después de {} intentos. Saltando thread '{}'",
                            MAX_RETRIES, thread.getName());
                    return new ArrayList<>();
                }
                smartSleep(1000 * attempt); // Backoff exponencial
            }
        }
        return new ArrayList<>();
    }

    private List<Message> fetchAllMessages(ThreadChannel thread) {
        List<Message> allMessages = new ArrayList<>();
        String before = null;
        int pageCount = 0;

        log.debug("📥 Iniciando fetch de mensajes para thread: {}", thread.getName());

        while (true) {
            pageCount++;
            var action = (before == null)
                    ? MessageHistory.getHistoryFromBeginning(thread)
                    : MessageHistory.getHistoryBefore(thread, before);

            var batch = action.limit(BATCH_SIZE).complete();
            List<Message> messages = batch.getRetrievedHistory();

            if (messages.isEmpty()) {
                log.info("📭 No hay más mensajes. Páginas procesadas: {}", pageCount);
                break;
            }

            allMessages.addAll(messages);
            before = messages.getLast().getId();

            log.info("📄 Página {}: {} mensajes (total: {})", pageCount, messages.size(), allMessages.size());

            // Pausa entre páginas para evitar rate limits
            if (pageCount % 5 == 0) { // Cada 5 páginas, pausa extra
                smartSleep(BASE_DELAY_MS * 2);
            } else {
                smartSleep(BASE_DELAY_MS / 2); // Pausa corta entre páginas
            }
        }

        return allMessages;
    }

    private List<Message> fetchAllMessagesFromTextChannel(TextChannel channel) {
        List<Message> allMessages = new ArrayList<>();
        String before = null;
        int pageCount = 0;

        log.debug("📥 Iniciando fetch de mensajes para canal: {}", channel.getName());

        while (true) {
            pageCount++;
            var action = (before == null)
                    ? MessageHistory.getHistoryFromBeginning(channel)
                    : MessageHistory.getHistoryBefore(channel, before);

            var batch = action.limit(BATCH_SIZE).complete();
            List<Message> messages = batch.getRetrievedHistory();

            if (messages.isEmpty()) {
                log.info("📭 No hay más mensajes. Páginas procesadas: {}", pageCount);
                break;
            }

            allMessages.addAll(messages);
            before = messages.getLast().getId();

            log.info("📄 Página {}: {} mensajes (total: {})", pageCount, messages.size(), allMessages.size());

            // Pausa entre páginas para evitar rate limits
            if (pageCount % 5 == 0) { // Cada 5 páginas, pausa extra
                smartSleep(BASE_DELAY_MS * 2);
            } else {
                smartSleep(BASE_DELAY_MS / 2); // Pausa corta entre páginas
            }
        }

        return allMessages;
    }

    private void smartSleep(long ms) {
        if (ms <= 0) return;

        // Limitar el tiempo máximo de espera
        long actualDelay = Math.min(ms, MAX_RETRY_DELAY_MS);

        try {
            log.debug("💤 Pausa de {} ms para rate limiting", actualDelay);
            Thread.sleep(actualDelay);
        } catch (InterruptedException e) {
            log.warn("⚠️ Sleep interrumpido: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    public List<DiscordMessage> allMessages(){
        return repository.findAll();
    }
}
