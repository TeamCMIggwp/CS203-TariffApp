package web;

import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.web.servlet.config.annotation.*;

@SpringBootApplication(scanBasePackages = {
        "web",
        "geminianalysis",
        "wits",
        "wto",
        "tariffcalculator",
        "database",
        "auth",
        "config",
        "scraper",
        "exchangerate"
})
public class AppApplication implements WebMvcConfigurer {

    private final ApiCallLogger apiCallLogger;

    public AppApplication(ApiCallLogger apiCallLogger) {
        this.apiCallLogger = apiCallLogger;
    }

    public static void main(String[] args) {
        SpringApplication.run(AppApplication.class, args);
    }

    /**
     * Register the API call logger interceptor
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiCallLogger);
    }
}
