package com.quickbase.datatransfer.cli.exceptionresolving;

import com.quickbase.datatransfer.exception.AmbiguousDataException;
import com.quickbase.datatransfer.exception.HttpRequestFailedException;
import com.quickbase.datatransfer.exception.InvalidDataException;
import com.quickbase.datatransfer.exception.InvalidParamException;
import com.quickbase.datatransfer.exception.MissingExternalSystemParamException;
import com.quickbase.datatransfer.exception.UnauthorizedOperationException;
import com.quickbase.datatransfer.exception.UnsupportedOperationException;
import org.springframework.shell.command.CommandHandlingResult;
import org.springframework.shell.command.annotation.ExceptionResolver;

public class CustomCommandExceptionResolver {
    @ExceptionResolver()
    CommandHandlingResult errorHandler(InvalidParamException ex) {
        return CommandHandlingResult.of(String.format(
                "Invalid parameter '%s'. %s\n", ex.param, ex.getMessage()
        ), 1);
    }

    @ExceptionResolver()
    CommandHandlingResult errorHandler(InvalidDataException ex) {
        return CommandHandlingResult.of(String.format(
                "There is something wrong with the data for transfer. %s\n", ex.getMessage()
        ), 1);
    }

    @ExceptionResolver()
    CommandHandlingResult errorHandler(UnauthorizedOperationException ex) {
        return CommandHandlingResult.of(String.format(
                "Unauthorized operation against '%s'. %s\n", ex.externalSystemName, ex.getMessage()
        ), 1);
    }

    @ExceptionResolver()
    CommandHandlingResult errorHandler(HttpRequestFailedException ex) {
        return CommandHandlingResult.of(String.format(
                "Request to '%s' failed with HTTP status code '%s'. %s\n",
                ex.externalSystemName, ex.httpStatusCode, ex.getMessage()
        ), 1);
    }

    @ExceptionResolver()
    CommandHandlingResult errorHandler(AmbiguousDataException ex) {
        return CommandHandlingResult.of(String.format(
                "The operation in '%s' cannot proceed due to ambiguity in the data. %s\n",
                ex.externalSystemName, ex.getMessage()
        ), 1);
    }

    @ExceptionResolver()
    CommandHandlingResult errorHandler(MissingExternalSystemParamException ex) {
        return CommandHandlingResult.of(String.format(
                "Missing %s parameter '%s'. %s\n", ex.externalSystemName, ex.param, ex.getMessage()
        ), 1);
    }

    @ExceptionResolver()
    CommandHandlingResult errorHandler(UnsupportedOperationException ex) {
        return CommandHandlingResult.of(String.format(
                "Operation is not supported. %s\n", ex.getMessage()
        ), 1);
    }

    @ExceptionResolver()
    CommandHandlingResult errorHandler(RuntimeException ex) {
        return CommandHandlingResult.of(String.format(
                "Unexpected error occurred. %s\n", ex.getMessage()
        ), 1);
    }
}
