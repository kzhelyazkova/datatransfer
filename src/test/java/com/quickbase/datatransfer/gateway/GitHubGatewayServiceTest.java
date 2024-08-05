package com.quickbase.datatransfer.gateway;

import static com.quickbase.datatransfer.gateway.github.GitHubGatewayService.EXTERNAL_SYSTEM_NAME;
import static com.quickbase.datatransfer.gateway.github.GitHubGatewayService.UserDataDownloader.USERNAME_PARAM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;
import com.quickbase.datatransfer.common.ConfigPropertyProvider;
import com.quickbase.datatransfer.data.UserData;
import com.quickbase.datatransfer.exception.HttpRequestFailedException;
import com.quickbase.datatransfer.exception.MissingExternalSystemParamException;
import com.quickbase.datatransfer.exception.UnauthorizedOperationException;
import com.quickbase.datatransfer.gateway.github.GitHubGatewayService.UserDataDownloader;
import com.quickbase.datatransfer.gateway.github.model.GitHubUserResponse;
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

        GitHubUserResponse gitHubUser = new GitHubUserResponse();
        gitHubUser.name = "John Smith";
        gitHubUser.email = "jsmith@bluesky.com";
        gitHubUser.location = "Arizona, US";
        gitHubUser.twitterUsername = "johnnys";
        gitHubUser.bio = "A cat lover";
        gitHubUser.login = "johnsmith";

        mockServer.stubFor(
                WireMock.get(WireMock.urlPathEqualTo("/users/" + username))
                        .willReturn(WireMock.aResponse()
                                .withStatus(HttpStatus.OK.value())
                                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .withBody(Json.write(gitHubUser))));

        Mono<UserData> resultMono = userDataDownloader.downloadData(Map.of(USERNAME_PARAM, username));

        StepVerifier.create(resultMono)
                .assertNext(downloadedData -> {
                    assertNotNull(downloadedData);
                    assertEquals(gitHubUser.name, downloadedData.name);
                    assertEquals(gitHubUser.email, downloadedData.email);
                    assertEquals(gitHubUser.location, downloadedData.address);
                    assertEquals(gitHubUser.twitterUsername, downloadedData.twitterHandle);
                    assertEquals(gitHubUser.bio, downloadedData.description);
                    assertEquals(gitHubUser.login, downloadedData.externalId);
                })
                .verifyComplete();
    }

    @Test
    public void testDownloadUserData_missingUsername() {
        Mono<UserData> resultMono = userDataDownloader.downloadData(Collections.emptyMap());

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

        Mono<UserData> resultMono = userDataDownloader.downloadData(Map.of(USERNAME_PARAM, "jsmith"));

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

        Mono<UserData> resultMono = userDataDownloader.downloadData(Map.of(USERNAME_PARAM, username));

        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable -> {
                    if (throwable instanceof HttpRequestFailedException ex) {
                        return EXTERNAL_SYSTEM_NAME.equals(ex.externalSystemName)
                                && HttpStatus.NOT_FOUND.value() == ex.httpStatusCode.value()
                                && String.format("GitHub user with username '%s' does not exist.", username).equals(ex.getMessage());
                    }
                    return false;
                })
                .verify();
    }
}
