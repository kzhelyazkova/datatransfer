package com.quickbase.datatransfer.service;

import com.quickbase.datatransfer.common.DataType;
import com.quickbase.datatransfer.dto.BaseDTO;
import com.quickbase.datatransfer.exception.UnsupportedOperationException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = DataTransferServiceImpl.class)
public class DataTransferServiceTest {
    @Autowired
    private DataTransferService dataTransferService;

    @MockBean(name = "dataDownloader1")
    private DataDownloader<BaseDTO> dataDownloader1;

    @MockBean(name = "dataDownloader2")
    private DataDownloader<BaseDTO> dataDownloader2;

    @MockBean(name = "dataUploader1")
    private DataUploader<BaseDTO> dataUploader1;

    @MockBean(name = "dataUploader2")
    private DataUploader<BaseDTO> dataUploader2;

    private static final String sourceSystem = "source-system";
    private static final String destSystem = "dest-system";
    private static final DataType dataType = DataType.USER;
    private static final Map<String, String> sourceParams = Map.of("key1", "val1");
    private static final Map<String, String> destParams = Map.of("key2", "val2");

    @Test
    public void testTransferData_success() {
        // make dataDownloader1 a suitable match
        when(dataDownloader1.systemTypeMatches(eq(sourceSystem)))
                .thenReturn(true);
        when(dataDownloader1.dataTypeMatches(eq(dataType)))
                .thenReturn(true);

        // make dataUploader1 a suitable match
        when(dataUploader1.systemTypeMatches(eq(destSystem)))
                .thenReturn(true);
        when(dataUploader1.dataTypeMatches(eq(dataType)))
                .thenReturn(true);

        // make dataDownloader2 not a suitable match
        when(dataDownloader2.systemTypeMatches(eq(sourceSystem)))
                .thenReturn(false);

        // make dataUploader2 not a suitable match
        when(dataUploader2.systemTypeMatches(eq(destSystem)))
                .thenReturn(true);
        when(dataUploader2.dataTypeMatches(eq(dataType)))
                .thenReturn(false);

        // mock download and upload operations
        BaseDTO downloadedData = new BaseDTO();
        when(dataDownloader1.downloadData(eq(sourceParams)))
                .thenReturn(Mono.just(downloadedData));
        when(dataUploader1.uploadData(eq(destParams), any()))
                .thenReturn(Mono.empty());

        // verify the transfer is successful
        StepVerifier.create(dataTransferService.transferData(sourceSystem, destSystem, dataType, sourceParams, destParams))
                .verifyComplete();

        // verify the suitable downloader and uploader were used
        verify(dataDownloader1, times(1))
                .downloadData(eq(sourceParams));
        verify(dataUploader1, times(1))
                .uploadData(eq(destParams), any());
    }

    @Test
    public void testTransferData_unsupportedExternalSystem() {
        // make both data downloaders not a suitable match for the source system type
        when(dataDownloader1.systemTypeMatches(eq(sourceSystem)))
                .thenReturn(false);
        when(dataDownloader2.systemTypeMatches(eq(sourceSystem)))
                .thenReturn(false);

        StepVerifier.create(dataTransferService.transferData(sourceSystem, destSystem, dataType, sourceParams, destParams))
                .expectErrorMatches(throwable -> {
                    if (throwable instanceof UnsupportedOperationException ex) {
                        return String.format("Unsupported external system type '%s'", sourceSystem).equals(ex.getMessage());
                    }
                    return false;
                })
                .verify();
    }

    @Test
    public void testTransferData_unsupportedDataType() {
        // make dataDownloader1 a suitable match
        when(dataDownloader1.systemTypeMatches(eq(sourceSystem)))
                .thenReturn(true);
        when(dataDownloader1.dataTypeMatches(eq(dataType)))
                .thenReturn(true);

        // make both data uploaders not a suitable match for the data type
        when(dataUploader1.systemTypeMatches(eq(destSystem)))
                .thenReturn(true);
        when(dataUploader1.dataTypeMatches(eq(dataType)))
                .thenReturn(false);
        when(dataUploader2.systemTypeMatches(eq(destSystem)))
                .thenReturn(true);
        when(dataUploader2.dataTypeMatches(eq(dataType)))
                .thenReturn(false);

        StepVerifier.create(dataTransferService.transferData(sourceSystem, destSystem, dataType, sourceParams, destParams))
                .expectErrorMatches(throwable -> {
                    if (throwable instanceof UnsupportedOperationException ex) {
                        return String.format(
                                "Unsupported data type '%s' for external system '%s'", dataType, destSystem).equals(ex.getMessage());
                    }
                    return false;
                })
                .verify();
    }
}
