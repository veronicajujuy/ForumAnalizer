package com.verovaldez.discord.config;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DiscordConfig {

    @Bean
    public Dotenv dotenv(){
        return Dotenv.load();
    }

    @Bean
    public JDA jda(Dotenv dotenv) throws Exception {
        String token = dotenv.get("DISCORD_TOKEN");
        JDA jda = JDABuilder.createDefault(
                token,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.MESSAGE_CONTENT
        ).build();
        jda.awaitReady();
        return jda;
    }
}
