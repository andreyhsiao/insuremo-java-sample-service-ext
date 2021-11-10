package com.ebao.cloud.gi;

import com.ebao.cloud.context.ThreadContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.http.HttpHeaders;

public class GIRequestInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {

      	String token = ThreadContext.getToken();
    	template.header(HttpHeaders.AUTHORIZATION, token);
    	//another option if thread changes
    	//HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    	//template.header(HttpHeaders.AUTHORIZATION, request.getHeader(HttpHeaders.AUTHORIZATION));
    }
}