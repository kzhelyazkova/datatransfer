package com.quickbase.datatransfer.gateway.github;

import com.quickbase.datatransfer.common.ConfigPropertyProvider;
import com.quickbase.datatransfer.data.UserData;
import com.quickbase.datatransfer.exception.MissingExternalSystemParamException;
import com.quickbase.datatransfer.exception.UnauthorizedOperationException;
import com.quickbase.datatransfer.gateway.github.model.GitHubUserResponse;
import com.quickbase.datatransfer.gateway.util.WebUtils;
import com.quickbase.datatransfer.common.DataType;
import com.quickbase.datatransfer.service.DataDownloader;
import com.quickbase.datatransfer.service.DataTypeToDataClassMatcher;
import com.quickbase.datatransfer.service.TransferrerTypeChecker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
public class GitHubGatewayService {
    public static final String EXTERNAL_SYSTEM_NAME = "GitHub";
    private static final String GITHUB_API_BASE_URL = "https://api.github.com";
    private static final String AUTH_TOKEN = "GITHUB_TOKEN";
    private static final String ACCEPT_HEADER_VALUE = "application/vnd.github+json";
    private static final String USER_API_PATH = "/users";

    public static abstract class GitHubDataProcessorBase implements TransferrerTypeChecker {
        private final ConfigPropertyProvider configPropertyProvider;

        public GitHubDataProcessorBase(ConfigPropertyProvider configPropertyProvider) {
            this.configPropertyProvider = configPropertyProvider;
        }

        @Override
        public boolean systemTypeMatches(String systemType) {
            return isGitHubSystemType(systemType);
        }

        public String getApiBaseUrl() {
            return getGitHubApiBaseUrl();
        }

        protected String getAuthToken() {
            String authToken = configPropertyProvider.getConfigPropertyValue(AUTH_TOKEN);

            if (authToken == null) {
                throw new UnauthorizedOperationException(
                        String.format("Please set '%s' (as %s) to your GitHub API token in order to authenticate.",
                                AUTH_TOKEN, configPropertyProvider.getConfigPropertyType()),
                        EXTERNAL_SYSTEM_NAME);
            }

            return authToken;
        }
    }

    @Service
    public static class UserDataDownloader extends GitHubDataProcessorBase implements DataDownloader<UserData> {
        public static final String USERNAME_PARAM = "username";

        @Autowired
        public UserDataDownloader(ConfigPropertyProvider configPropertyProvider) {
            super(configPropertyProvider);
        }

        @Override
        public Mono<UserData> downloadData(Map<String, String> params) {
            return downloadUserData(params)
                    .map(this::transformToAppData);
        }

        @Override
        public boolean dataTypeMatches(DataType dataType) {
            return DataTypeToDataClassMatcher.dataTypeMatchesDataClass(dataType, UserData.class);
        }

        private Mono<GitHubUserResponse> downloadUserData(Map<String, String> params) {
            return Mono.fromCallable(() -> getUsername(params))
                    .flatMap(username -> {
                        String baseApiUrl = getApiBaseUrl();
                        String authToken = getAuthToken();
                        WebClient webClient = createWebClient(baseApiUrl, authToken);

                        log.info("Getting GitHub user with username '{}'", username);

                        return webClient.get()
                                .uri(USER_API_PATH + "/" + username)
                                .exchangeToMono(clientResponse -> WebUtils.handleHttpError(
                                                clientResponse,
                                                EXTERNAL_SYSTEM_NAME,
                                                httpStatusCode -> httpStatusCode.value() == HttpStatus.NOT_FOUND.value() ?
                                                        String.format("GitHub user with username '%s' does not exist.",
                                                                username) :
                                                        String.format("Unexpected failure when getting GitHub user with username '%s'",
                                                                username))
                                        .flatMap(response -> response.bodyToMono(GitHubUserResponse.class)))
                                .doOnSuccess(__ -> log.info("Successfully obtained GitHub user with username '{}'",
                                        username))
                                .doOnError(ex -> log.error("Getting GitHub user with username '{}' failed:",
                                        username, ex));
                    });
        }

        private String getUsername(Map<String, String> params) {
            if (params == null || !params.containsKey(USERNAME_PARAM)){
                throw new MissingExternalSystemParamException(
                        "Not able to identify the GitHub user for which to download data.",
                        USERNAME_PARAM, EXTERNAL_SYSTEM_NAME);
            }

            return params.get(USERNAME_PARAM);
        }

        private UserData transformToAppData(GitHubUserResponse downloadedData) {
            UserData appData = new UserData();
            appData.name = downloadedData.name;
            appData.address = downloadedData.location;
            appData.email = downloadedData.email;
            appData.externalId = downloadedData.login;
            appData.description = downloadedData.bio;
            appData.twitterHandle = downloadedData.twitterUsername;
            return appData;
        }
    }

    public static boolean isGitHubSystemType(String systemType) {
        return EXTERNAL_SYSTEM_NAME.equalsIgnoreCase(systemType);
    }

    private static String getGitHubApiBaseUrl() {
        return GITHUB_API_BASE_URL;
    }

    private static WebClient createWebClient(String baseUrl, String authToken) {
        return WebClient.create(baseUrl)
                .mutate()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                .defaultHeader(HttpHeaders.ACCEPT, ACCEPT_HEADER_VALUE)
                .build();
    }
}
