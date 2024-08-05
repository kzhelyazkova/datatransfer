package com.quickbase.datatransfer.cli;

import com.quickbase.datatransfer.cli.exceptionresolving.CustomCommandExceptionResolver;
import com.quickbase.datatransfer.cli.util.CommandUtils;
import com.quickbase.datatransfer.common.DataType;
import com.quickbase.datatransfer.service.DataTransferService;
import jakarta.validation.constraints.NotBlank;
import org.jline.terminal.Terminal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.springframework.shell.command.CommandRegistration.*;

@Component
@Command(command = "transfer", group = "Data Transfer")
public class DataTransferCLI extends CustomCommandExceptionResolver {
    private final Terminal terminal;
    private final DataTransferService dataTransferService;

    @Autowired
    public DataTransferCLI(Terminal terminal, DataTransferService dataTransferService) {
        this.terminal = terminal;
        this.dataTransferService = dataTransferService;
    }

    @Async
    @Command(command = "user", description = "Transfer data for a specified user from one external system to another")
    public void transferUser(
            @NotBlank
            @Option(longNames = {"source-system"}, shortNames = {'s'}, required = true, arity = OptionArity.EXACTLY_ONE,
                    description = "External system from which data will be retrieved. Currently supported: GitHub")
            String sourceSystem,
            @NotBlank
            @Option(longNames = {"destination-system"}, shortNames = {'d'}, required = true, arity = OptionArity.EXACTLY_ONE,
                    description = "External system to which data will be transferred. Currently supported: Freshdesk")
            String destinationSystem,
            @Option(longNames = {"source-params"}, shortNames = {'p'}, arity = OptionArity.ONE_OR_MORE,
                    description = "Parameters in a <key>=<value> format identifying the user for which data will be " +
                            "retrieved from the source system. For example, GitHub requires username")
            String[] sourceParams,
            @Option(longNames = {"destination-params"}, shortNames = {'t'}, arity = OptionArity.ONE_OR_MORE,
                    description = "Parameters in a <key>=<value> format identifying where to upload the user data in " +
                            "the destination system. For example, Freshdesk requires domain")
            String[] destinationParams) {
        Map<String, String> sourceParamsMap = CommandUtils.convertArrayParamsToMap(sourceParams);
        Map<String, String> destParamsMap = CommandUtils.convertArrayParamsToMap(destinationParams);

        dataTransferService.transferData(sourceSystem, destinationSystem, DataType.USER, sourceParamsMap, destParamsMap)
                .doOnSuccess(__ -> {
                    terminal.writer().printf("Successfully completed user data transfer from %s to %s!\n",
                            sourceSystem, destinationSystem);
                    terminal.writer().flush();
                })
                .doOnError(__ -> {
                    terminal.writer().printf("Failed to transfer user data from %s to %s!\n",
                            sourceSystem, destinationSystem);
                    terminal.writer().flush();
                })
                .block();
    }
}
