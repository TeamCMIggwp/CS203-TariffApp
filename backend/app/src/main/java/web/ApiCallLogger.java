package web;

import jakarta.servlet.http.*;
import org.slf4j.*;
import org.springframework.stereotype.*;
import org.springframework.web.servlet.*;

@Component
public class ApiCallLogger implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(ApiCallLogger.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String fullUrl = request.getRequestURL().toString();
        String queryString = request.getQueryString();
        
        if (queryString != null) {
            fullUrl += "?" + queryString;
        }
        
        logger.info("API Call: {}", fullUrl);
        
        return true;
    }
}