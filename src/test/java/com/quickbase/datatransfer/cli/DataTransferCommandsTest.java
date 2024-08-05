package com.quickbase.datatransfer.cli;

import com.quickbase.datatransfer.common.DataType;
import com.quickbase.datatransfer.exception.InvalidParamException;
import com.quickbase.datatransfer.service.DataTransferService;
import org.jline.terminal.Terminal;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Mono;

import java.io.PrintWriter;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = DataTransferCommands.class)
public class DataTransferCommandsTest {
    @Autowired
    private DataTransferCommands dataTransferCommands;

    @MockBean
    private DataTransferService dataTransferService;

    @MockBean
    private Terminal terminal;

    private static final String sourceSystem = "source-system";
    private static final String destSystem = "dest-system";
    private static final String[] sourceParams = {"key1=val1"};
    private static final String[] destParams = {"key2=val2"};

    @Test
    public void testTransferUser_success() {
        when(dataTransferService.transferData(any(), any(), any(), any(), any()))
                .thenReturn(Mono.empty());
        PrintWriter writer = mock(PrintWriter.class);
        when(terminal.writer()).thenReturn(writer);

        dataTransferCommands.transferUser(sourceSystem, destSystem, sourceParams, destParams);

        verify(dataTransferService, times(1))
                .transferData(eq(sourceSystem), eq(destSystem), eq(DataType.USER),
                        eq(Map.of("key1", "val1")), eq(Map.of("key2", "val2")));
        verify(writer, times(1))
                .printf(eq("Successfully completed user data transfer from %s to %s!\n"),
                        eq(sourceSystem), eq(destSystem));
    }

    @Test
    public void testTransferUser_invalidParam() {
        String[] invalidSourceParams = {"username"};

        InvalidParamException ex = assertThrows(InvalidParamException.class, () ->
                dataTransferCommands.transferUser(sourceSystem, destSystem, invalidSourceParams, destParams));

        assertEquals(invalidSourceParams[0], ex.param);
        assertEquals(String.format("Parameter '%s' does not match the pattern '%s'.", invalidSourceParams[0], "<key>=<value>"),
                ex.getMessage());
    }

    @Test
    public void testTransferUser_transferFailure() {
        when(dataTransferService.transferData(any(), any(), any(), any(), any()))
                .thenReturn(Mono.error(new RuntimeException("something failed")));
        PrintWriter writer = mock(PrintWriter.class);
        when(terminal.writer()).thenReturn(writer);

        assertThrows(RuntimeException.class, () -> dataTransferCommands.transferUser(sourceSystem, destSystem, sourceParams, destParams));

        verify(dataTransferService, times(1))
                .transferData(eq(sourceSystem), eq(destSystem), eq(DataType.USER),
                        eq(Map.of("key1", "val1")), eq(Map.of("key2", "val2")));
        verify(writer, times(1))
                .printf(eq("Failed to transfer user data from %s to %s!\n"),
                        eq(sourceSystem), eq(destSystem));
    }
}
