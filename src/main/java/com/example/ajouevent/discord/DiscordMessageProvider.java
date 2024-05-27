package com.example.ajouevent.discord;

import static com.example.ajouevent.discord.DiscordMessage.*;

import org.springframework.stereotype.Component;

import feign.FeignException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class DiscordMessageProvider {
	private final DiscordFeignClient discordFeignClient;

	public void sendMessage(String eventMessage) {
		DiscordMessage discordMessage = createDiscordMessage(eventMessage);
		sendMessageToDiscord(discordMessage);
	}

	private void sendMessageToDiscord(DiscordMessage discordMessage) {
		try {
			discordFeignClient.sendMessage(discordMessage);
		} catch (FeignException e) {
			// throw new InvalidValueException(ErrorMessage.INVALID_DISCORD_MESSAGE);
			throw new IllegalArgumentException();
		}
	}
}