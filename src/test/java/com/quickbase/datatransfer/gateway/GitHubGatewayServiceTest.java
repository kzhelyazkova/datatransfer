package com.quickbase.datatransfer.gateway;

import static com.quickbase.datatransfer.gateway.github.GitHubGatewayService.EXTERNAL_SYSTEM_NAME;
import static com.quickbase.datatransfer.gateway.github.GitHubGatewayService.UserDataDownloader.USERNAME_PARAM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;
import com.quickbase.datatransfer.common.ConfigPropertyProvider;
import com.quickbase.datatransfer.dto.UserDTO;
import com.quickbase.datatransfer.exception.HttpRequestFailedException;
import com.quickbase.datatransfer.exception.MissingExternalSystemParamException;
import com.quickbase.datatransfer.exception.UnauthorizedOperationException;
import com.quickbase.datatransfer.gateway.github.GitHubGatewayService.UserDataDownloader;
import com.quickbase.datatransfer.gateway.github.model.GitHubUser;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.Map;

public class GitHubGatewayServiceTest extends GatewayTestBase {
    @SpyBean
    private UserDataDownloader userDataDownloader;

    @MockBean
    private ConfigPropertyProvider configPropertyProvider;

    @Before
    public void setUp() {
        when(userDataDownloader.getApiBaseUrl()).thenReturn("http://localhost:" + mockServer.port());
        when(configPropertyProvider.getConfigPropertyValue("GITHUB_TOKEN")).thenReturn("mock-token");
    }

    @Test
    public void testDownloadUserData_success() {
        String username = "jsmith";

        GitHubUser gitHubUser = new GitHubUser();
        gitHubUser.name = "John Smith";
        gitHubUser.company = "BlueSKy";
        gitHubUser.email = "jsmith@bluesky.com";
        gitHubUser.location = "Arizona, US";

        mockServer.stubFor(
                WireMock.get(WireMock.urlPathEqualTo("/users/" + username))
                        .willReturn(WireMock.aResponse()
                                .withStatus(HttpStatus.OK.value())
                                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .withBody(Json.write(gitHubUser))));

        Mono<UserDTO> resultMono = userDataDownloader.downloadData(Map.of(USERNAME_PARAM, username));

        StepVerifier.create(resultMono)
                .assertNext(downloadedData -> {
                    assertNotNull(downloadedData);
                    assertEquals(gitHubUser.name, downloadedData.name);
                    assertEquals(gitHubUser.company, downloadedData.company);
                    assertEquals(gitHubUser.email, downloadedData.email);
                    assertEquals(gitHubUser.location, downloadedData.address);
                })
                .verifyComplete();
    }

    @Test
    public void testDownloadUserData_missingUsername() {
        Mono<UserDTO> resultMono = userDataDownloader.downloadData(Collections.emptyMap());

        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable -> {
                    if (throwable instanceof MissingExternalSystemParamException ex) {
                        return USERNAME_PARAM.equals(ex.param) && EXTERNAL_SYSTEM_NAME.equals(ex.externalSystemName);
                    }
                    return false;
                })
                .verify();
    }

    @Test
    public void testDownloadUserData_missingAuthToken() {
        when(configPropertyProvider.getConfigPropertyValue("GITHUB_TOKEN")).thenReturn(null);

        Mono<UserDTO> resultMono = userDataDownloader.downloadData(Map.of(USERNAME_PARAM, "jsmith"));

        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable -> {
                    if (throwable instanceof UnauthorizedOperationException ex) {
                        return EXTERNAL_SYSTEM_NAME.equals(ex.externalSystemName);
                    }
                    return false;
                })
                .verify();
    }

    @Test
    public void testDownloadUserData_nonExistingGitHubUser() {
        String username = "jsmith";

        mockServer.stubFor(
                WireMock.get(WireMock.urlPathEqualTo("/users/" + username))
                        .willReturn(WireMock.aResponse()
                                .withStatus(HttpStatus.NOT_FOUND.value())));

        Mono<UserDTO> resultMono = userDataDownloader.downloadData(Map.of(USERNAME_PARAM, username));

        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable -> {
                    if (throwable instanceof HttpRequestFailedException ex) {
                        return EXTERNAL_SYSTEM_NAME.equals(ex.externalSystemName)
                                && HttpStatus.NOT_FOUND.value() == ex.httpStatusCode.value()
                                && String.format("GitHub user with username '%s' not found.", username).equals(ex.getMessage());
                    }
                    return false;
                })
                .verify();
    }
}
