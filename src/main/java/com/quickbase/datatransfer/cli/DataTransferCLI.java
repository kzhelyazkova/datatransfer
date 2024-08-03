package com.quickbase.datatransfer.cli;

import com.quickbase.datatransfer.cli.exceptionresolving.CustomCommandExceptionResolver;
import com.quickbase.datatransfer.cli.util.CommandUtils;
import com.quickbase.datatransfer.common.DataType;
import com.quickbase.datatransfer.service.DataTransferService;
import org.jline.terminal.Terminal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.springframework.shell.command.CommandRegistration.*;

@Component
@Command(command = "transfer")
public class DataTransferCLI extends CustomCommandExceptionResolver {
    private final Terminal terminal;
    private final DataTransferService dataTransferService;

    @Autowired
    public DataTransferCLI(Terminal terminal, DataTransferService dataTransferService) {
        this.terminal = terminal;
        this.dataTransferService = dataTransferService;
    }

    @Async
    @Command(command = "user")
    public void transferUser(
            @Option(longNames = {"source-system"}, required = true, arity = OptionArity.EXACTLY_ONE)
            String sourceSystem,
            @Option(longNames = {"destination-system"}, required = true, arity = OptionArity.EXACTLY_ONE)
            String destinationSystem,
            @Option(longNames = {"source-params"}, arity = OptionArity.ONE_OR_MORE)
            String[] sourceParams,
            @Option(longNames = {"destination-params"}, arity = OptionArity.ONE_OR_MORE)
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
