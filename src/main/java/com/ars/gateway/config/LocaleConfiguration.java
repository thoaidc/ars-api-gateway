package com.ars.gateway.config;

import com.dct.model.common.MessageTranslationUtils;
import com.dct.model.config.properties.I18nProps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.i18n.LocaleContextResolver;

import java.util.Locale;
import java.util.Optional;

@Configuration
@EnableConfigurationProperties(I18nProps.class)
public class LocaleConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LocaleConfiguration.class);
    private static final String ENTITY_NAME = "LocaleConfiguration";
    private final I18nProps i18nProps;

    public LocaleConfiguration(I18nProps i18nProps) {
        this.i18nProps = i18nProps;
    }

    @Bean
    public LocaleContextResolver localeContextResolver() {
        log.debug("[{}] - Auto configure AcceptHeaderLocaleContextResolver as default local resolver", ENTITY_NAME);
        return new AcceptHeaderLocaleContextResolver();
    }

    @Bean
    @ConditionalOnMissingBean(MessageSource.class)
    public MessageSource messageSource() {
        log.debug("[{}] - Auto configure default MessageSource", ENTITY_NAME);
        // Provides a mechanism to load notifications from .properties files to support i18n
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        // Set the location of the message files
        // Spring will look for files by name messages_{locale}.properties
        messageSource.setBasenames(Optional.ofNullable(i18nProps).orElse(new I18nProps()).getBaseNames());
        messageSource.setDefaultEncoding(Optional.ofNullable(i18nProps).orElse(new I18nProps()).getEncoding());
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setDefaultLocale(Locale.ENGLISH);
        return messageSource;
    }

    @Bean
    @ConditionalOnMissingBean(MessageTranslationUtils.class)
    public MessageTranslationUtils messageTranslationUtils(MessageSource messageSource) {
        log.debug("[{}] - Auto configure default message translation utils", ENTITY_NAME);
        return new MessageTranslationUtils(messageSource);
    }
}
