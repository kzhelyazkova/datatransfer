package com.quickbase.datatransfer.cli.util;

import com.quickbase.datatransfer.exception.InvalidParamException;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CommandUtils {
    private static final Pattern paramPattern = Pattern.compile("([\\w-]+)=([\\w-]+)");

    public static Map<String, String> convertArrayParamsToMap(String[] params) {
        if (params == null || params.length == 0) {
            return Collections.emptyMap();
        }

        return Arrays.stream(params)
                .peek(param -> {
                    if (!paramPattern.matcher(param).matches()) {
                        throw new InvalidParamException(
                                String.format("Parameter '%s' does not match the pattern '%s'.", param, "<key>=<value>"),
                                param);
                    }
                })
                .collect(Collectors.toMap(
                        param -> param.split("=")[0],
                        param -> param.split("=")[1]
                ));
    }
}
