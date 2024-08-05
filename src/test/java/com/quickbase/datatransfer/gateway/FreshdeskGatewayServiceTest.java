package com.quickbase.datatransfer.gateway;

import com.github.tomakehurst.wiremock.common.Json;
import com.quickbase.datatransfer.common.ConfigPropertyProvider;
import com.quickbase.datatransfer.data.UserData;
import com.quickbase.datatransfer.exception.*;
import com.quickbase.datatransfer.gateway.freshdesk.FreshdeskGatewayService.UserDataUploader;
import com.quickbase.datatransfer.gateway.freshdesk.model.FreshdeskContactResponse;
import com.quickbase.datatransfer.gateway.freshdesk.model.FreshdeskContactRequest;
import com.quickbase.datatransfer.gateway.freshdesk.model.FreshdeskErrorResponse;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.quickbase.datatransfer.gateway.freshdesk.FreshdeskGatewayService.CONTACTS_API_PATH;
import static com.quickbase.datatransfer.gateway.freshdesk.FreshdeskGatewayService.DOMAIN_PARAM;
import static com.quickbase.datatransfer.gateway.freshdesk.FreshdeskGatewayService.EXTERNAL_SYSTEM_NAME;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class FreshdeskGatewayServiceTest extends GatewayTestBase {

    @SpyBean
    private UserDataUploader userDataUploader;

    @MockBean
    private ConfigPropertyProvider configPropertyProvider;


    @Before
    public void setUp() {
        when(userDataUploader.getApiBaseUrl(anyString())).thenReturn("http://localhost:" + mockServer.port());
        when(configPropertyProvider.getConfigPropertyValue("FRESHDESK_TOKEN")).thenReturn("mock-token");
    }

    @Test
    public void testCreateUser_success() {
        UserData user = buildUser();

        mockServer.stubFor(
                get(urlPathEqualTo("/api/v2/contacts/autocomplete"))
                        .willReturn(aResponse()
                                .withStatus(HttpStatus.OK.value())
                                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .withBody("[]")));

        mockServer.stubFor(
                post(urlPathEqualTo(CONTACTS_API_PATH))
                        .willReturn(aResponse()
                                .withStatus(HttpStatus.CREATED.value())));

        StepVerifier.create(userDataUploader.uploadData(Map.of(DOMAIN_PARAM, "bluesky"), user))
                .verifyComplete();

        FreshdeskContactRequest expectedCreateRequestBody = buildRequestBody(user);

        mockServer.verify(
                getRequestedFor(urlPathEqualTo("/api/v2/contacts/autocomplete")));
        mockServer.verify(
                postRequestedFor(urlPathEqualTo(CONTACTS_API_PATH))
                        .withRequestBody(
                                equalToJson(Json.write(expectedCreateRequestBody))));
    }

    @Test
    public void testUpdateUser_success() {
        UserData user = buildUser();
        Long freshdeskContactId = 123L;
        List<FreshdeskContactResponse> searchResponseBody = new ArrayList<>(1);
        searchResponseBody.add(new FreshdeskContactResponse(freshdeskContactId, user.name));

        mockServer.stubFor(
                get(urlPathEqualTo("/api/v2/contacts/autocomplete"))
                        .willReturn(aResponse()
                                .withStatus(HttpStatus.OK.value())
                                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .withBody(Json.write(searchResponseBody))));

        mockServer.stubFor(
                put(urlPathEqualTo(CONTACTS_API_PATH + "/" + freshdeskContactId))
                        .willReturn(aResponse()
                                .withStatus(HttpStatus.OK.value())));

        StepVerifier.create(userDataUploader.uploadData(Map.of(DOMAIN_PARAM, "bluesky"), user))
                .verifyComplete();

        FreshdeskContactRequest expectedUpdateRequestBody = buildRequestBody(user);

        mockServer.verify(
                getRequestedFor(urlPathEqualTo("/api/v2/contacts/autocomplete")));
        mockServer.verify(
                putRequestedFor(urlPathEqualTo(CONTACTS_API_PATH + "/" + freshdeskContactId))
                        .withRequestBody(
                                equalToJson(Json.write(expectedUpdateRequestBody))));
    }

    @Test
    public void testUploadUserData_missingName() {
        Mono<Void> resultMono = userDataUploader.uploadData(Map.of(DOMAIN_PARAM, "bluesky"), null);

        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable -> {
                    if (throwable instanceof InvalidDataException ex) {
                        return "Can't create/update Freshdesk contact: missing name in data for upload.".equals(ex.getMessage());
                    }
                    return false;
                })
                .verify();
    }

    @Test
    public void testUploadUserData_missingDomain() {
        UserData user = buildUser();

        Mono<Void> resultMono = userDataUploader.uploadData(Collections.emptyMap(), user);

        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable -> {
                    if (throwable instanceof MissingExternalSystemParamException ex) {
                        return DOMAIN_PARAM.equals(ex.param) && EXTERNAL_SYSTEM_NAME.equals(ex.externalSystemName);
                    }
                    return false;
                })
                .verify();
    }

    @Test
    public void testUploadUserData_missingAuthToken() {
        when(configPropertyProvider.getConfigPropertyValue("FRESHDESK_TOKEN")).thenReturn(null);
        UserData user = buildUser();

        Mono<Void> resultMono = userDataUploader.uploadData(Map.of(DOMAIN_PARAM, "bluesky"), user);

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
    public void testUploadUserData_twoExistingContacts() {
        UserData user = buildUser();
        List<FreshdeskContactResponse> searchResponseBody = new ArrayList<>(2);
        searchResponseBody.add(new FreshdeskContactResponse(123L, user.name));
        searchResponseBody.add(new FreshdeskContactResponse(456L, user.name));

        mockServer.stubFor(
                get(urlPathEqualTo("/api/v2/contacts/autocomplete"))
                        .willReturn(aResponse()
                                .withStatus(HttpStatus.OK.value())
                                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .withBody(Json.write(searchResponseBody))));

        Mono<Void> resultMono = userDataUploader.uploadData(Map.of(DOMAIN_PARAM, "bluesky"), user);

        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable -> {
                    if (throwable instanceof AmbiguousDataException ex) {
                        return EXTERNAL_SYSTEM_NAME.equals(ex.externalSystemName)
                                && String.format("Found more than one Freshdesk contact with name '%s'. " +
                                "Can't define which contact to update.", user.name).equals(ex.getMessage());
                    }
                    return false;
                })
                .verify();
    }

    @Test
    public void testCreateUser_httpError() {
        UserData user = buildUser();

        mockServer.stubFor(
                get(urlPathEqualTo("/api/v2/contacts/autocomplete"))
                        .willReturn(aResponse()
                                .withStatus(HttpStatus.OK.value())
                                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .withBody("[]")));

        FreshdeskErrorResponse createRequestResponse = new FreshdeskErrorResponse();
        createRequestResponse.description = "Validation failed";
        createRequestResponse.errors = List.of(
                new FreshdeskErrorResponse.Error("email", "invalid email", "invalid_value"));

        mockServer.stubFor(
                post(urlPathEqualTo(CONTACTS_API_PATH))
                        .willReturn(aResponse()
                                .withStatus(HttpStatus.BAD_REQUEST.value())
                                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .withBody(Json.write(createRequestResponse))));

        Mono<Void> resultMono = userDataUploader.uploadData(Map.of(DOMAIN_PARAM, "bluesky"), user);

        StepVerifier.create(resultMono)
                .expectErrorMatches(throwable -> {
                    if (throwable instanceof HttpRequestFailedException ex) {
                        return EXTERNAL_SYSTEM_NAME.equals(ex.externalSystemName)
                                && ex.httpStatusCode.value() == HttpStatus.BAD_REQUEST.value()
                                && ("Validation failed; more info about the failure:\n" +
                                "The request field that triggerred this error: 'email'. " +
                                "Detailed error message: 'invalid email'.").equals(ex.getMessage());
                    }
                    return false;
                })
                .verify();
    }

    private static UserData buildUser() {
        UserData user = new UserData();
        user.name = "John Smith";
        user.email = "jsmith@bluesky.com";
        user.address = "Arizona, US";
        user.twitterHandle= "johnnys";
        user.description = "A cat lover";
        user.externalId = "johnsmith";
        return user;
    }

    private static FreshdeskContactRequest buildRequestBody(UserData user) {
        FreshdeskContactRequest requestBody = new FreshdeskContactRequest();
        requestBody.name = user.name;
        requestBody.email = user.email;
        requestBody.address = user.address;
        requestBody.twitterId = user.twitterHandle;
        requestBody.description = user.description;
        requestBody.uniqueExternalId = user.externalId;
        return requestBody;
    }
}
