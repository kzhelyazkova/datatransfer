package com.quickbase.datatransfer.gateway;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
// @SpringBootTest
public abstract class GatewayTestBase {
    @Rule
    public WireMockClassRule mockServer = new WireMockClassRule(new WireMockConfiguration().dynamicPort());
}
