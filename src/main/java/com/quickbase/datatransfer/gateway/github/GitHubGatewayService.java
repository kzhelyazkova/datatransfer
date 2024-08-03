package com.quickbase.datatransfer.gateway.github;

import com.quickbase.datatransfer.dto.UserDTO;
import com.quickbase.datatransfer.exception.MissingExternalSystemParamException;
import com.quickbase.datatransfer.gateway.github.model.GitHubUser;
import com.quickbase.datatransfer.gateway.util.WebUtils;
import com.quickbase.datatransfer.common.DataType;
import com.quickbase.datatransfer.service.DataDownloader;
import com.quickbase.datatransfer.service.DataTypeToDtoMatcher;
import com.quickbase.datatransfer.service.TransferrerTypeChecker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
public class GitHubGatewayService {
    private static final String GITHUB_API_BASE_URL = "https://api.github.com";
    private static final String AUTH_TOKEN_ENV_VAR = "GITHUB_TOKEN";
    private static final String ACCEPT_HEADER_VALUE = "application/vnd.github+json";
    private static final String EXTERNAL_SYSTEM_NAME = "GitHub";
    private static final String USER_API_PATH = "/users";

    public static abstract class GitHubTypeChecker implements TransferrerTypeChecker {
        @Override
        public boolean systemTypeMatches(String systemType) {
            return isGitHubSystemType(systemType);
        }
    }

    @Service
    public static class UserDataDownloader extends GitHubTypeChecker implements DataDownloader<UserDTO> {
        private static final String USERNAME_PARAM = "username";

        @Override
        public Mono<UserDTO> downloadData(Map<String, String> params) {
            return downloadUserData(params)
                    .map(this::transformToDTO);
        }

        @Override
        public boolean dataTypeMatches(DataType dataType) {
            return DataTypeToDtoMatcher.dataTypeMatchesDTO(dataType, UserDTO.class);
        }

        private Mono<GitHubUser> downloadUserData(Map<String, String> params) {
            return Mono.fromCallable(() -> getUsername(params))
                    .flatMap(username -> {
                        WebClient webClient = createWebClient();

                        log.info("Getting GitHub user with username '{}'", username);

                        return webClient.get()
                                .uri(USER_API_PATH + "/" + username)
                                .exchangeToMono(clientResponse -> WebUtils.handleHttpError(
                                                clientResponse,
                                                EXTERNAL_SYSTEM_NAME,
                                                httpStatusCode -> httpStatusCode.value() == HttpStatus.NOT_FOUND.value() ?
                                                        String.format("GitHub user with username '%s' not found.",
                                                                username) :
                                                        String.format("Unexpected failure when getting GitHub user with username '%s'",
                                                                username))
                                        .flatMap(response -> response.bodyToMono(GitHubUser.class)))
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

        private UserDTO transformToDTO(GitHubUser downloadedData) {
            UserDTO result = new UserDTO();
            result.name = downloadedData.name;
            result.address = downloadedData.location;
            result.company = downloadedData.company;
            result.email = downloadedData.email;
            return result;
        }
    }

    public static boolean isGitHubSystemType(String systemType) {
        return EXTERNAL_SYSTEM_NAME.equalsIgnoreCase(systemType);
    }

    private static WebClient createWebClient() {
        String authToken = WebUtils.getAuthToken(AUTH_TOKEN_ENV_VAR,
                String.format(
                        "Please set '%s' env var to your GitHub API token in order to authenticate.",
                        AUTH_TOKEN_ENV_VAR),
                EXTERNAL_SYSTEM_NAME);

        return WebClient.create(GITHUB_API_BASE_URL)
                .mutate()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                .defaultHeader(HttpHeaders.ACCEPT, ACCEPT_HEADER_VALUE)
                .build();
    }
}
