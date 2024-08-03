package com.quickbase.datatransfer.gateway.freshdesk;

import com.quickbase.datatransfer.dto.UserDTO;
import com.quickbase.datatransfer.exception.AmbiguousDataException;
import com.quickbase.datatransfer.exception.HttpRequestFailedException;
import com.quickbase.datatransfer.exception.InvalidDataException;
import com.quickbase.datatransfer.exception.MissingExternalSystemParamException;
import com.quickbase.datatransfer.gateway.freshdesk.model.FreshdeskContact;
import com.quickbase.datatransfer.gateway.freshdesk.model.FreshdeskContactRequestBody;
import com.quickbase.datatransfer.gateway.freshdesk.model.FreshdeskErrorResponse;
import com.quickbase.datatransfer.gateway.util.WebUtils;
import com.quickbase.datatransfer.common.DataType;
import com.quickbase.datatransfer.service.DataTypeToDtoMatcher;
import com.quickbase.datatransfer.service.DataUploader;
import com.quickbase.datatransfer.service.TransferrerTypeChecker;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class FreshdeskGatewayService {
    private static final String FRESHDESK_API_BASE_URL_FORMAT = "https://%s.freshdesk.com";
    private static final String AUTH_TOKEN_ENV_VAR = "FRESHDESK_TOKEN";
    private static final String DOMAIN_PARAM = "domain";
    private static final String CONTACTS_API_PATH = "/api/v2/contacts";
    private static final String SEARCH_CONTACTS_API_PATH = CONTACTS_API_PATH + "/autocomplete?term=";
    private static final String EXTERNAL_SYSTEM_NAME = "Freshdesk";

    public static abstract class FreshdeskTypeChecker implements TransferrerTypeChecker {
        @Override
        public boolean systemTypeMatches(String systemType) {
            return isFreshdeskSystemType(systemType);
        }
    }

    @Service
    public static class UserDataUploader extends FreshdeskTypeChecker implements DataUploader<UserDTO> {
        @Override
        public Mono<Void> uploadData(Map<String, String> params, UserDTO data) {
            if (data == null || Strings.isBlank(data.name)) {
                return Mono.error(new InvalidDataException("Can't create/update Freshdesk contact: missing name in data for upload."));
            }

            return Mono.fromCallable(() -> createWebClient(params))
                    .flatMap(webClient -> searchContactsByName(webClient, data.name)
                            .flatMap(contacts -> {
                                if (contacts.size() > 1) {
                                    return Mono.error(new AmbiguousDataException(
                                            String.format("Found more than one Freshdesk contact with name '%s'. " +
                                                    "Can't define which contact to update.", data.name),
                                            EXTERNAL_SYSTEM_NAME));
                                }

                                FreshdeskContactRequestBody contact = transformFromDTO(data);

                                if (contacts.size() == 1) {
                                    return updateExistingContact(webClient, contact, contacts.get(0).id, data.name);
                                }

                                return createContact(webClient, contact);
                            }));
        }

        @Override
        public boolean dataTypeMatches(DataType dataType) {
            return DataTypeToDtoMatcher.dataTypeMatchesDTO(dataType, UserDTO.class);
        }

        private Mono<List<FreshdeskContact>> searchContactsByName(WebClient webClient, String name) {
            ParameterizedTypeReference<List<FreshdeskContact>> responseType =
                    new ParameterizedTypeReference<>() {};
            String searchTerm = WebUtils.urlEncode(name);

            log.info("Searching for Freshdesk contacts by search term '{}'...", searchTerm);

            return webClient.get()
                    .uri(SEARCH_CONTACTS_API_PATH + searchTerm)
                    .exchangeToMono(clientResponse -> handleHttpError(clientResponse)
                            .flatMap(response -> response.bodyToMono(responseType)))
                    // result includes all contacts whose name starts with the search term, but we only care about exact matches
                    .map(contacts -> contacts.stream()
                            .filter(contact -> name.equals(contact.name))
                            .collect(Collectors.toList()))
                    .doOnSuccess(contacts -> {
                        if (!contacts.isEmpty()) {
                            log.info("Found {} Freshdesk contact(s) matching name '{}': {}",
                                    contacts.size(), name, contacts);
                        }
                    })
                    .doOnError(ex -> log.error("Searching for Freshdesk contacts by search term '{}' failed:",
                            searchTerm, ex));
        }

        private FreshdeskContactRequestBody transformFromDTO(UserDTO dataDTO) {
            FreshdeskContactRequestBody contact = new FreshdeskContactRequestBody();
            contact.name = dataDTO.name;
            contact.address = dataDTO.address;
            contact.email = dataDTO.email;
            return contact;
        }

        private Mono<Void> updateExistingContact(WebClient webClient, FreshdeskContactRequestBody body, Long id, String name) {
            if (id == null) {
                return Mono.error(new InvalidDataException("Freshdesk contact in search-by-name result has no ID. Can't update it."));
            }

            log.info("Updating existing Freshdesk contact with name '{}' and id '{}'...", name, id);

            return webClient.put()
                    .uri(CONTACTS_API_PATH + "/" + id)
                    .bodyValue(body)
                    .exchangeToMono(clientResponse -> handleHttpError(clientResponse)
                            .flatMap(response -> response.bodyToMono(Void.class)))
                    .doOnSuccess(__ -> log.info("Successfully updated Freshdesk contact with name '{}' and id '{}'",
                            name, id))
                    .doOnError(ex -> log.error("Updating Freshdesk contact with name '{}' and id '{}' failed:",
                            name, id, ex));
        }


        private Mono<Void> createContact(WebClient webClient, FreshdeskContactRequestBody requestBody) {
            log.info("Creating Freshdesk contact with name '{}'...", requestBody.name);

            return webClient.post()
                    .uri(CONTACTS_API_PATH)
                    .bodyValue(requestBody)
                    .exchangeToMono(clientResponse -> handleHttpError(clientResponse)
                            .flatMap(response -> response.bodyToMono(Void.class)))
                    .doOnSuccess(__ -> log.info("Successfully created Freshdesk contact with name '{}'",
                            requestBody.name))
                    .doOnError(ex -> log.error("Creating Freshdesk contact with name '{}' failed:",
                            requestBody.name, ex));
        }
    }

    public static boolean isFreshdeskSystemType(String systemType) {
        return EXTERNAL_SYSTEM_NAME.equalsIgnoreCase(systemType);
    }

    private static WebClient createWebClient(Map<String, String> params) {
        String freshdeskDomain = getDomain(params);
        String authorizationToken = WebUtils.getAuthToken(AUTH_TOKEN_ENV_VAR,
                String.format(
                        "Please set '%s' env var to your base64-encoded Freshdesk API token in order to authenticate.",
                        AUTH_TOKEN_ENV_VAR),
                EXTERNAL_SYSTEM_NAME);

        return WebClient.create(String.format(FRESHDESK_API_BASE_URL_FORMAT, freshdeskDomain))
                .mutate()
                .defaultHeader(HttpHeaders.AUTHORIZATION, authorizationToken)
                .build();
    }

    private static String getDomain(Map<String, String> params) {
        if (params == null || !params.containsKey(DOMAIN_PARAM)){
            throw new MissingExternalSystemParamException(
                    "Not able to identify the Freshdesk domain towards which to make calls.",
                    DOMAIN_PARAM, EXTERNAL_SYSTEM_NAME);
        }

        return params.get(DOMAIN_PARAM);
    }

    private static Mono<ClientResponse> handleHttpError(ClientResponse response) {
        HttpStatusCode httpStatus = response.statusCode();

        if (httpStatus.isError()) {
            return response.bodyToMono(FreshdeskErrorResponse.class)
                    .flatMap(errorResponse -> {
                        String humanReadableMsg = convertToHumanReadableMessage(errorResponse);

                        return Mono.error(new HttpRequestFailedException(
                                Strings.isNotBlank(humanReadableMsg) ? humanReadableMsg : "No info about the error from Freshdesk",
                                EXTERNAL_SYSTEM_NAME,
                                httpStatus
                        ));
                    });
        }

        return Mono.just(response);
    }

    private static String convertToHumanReadableMessage(FreshdeskErrorResponse response) {
        StringBuilder sb = new StringBuilder();

        if (response.description != null) {
            sb.append(response.description);
        }

        if (response.errors != null && !response.errors.isEmpty()) {
            sb.append("; more info about the failure:");

            response.errors.forEach(error -> {
                sb.append('\n');

                if (error.field != null) {
                    sb.append(String.format("The request field that triggerred this error: '%s'.", error.field));
                }

                if (error.message != null) {
                    sb.append(String.format(" Detailed error message: '%s'.", error.message));
                }
            });
        }

        return sb.toString();
    }
}
