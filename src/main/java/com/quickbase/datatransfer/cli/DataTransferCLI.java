package com.quickbase.datatransfer.cli;

import com.quickbase.datatransfer.service.DataTransferService;
import org.jline.terminal.Terminal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.shell.command.annotation.OptionValues;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

//@ShellComponent
@Component
@Command(command = "transfer")
public class DataTransferCLI {

    private final AsyncService asyncService;
    private final Terminal terminal;
    private final DataTransferService dataTransferService;

    @Autowired
    public DataTransferCLI(AsyncService asyncService, Terminal terminal, DataTransferService dataTransferService) {
        this.asyncService = asyncService;
        this.terminal = terminal;
        this.dataTransferService = dataTransferService;
    }

    //@ShellMethod("Execute a task asynchronously")
    @Command(command = "user")
    public void asyncCommand(@Option(longNames = {"source-system"}, required = true) String sourceSystem,
                             @Option(longNames = {"destination-system"}, required = true) String destinationSystem,
                             @Option(longNames = {"source-params"}) String[] sourceParams,
                             @Option(longNames = {"destination-params"}) String[] destinationParams) {
        terminal.writer().printf("source system is %s, destination system is %s\n", sourceSystem, destinationSystem);
        terminal.writer().printf("source params are %s, destination params are %s\n", Arrays.toString(sourceParams), Arrays.toString(destinationParams));
        Map<String, String> sourceParamsMap = DataTransferCLI.convertArrayParamsToMap(sourceParams);
        Map<String, String> destParamsMap = DataTransferCLI.convertArrayParamsToMap(destinationParams);
        terminal.writer().printf("source params are %s, destination params are %s\n",
                sourceParamsMap.keySet().stream()
                .map(key -> key + "=" + sourceParamsMap.get(key))
                .collect(Collectors.joining(", ", "{", "}")),
                destParamsMap.keySet().stream()
                        .map(key -> key + "=" + destParamsMap.get(key))
                        .collect(Collectors.joining(", ", "{", "}")));
        terminal.writer().flush();

        dataTransferService.transferData(sourceSystem, destinationSystem, "user", sourceParamsMap, destParamsMap);

        /*asyncService.performAsyncTask()
                .subscribe(result -> {
                    terminal.writer().println(result);
                    //System.out.println(result);
                });*/
    }

    private static Map<String, String> convertArrayParamsToMap(String[] params) {
        return Arrays.stream(params).collect(Collectors.toMap(
                param -> param.split("=")[0],
                param -> param.split("=")[1]
        ));
    }


    /*@Async
    @Command(command = "user")
    public Mono<String> transferUserData()  {
        return Mono.fromCallable(() -> {
            Thread.sleep(5000);
            return "User data successfully transferred.";
        });
    }*/
}
