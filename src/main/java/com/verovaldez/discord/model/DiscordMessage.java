package com.verovaldez.discord.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "discord_messages")
@Getter @Setter
public class DiscordMessage {
    @Id
    private String id;
    private String guildId;
    private String guildName;
    private String forumId; // id del foro
    private String forumName; // nombre del foro
    private String threadId; // id del hilo
    private String threadName; // nombre del hilo
    private String authorId;
    private String authorName;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private TypeChannel typeChannel;

    private OffsetDateTime createdAt;
}
