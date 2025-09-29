package web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Web", description = "Endpoint to verify running SpringBoot")
public class HelloController {
    
    @GetMapping("/hello")
    @Operation(
        summary = "Get a return message",
        description = """
            Returns a message to verify the Spring Boot application is running correctly.
            
            **Example URL:** `https://teamcmiggwp.duckdns.org/hello`
            
            **cURL Example:**

            curl -X GET "https://teamcmiggwp.duckdns.org/hello" -H "accept: text/plain"
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved message",
            content = @Content(
                mediaType = "text/plain",
                schema = @Schema(type = "string", example = "Hello, Spring Boot is working!"),
                examples = @ExampleObject(
                    name = "Success Response",
                    value = "Hello, Spring Boot is working!",
                    description = "Standard message"
                )
            )
        )
    })
    public String hello() {
        return "Hello, Spring Boot is working!";
    }
}