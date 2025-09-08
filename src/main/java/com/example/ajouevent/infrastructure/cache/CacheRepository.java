package com.example.ajouevent.infrastructure.cache;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.type.TypeReference;

public interface CacheRepository {

	<T> Optional<T> get(String key, TypeReference<T> typeRef);

	void set(String key, Object value, long timeout, TimeUnit timeUnit);

	void delete(String key);
}