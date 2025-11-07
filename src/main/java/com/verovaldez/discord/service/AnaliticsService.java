package com.verovaldez.discord.service;

import com.verovaldez.discord.model.DiscordMessage;
import com.verovaldez.discord.repository.DiscordMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnaliticsService {
    private final DiscordMessageRepository repository;
    private ZoneId zoneId;

    @Value("${app.discord.tz:America/Argentina/Buenos_Aires}")
    public void setZoneId(String tz) {
        zoneId = ZoneId.of(tz);
    }

    // --- Participantes únicos ---
    public Map<String, Object> participants(String forumId, OffsetDateTime from, OffsetDateTime to) {
        List<DiscordMessage> list = load(forumId, from, to);
        long unique = list.stream().map(DiscordMessage::getAuthorId).filter(Objects::nonNull).distinct().count();
        return Map.of("forumId", forumId, "participants", unique, "messages", list.size());
    }
    // --- Cadencia (mediana minutos entre mensajes del foro) ---
    public Map<String, Object> cadence(String forumId, OffsetDateTime from, OffsetDateTime to) {
        List<DiscordMessage> list = loadSorted(forumId, from, to);
        if (list.size() < 2) return Map.of("forumId", forumId, "median_gap_min", 0);
        List<Long> gaps = new ArrayList<>();
        for (int i=1;i<list.size();i++){
            var prev = list.get(i-1).getCreatedAt();
            var cur  = list.get(i).getCreatedAt();
            gaps.add(Duration.between(prev, cur).toMinutes());
        }
        Collections.sort(gaps);
        double median = (gaps.size()%2==1) ? gaps.get(gaps.size()/2)
                : (gaps.get(gaps.size()/2-1)+gaps.get(gaps.size()/2))/2.0;
        return Map.of("forumId", forumId, "median_gap_min", median, "samples", gaps.size());
    }

    // --- Actividad por hora local (bins de 0..23) ---
    public Map<String, Object> hourly(String forumId, OffsetDateTime from, OffsetDateTime to) {
        List<DiscordMessage> list = load(forumId, from, to);
        int[] byHour = new int[24];
        for (DiscordMessage m : list) {
            int h = m.getCreatedAt().atZoneSameInstant(zoneId).getHour();
            byHour[h]++;
        }
        Map<String,Integer> hist = new LinkedHashMap<>();
        for (int h=0;h<24;h++) hist.put(String.valueOf(h), byHour[h]);
        return Map.of("forumId", forumId, "hourly", hist, "messages", list.size(), "tz", zoneId.toString());
    }

    // --- FAQs: top preguntas (heurística simple) ---
    public Map<String, Object> faqs(String forumId, OffsetDateTime from, OffsetDateTime to, int topN) {
        List<DiscordMessage> list = load(forumId, from, to);

        // 1) filtrar preguntas: content con "?"
        List<String> questions = list.stream()
                .map(DiscordMessage::getContent)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> s.length()>=6 && s.contains("?"))   // heurística básica
                .map(this::normalize)
                .toList();

        // 2) contar frecuencia (texto exacto normalizado)
        Map<String, Long> freq = questions.stream()
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        // 3) top N
        List<Map<String,Object>> top = freq.entrySet().stream()
                .sorted((a,b)-> Long.compare(b.getValue(), a.getValue()))
                .limit(topN)
                .map(e -> Map.<String,Object>of("question", e.getKey(), "count", e.getValue()))
                .collect(Collectors.toList());

        return Map.of("forumId", forumId, "total_questions", questions.size(), "top", top);


    }

    // Helpers
    private List<DiscordMessage> load(String forumId, OffsetDateTime from, OffsetDateTime to){
        if (from!=null && to!=null) return repository.findByForumIdAndCreatedAtBetween(forumId, from, to);
        return repository.findByForumId(forumId);
    }
    private List<DiscordMessage> loadSorted(String forumId, OffsetDateTime from, OffsetDateTime to){
        List<DiscordMessage> l = load(forumId, from, to);
        l.sort(Comparator.comparing(DiscordMessage::getCreatedAt));
        return l;
    }
    private String normalize(String s) {
        String lower = s.toLowerCase(Locale.ROOT);
        String noAccents = Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}","");
        String clean = noAccents.replaceAll("[^a-z0-9¿?áéíóúüñ \\-:_./]", " ").replaceAll("\\s+"," ").trim();
        // opcional: recortar largo máximo
        return clean.length()>300 ? clean.substring(0,300) : clean;
    }


}
