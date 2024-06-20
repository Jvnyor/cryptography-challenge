package com.jvnyor.cryptographychallenge.config;

import com.jvnyor.cryptographychallenge.util.CacheConstants;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Locale;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String KEY_GENERATOR = "keyGenerator";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                CacheConstants.GET_TRANSACTION,
                CacheConstants.GET_TRANSACTIONS
        );
    }

    @Bean(KEY_GENERATOR)
    public KeyGenerator keyGenerator() {
        return (target, method, params) -> Arrays.toString(params).toUpperCase(Locale.ROOT);
    }
}