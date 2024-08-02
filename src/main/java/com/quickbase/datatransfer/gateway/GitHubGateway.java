package com.quickbase.datatransfer.gateway;

import com.quickbase.datatransfer.dto.UserDataDTO;
import com.quickbase.datatransfer.gateway.model.GitHubUserData;
import com.quickbase.datatransfer.service.DataDownloader;
import com.quickbase.datatransfer.service.TransfererTypeChecker;
import com.quickbase.datatransfer.service.ExternalSystem;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

public class GitHubGateway implements ExternalSystem {
    private static final String GITHUB_API_BASE_URL = "https://api.github.com";
    private static final String AUTHORIZATION_TOKEN_ENV_VAR = "GITHUB_TOKEN";
    private static final String ACCEPT_HEADER_VALUE = "application/vnd.github+json";
    private static final WebClient WEB_CLIENT = initializeWebClient();

    public static abstract class GitHubTypeChecker implements TransfererTypeChecker {
        @Override
        public boolean systemTypeMatches(String systemType) {
            return isGitHubSystemType(systemType);
        }
    }

    @Service
    public static class UserDataDownloader extends GitHubTypeChecker implements DataDownloader<UserDataDTO> {
        private static final String USER_API_PATH = "/users";
        private static final String USERNAME_PARAM = "username";

        @Override
        public Mono<UserDataDTO> downloadData(Map<String, String> params) {
            //System.out.println("Downloaded user data: " + downloadedData);
            return downloadUserData(params)
                    .map(this::transformToDTO);
            //return new UserDataDTO("kzhelyazkova");
        }

        @Override
        public boolean dataTypeMatches(String dataType) {
            return UserDataDTO.isUserDataType(dataType);
        }

        private String getUsername(Map<String, String> params) {
            if (!params.containsKey(USERNAME_PARAM)){
                throw new RuntimeException("Missing required by GitHub parameter: " + USERNAME_PARAM);
            }

            return params.get(USERNAME_PARAM);
        }

        private Mono<GitHubUserData> downloadUserData(Map<String, String> params) {
            return WEB_CLIENT
                    .get()
                    .uri(USER_API_PATH + "/" + getUsername(params))
                    .exchangeToMono(response -> response.bodyToMono(GitHubUserData.class));
        }

        private UserDataDTO transformToDTO(GitHubUserData downloadedData) {
            UserDataDTO result = new UserDataDTO();
            result.name = downloadedData.name;
            result.address = downloadedData.location;
            result.company = downloadedData.company;
            result.email = downloadedData.email;
            return result;
        }
    }

    public static boolean isGitHubSystemType(String systemType) {
        return "github".equals(systemType);
    }

    private static WebClient initializeWebClient() {
        return WebClient.create(GITHUB_API_BASE_URL)
                .mutate()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + getAuthorizationToken())
                .defaultHeader(HttpHeaders.ACCEPT, ACCEPT_HEADER_VALUE)
                .build();
    }

    private static String getAuthorizationToken() {
        String envVarValue = System.getenv(AUTHORIZATION_TOKEN_ENV_VAR);
        if (envVarValue == null) {
            throw new RuntimeException("Can't authenticate to GitHub! Please set " + AUTHORIZATION_TOKEN_ENV_VAR
                    + " env var to your GitHub API token in order to authenticate.");
        }

        return envVarValue;
    }
}
