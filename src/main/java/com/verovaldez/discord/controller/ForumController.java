package com.verovaldez.discord.controller;

import com.verovaldez.discord.service.AnaliticsService;
import com.verovaldez.discord.service.IngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ForumController {
    private final IngestionService ingestionService;
    private final AnaliticsService analiticsService;

    // Ingestar foro a DB
    @PostMapping("/ingest/forum/{forumId}")
    public Map<String,Object> ingest(@PathVariable String forumId) {
        return ingestionService.ingestForum(forumId);
    }

    // participantes unicos
    @GetMapping("/forums/{forumId}/participants")
    public Map<String,Object> participants(
            @PathVariable String forumId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        return analiticsService.participants(forumId, from, to);
    }
    // cadencia (mediana minutos entre mensajes)
    @GetMapping("/forums/{forumId}/cadence")
    public Map<String,Object> cadence(
            @PathVariable String forumId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        return analiticsService.cadence(forumId, from, to);
    }

    // Actividad por hora local
    @GetMapping("/forums/{forumId}/hourly")
    public Map<String,Object> hourly(
            @PathVariable String forumId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        return analiticsService.hourly(forumId, from, to);
    }

    // FAQS
    @GetMapping("/forums/{forumId}/faqs")
    public Map<String,Object> faqs(
            @PathVariable String forumId,
            @RequestParam(defaultValue = "20") int top,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        return analiticsService.faqs(forumId, from, to, top);
    }

}
