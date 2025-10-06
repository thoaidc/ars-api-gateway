package com.ars.gateway.common;

import com.ars.gateway.constants.CommonConstants;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.server.ServerWebExchange;

import java.util.Locale;
import java.util.Optional;

public class LocaleUtils {
    public static void setLocale(ServerWebExchange exchange) {
        Locale locale = exchange.getLocaleContext().getLocale();
        locale = Optional.ofNullable(locale).orElse(Locale.forLanguageTag(CommonConstants.VI));
        LocaleContextHolder.setLocale(locale);
    }
}
