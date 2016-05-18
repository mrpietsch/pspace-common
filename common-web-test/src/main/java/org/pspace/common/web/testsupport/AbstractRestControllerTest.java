package org.pspace.common.web.testsupport;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@IntegrationTest("server.port:0")
@WebAppConfiguration
public abstract class AbstractRestControllerTest<T> extends VirginDatabaseTest {

    private final String   urlPrefix;
    private final Class<T> entityClass;

    @Value("${local.server.port}")
    private int port;

    protected TestRestTemplate restTemplate = new TestRestTemplate();

    protected AbstractRestControllerTest(String urlPrefix, Class<T> entityClass) {
        assert urlPrefix.startsWith("/");
        assert !urlPrefix.endsWith("/");

        this.urlPrefix = urlPrefix;
        this.entityClass = entityClass;
    }


    private String getBaseUrl() {
        return "http://localhost:" + port + urlPrefix;
    }

    protected String getUrl(String url) {
        assert url.startsWith("/");
        return getBaseUrl()  + url;
    }

    protected T testGetEntity(String url, T exampleEntity) {
        ResponseEntity<T> responseEntity = restTemplate.getForEntity(getUrl(url), entityClass);
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.OK));
        T entity = responseEntity.getBody();
        assertThat(entity, equalTo(exampleEntity));
        return entity;
    }

    /**
     * @return the result of the service, or null on error
     */
    protected List<T> testGetList(String url) {
        @SuppressWarnings("unchecked")
        Class<T[]> arrayClass = (Class<T[]>) ((T[]) Array.newInstance(entityClass, 0)).getClass();
        ResponseEntity<T[]> responseEntity = restTemplate.getForEntity(getUrl(url), arrayClass);
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.OK));
        return Arrays.asList(responseEntity.getBody());
    }

    protected void testPostEntity(String url, T objectToPost) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<T> entity = new HttpEntity<>(objectToPost, headers);
        ResponseEntity<String> responseEntity = restTemplate.exchange("url", HttpMethod.POST, entity, String.class);
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.OK));
    }
}