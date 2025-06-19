package com.ars.gateway.config;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.Objects;

/**
 * Used in the logging system (Logback) to ensure that log messages do not contain newline characters
 * (CR - Carriage Return and LF - Line Feed)
 * or other unsafe characters that may cause security problems or affect the log format
 * @author thoaidc
 */
public class CRLFLogConverter extends ClassicConverter {

    public static final Marker CRLF_SAFE_MARKER = MarkerFactory.getMarker("CRLF_SAFE");
    private static final String[] SAFE_LOGGERS = { "org.hibernate" };

    public CRLFLogConverter() {}

    @Override
    public String convert(ILoggingEvent event) {
        String logMessage = event.getFormattedMessage();
        Marker marker = Objects.nonNull(event.getMarkerList()) ? event.getMarkerList().get(0) : null;

        // If the logger is safe (in SAFE_LOGGERS list) or has the marker CRLF_SAFE_MARKER, keep original log string
        if ((Objects.nonNull(marker) && marker.contains(CRLF_SAFE_MARKER)) || isLoggerSafe(event)) {
            return logMessage;
        }

        return logMessage.replaceAll("[\n\r\t]", "_");
    }

    protected boolean isLoggerSafe(ILoggingEvent event) {
        for (String safeLogger : SAFE_LOGGERS) {
            if (event.getLoggerName().startsWith(safeLogger)) {
                return true;
            }
        }

        return false;
    }
}
