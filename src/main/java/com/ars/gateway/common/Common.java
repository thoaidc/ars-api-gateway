package com.ars.gateway.common;

import org.apache.commons.lang.StringUtils;

import java.util.Objects;

public class Common {

    public static String normalizePath(String path) {
        path = StringUtils.trimToNull(path);

        if (Objects.nonNull(path)) {
            // Remove any '/' characters at the beginning and end of the path
            path = path.replaceAll("^/+", "").replaceAll("/+$", "");
            return StringUtils.trimToNull(path);
        }

        return null;
    }
}
