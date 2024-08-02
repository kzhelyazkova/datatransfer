package com.quickbase.datatransfer.gateway;

import com.quickbase.datatransfer.dto.UserDataDTO;
import com.quickbase.datatransfer.gateway.model.FreshdeskContact;
import com.quickbase.datatransfer.gateway.model.FreshdeskContactBody;
import com.quickbase.datatransfer.service.DataUploader;
import com.quickbase.datatransfer.service.TransfererTypeChecker;
import com.quickbase.datatransfer.service.ExternalSystem;
import org.apache.logging.log4j.util.Strings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FreshdeskGateway implements ExternalSystem {
    private static final String FRESHDESK_API_BASE_URL_FORMAT = "https://%s.freshdesk.com";
    private static final String AUTHORIZATION_TOKEN_ENV_VAR = "FRESHDESK_TOKEN";
    private static final String DOMAIN_PARAM = "domain";

    public static abstract class FreshdeskTypeChecker implements TransfererTypeChecker {
        @Override
        public boolean systemTypeMatches(String systemType) {
            return isFreshdeskSystemType(systemType);
        }
    }

    @Service
    public static class UserDataUploader extends FreshdeskTypeChecker implements DataUploader<UserDataDTO> {
        private static final String CONTACTS_API_PATH = "/api/v2/contacts";
        private static final String SEARCH_CONTACTS_API_PATH = CONTACTS_API_PATH + "/autocomplete?term=";

        @Override
        public Mono<Void> uploadData(Map<String, String> params, UserDataDTO data) {
            System.out.println("Uploading data: " + data);

            if (Strings.isBlank(data.name)) {
                return Mono.error(new RuntimeException("Can't create/update Freshdesk contact. Missing name in downloaded data."));
            }

            WebClient webClient = createWebClient(params);
            return searchContactsByName(webClient, data.name)
                    .flatMap(contacts -> {
                        if (contacts.size() > 1) {
                            return Mono.error(new RuntimeException("Found more than one contact with name " + data.name + ". Can't define which contact to update. "));
                        }

                        FreshdeskContactBody contact = transformFromDTO(data);

                        if (contacts.size() == 1) {
                            System.out.println("Found 1 existing contact with name " + data.name + ". Updating it...");
                            return updateExistingContact(webClient, contacts.get(0).id, contact);
                        }

                        return createContact(webClient, contact);
                    });
            /*return Mono.just("uploaded data");*/
            // System.out.println("Uploading username: " + data.username);
        }

        @Override
        public boolean dataTypeMatches(String dataType) {
            return UserDataDTO.isUserDataType(dataType);
        }

        private String urlEncode(String value) {
            return new String(Base64.getUrlEncoder().encode(
                    value.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        }

        private Mono<List<FreshdeskContact>> searchContactsByName(WebClient webClient, String name) {
            ParameterizedTypeReference<List<FreshdeskContact>> responseType =
                    new ParameterizedTypeReference<>() {};

            return webClient.get()
                    .uri(SEARCH_CONTACTS_API_PATH + urlEncode(name))
                    .exchangeToMono(response -> response.bodyToMono(responseType))
                    // result includes all contacts whose name starts with the search term
                    .map(contacts -> contacts.stream()
                            .filter(contact -> name.equals(contact.name))
                            .collect(Collectors.toList()));
        }

        private FreshdeskContactBody transformFromDTO(UserDataDTO dataDTO) {
            FreshdeskContactBody contact = new FreshdeskContactBody();
            contact.name = dataDTO.name;
            contact.address = dataDTO.address;
            contact.email = dataDTO.email;
            return contact;
        }

        private Mono<Void> updateExistingContact(WebClient webClient, long id, FreshdeskContactBody body) {
            return webClient.put()
                    .uri(CONTACTS_API_PATH + "/" + id)
                    .bodyValue(body)
                    .exchangeToMono(response -> response.bodyToMono(Void.class));
        }


        private Mono<Void> createContact(WebClient webClient, FreshdeskContactBody body) {
            System.out.println("Creating contact...");
            System.out.println("body = " + body);

            return webClient.post()
                    .uri(CONTACTS_API_PATH)
                    .bodyValue(body)
                    .exchangeToMono(response -> {
                        System.out.println("status code = " + response.statusCode());
                        return response.bodyToMono(Void.class);
                    })
                    .doOnSuccess(__ -> System.out.println("Contact created successfully."))
                    .doOnError(e -> System.out.println("Failed to create contact: " + e.getMessage()));
        }
    }

    public static boolean isFreshdeskSystemType(String systemType) {
        return "freshdesk".equals(systemType);
    }

    private static WebClient createWebClient(Map<String, String> params) {
        System.out.println("url = " + String.format(FRESHDESK_API_BASE_URL_FORMAT, getDomain(params)));

        return WebClient.create(String.format(FRESHDESK_API_BASE_URL_FORMAT, getDomain(params)))
                .mutate()
                .defaultHeader(HttpHeaders.AUTHORIZATION, getAuthorizationToken())
                .build();
    }

    private static String getDomain(Map<String, String> params) {
        if (!params.containsKey(DOMAIN_PARAM)){
            throw new RuntimeException("Missing Freshdesk domain parameter: " + DOMAIN_PARAM);
        }

        return params.get(DOMAIN_PARAM);
    }

    private static String getAuthorizationToken() {
        String envVarValue = System.getenv(AUTHORIZATION_TOKEN_ENV_VAR);
        if (envVarValue == null) {
            throw new RuntimeException("Can't authenticate to Freshdesk! Please set " + AUTHORIZATION_TOKEN_ENV_VAR
                    + " env var to your base64-encoded Freshdesk API token in order to authenticate.");
        }

        return envVarValue;
    }
}
