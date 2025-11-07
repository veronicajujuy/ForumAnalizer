package com.verovaldez.discord.repository;

import com.verovaldez.discord.model.DiscordMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface DiscordMessageRepository extends JpaRepository<DiscordMessage, String> {
    List<DiscordMessage> findByForumId(String forumId);
    List<DiscordMessage> findByForumIdAndCreatedAtBetween(String forumId, OffsetDateTime from, OffsetDateTime to);
}
