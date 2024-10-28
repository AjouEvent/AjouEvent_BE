package com.example.ajouevent.config;

import java.time.Duration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class RedisCacheConfig {

	@Bean
	public RedisCacheConfiguration redisCacheConfiguration(){
		return RedisCacheConfiguration.defaultCacheConfig()
			.computePrefixWith(name -> name + ":")
			.entryTtl(Duration.ofSeconds(300))
			.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()));
	}

}