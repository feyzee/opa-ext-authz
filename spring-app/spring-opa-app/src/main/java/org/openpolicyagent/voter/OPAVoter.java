package org.openpolicyagent.voter;

import org.springframework.http.HttpEntity;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class OPAVoter implements AccessDecisionVoter<Object> {

    private final String opaUrl;

    private final RestTemplate client = new RestTemplate();

    public OPAVoter(String opaUrl) {
        this.opaUrl = opaUrl;
    }

    @Override
    public boolean supports(ConfigAttribute attribute) {
        return true;
    }

    @Override
    public boolean supports(Class clazz) {
        return true;
    }

    @Override
    public int vote(Authentication authentication, Object obj, Collection<ConfigAttribute> attrs) {

        if (!(obj instanceof FilterInvocation)) {
            return ACCESS_ABSTAIN;
        }

        FilterInvocation filter = (FilterInvocation) obj;
        Map<String, String> headers = new HashMap<String, String>();

        for (Enumeration<String> headerNames = filter.getRequest().getHeaderNames(); headerNames.hasMoreElements(); ) {
            String header = headerNames.nextElement();
            headers.put(header, filter.getRequest().getHeader(header));
        }

        String[] path = filter.getRequest().getRequestURI().replaceAll("^/|/$", "").split("/");

        Map<String, Object> input = new HashMap<String, Object>();
        input.put("auth", authentication);
        input.put("user", authentication.getName());
        input.put("method", filter.getRequest().getMethod());
        input.put("path", path);
        input.put("headers", headers);

        HttpEntity<?> request = new HttpEntity<>(new OPADataRequest(input));
        OPADataResponse response = client.postForObject(this.opaUrl, request, OPADataResponse.class);

        if (Boolean.FALSE.equals(response.getResult().get("allowed"))) {
            return ACCESS_DENIED;
        }

        return ACCESS_GRANTED;
    }

}
