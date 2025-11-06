# Integration Testing Guide

## Overview

This directory contains integration tests for the CS203 Tariff Application. Integration tests verify that multiple components of the application work together correctly, testing the full stack from HTTP requests through controllers, services, repositories, and database operations.

## Test Structure

### Base Test Class

**`BaseIntegrationTest.java`** - Abstract base class providing common test infrastructure:
- Starts full Spring Boot application with random port
- Uses H2 in-memory database configured to MySQL compatibility mode
- Provides `TestRestTemplate` for making HTTP requests
- Includes helper methods for authentication and database cleanup
- Auto-runs schema initialization before each test

### Integration Test Classes

1. **`NewsControllerIntegrationTest.java`** (9 tests)
   - Tests CRUD operations for news articles
   - Validates visibility toggle functionality
   - Tests duplicate handling and error scenarios
   - Verifies database persistence

2. **`TariffControllerIntegrationTest.java`** (10 tests)
   - Tests tariff rate CRUD operations
   - Validates composite key operations (reporter, partner, product, year)
   - Tests parameter validation
   - Verifies data constraints and error handling

3. **`AuthControllerIntegrationTest.java`** (11 tests)
   - Tests complete authentication flow: signup → login → refresh
   - Validates JWT token generation and format
   - Tests password reset flow
   - Tests Google OAuth integration endpoints
   - Validates duplicate email handling
   - Tests refresh token cookie management

4. **`UserHiddenSourcesControllerIntegrationTest.java`** (10 tests)
   - Tests user-specific hidden sources management
   - Validates user isolation (users cannot see each other's hidden sources)
   - Tests hide/unhide single and bulk operations
   - Validates authentication requirements
   - Tests edge cases (empty lists, duplicates, missing parameters)

## Running the Tests

### Run All Integration Tests

```bash
# Using Maven
mvn test -Dtest="integration.**"

# Run specific test class
mvn test -Dtest=NewsControllerIntegrationTest

# Run specific test method
mvn test -Dtest=NewsControllerIntegrationTest#createNews_withValidData_returnsCreatedNews
```

### Run from IDE

- **IntelliJ IDEA**: Right-click on `integration` package → Run 'Tests in integration'
- **Eclipse**: Right-click on `integration` package → Run As → JUnit Test
- **VS Code**: Click "Run Test" above each `@Test` method

### Run with Maven Wrapper

```bash
# Windows
mvnw.cmd test -Dtest="integration.**"

# Unix/Mac
./mvnw test -Dtest="integration.**"
```

## Test Configuration

### Test Properties

Tests use **`src/test/resources/application-test.properties`**:
- H2 in-memory database with MySQL compatibility
- Test JWT secrets and configuration
- Mock external API keys
- Disabled email sending (mock SMTP)
- Debug logging for SQL and authentication

### Test Database Schema

**`src/test/resources/schema.sql`** defines the database structure:
- `wto_tariffs.TariffRates` - Tariff rate data
- `News` - News articles with visibility flag
- `UserHiddenSources` - User-specific hidden news sources
- `NewsTariffRates` - News-tariff relationships
- Includes foreign keys and constraints

## Key Testing Patterns

### 1. AAA Pattern (Arrange-Act-Assert)

```java
@Test
void exampleTest() {
    // Arrange - Set up test data
    CreateNewsRequest request = new CreateNewsRequest();
    request.setNewsLink("https://example.com/test");

    // Act - Execute the operation
    ResponseEntity<NewsResponse> response = restTemplate.postForEntity(
        baseUrl + "/api/v1/news", request, NewsResponse.class
    );

    // Assert - Verify the results
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getNewsLink()).isEqualTo("https://example.com/test");
}
```

### 2. Database Verification

Always verify that operations actually persisted to the database:

```java
// After creating/updating data, verify in database
Integer count = jdbcTemplate.queryForObject(
    "SELECT COUNT(*) FROM News WHERE NewsLink = ?",
    Integer.class,
    newsLink
);
assertThat(count).isEqualTo(1);
```

### 3. Cleanup

Each test class uses `@AfterEach` to clean up test data:

```java
@AfterEach
void tearDown() {
    cleanDatabase(); // Removes test data to prevent interference
}
```

### 4. Authentication Testing

Use `@WithMockUser` for authenticated endpoints:

```java
@Test
@WithMockUser(username = "testuser", roles = {"USER"})
void authenticatedTest() {
    // This test runs with a mocked authenticated user
}
```

## Test Coverage

### What Integration Tests Cover

✅ **HTTP Request/Response Flow**: Full request handling from client to server
✅ **Data Persistence**: Verify data is correctly saved/retrieved from database
✅ **Business Logic**: Service layer operations with actual dependencies
✅ **Validation**: Input validation and error handling
✅ **Security**: Authentication and authorization
✅ **Database Transactions**: Multi-step operations with rollback scenarios

### What Integration Tests Don't Cover

❌ **External APIs**: Mock API calls (WTO, WITS, Gemini, etc.)
❌ **Email Sending**: Mock SMTP configuration
❌ **Frontend**: UI interactions (use E2E tests for this)
❌ **Production Infrastructure**: Load balancing, caching, etc.

## Best Practices

### 1. Test Isolation

Each test should be independent:
- Use `@AfterEach` to clean up data
- Don't rely on test execution order
- Create fresh test data for each test

### 2. Descriptive Test Names

Use clear, descriptive naming:
```java
@Test
void createNews_withValidData_returnsCreatedNews() { }  // ✅ Good

@Test
void testCreate() { }  // ❌ Bad
```

### 3. Test Data Management

- Use realistic but simple test data
- Avoid hardcoded IDs that might conflict
- Clean up after each test

### 4. Assertions

Use AssertJ for readable assertions:
```java
// Good - Fluent and readable
assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
assertThat(response.getBody()).isNotNull();
assertThat(response.getBody().getNewsLink()).isEqualTo(expected);

// Avoid - Less readable
assertTrue(response.getStatusCode() == 200);
assertNotNull(response.getBody());
```

### 5. Error Testing

Test both success and failure scenarios:
```java
@Test
void createNews_withDuplicateLink_returnsConflict() {
    // Test that duplicate keys are properly handled
}

@Test
void getTariff_withMissingParameters_returnsBadRequest() {
    // Test parameter validation
}
```

## Troubleshooting

### Common Issues

#### 1. Tests Fail Due to Schema Issues

**Problem**: `Table 'XYZ' doesn't exist`

**Solution**: Ensure `schema.sql` is being executed. Check `@Sql` annotation in `BaseIntegrationTest`.

#### 2. Port Already in Use

**Problem**: `Port 8080 is already in use`

**Solution**: Tests use `RANDOM_PORT`, so this shouldn't happen. Check if another instance is running.

#### 3. Authentication Failures

**Problem**: `401 Unauthorized` when using `@WithMockUser`

**Solution**: Ensure Spring Security test dependency is present and `@WithMockUser` is correctly configured.

#### 4. H2 Database Syntax Errors

**Problem**: SQL syntax not compatible with H2

**Solution**: Use H2 MySQL mode: `jdbc:h2:mem:testdb;MODE=MySQL`

#### 5. Test Data Conflicts

**Problem**: Tests fail intermittently due to data conflicts

**Solution**: Ensure `@AfterEach` cleanup is working and tests aren't sharing data.

### Debug Tips

1. **Enable SQL Logging**: Already enabled in `application-test.properties`
   ```properties
   logging.level.org.springframework.jdbc.core=DEBUG
   ```

2. **Check H2 Console**: Enable in test properties
   ```properties
   spring.h2.console.enabled=true
   ```

3. **Use Breakpoints**: Set breakpoints in test code and step through execution

4. **Isolate Failing Tests**: Run single tests to identify issues
   ```bash
   mvn test -Dtest=ClassName#methodName
   ```

## Adding New Integration Tests

### Step 1: Create Test Class

```java
package integration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class YourControllerIntegrationTest extends BaseIntegrationTest {

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void yourTest_description_expectedResult() {
        // Test implementation
    }
}
```

### Step 2: Set Up Test Data

Use `@BeforeEach` if you need setup common to all tests:

```java
@BeforeEach
void setUp() {
    jdbcTemplate.update("INSERT INTO YourTable ...");
}
```

### Step 3: Write Tests

Follow the AAA pattern and best practices listed above.

### Step 4: Verify Coverage

Run tests and check that your code paths are covered:

```bash
mvn clean test jacoco:report
```

View report at: `target/site/jacoco/index.html`

## Continuous Integration

### GitHub Actions / CI Pipeline

Integration tests should run on every pull request:

```yaml
# .github/workflows/test.yml
- name: Run Integration Tests
  run: mvn test -Dtest="integration.**"
```

### Test Execution Time

- Total: ~2-3 minutes for all 40 integration tests
- Individual test: ~50-200ms average
- Optimize by running tests in parallel if needed

## Further Reading

- [Spring Boot Testing Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [TestRestTemplate Guide](https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/test/web/client/TestRestTemplate.html)

## Summary

Integration tests are crucial for ensuring your application components work together correctly. This test suite provides:

- ✅ 40 comprehensive integration tests across 4 major components
- ✅ Full HTTP request/response testing
- ✅ Database persistence verification
- ✅ Authentication and authorization testing
- ✅ Error handling and edge case coverage
- ✅ Isolated, repeatable tests with H2 in-memory database

Keep tests isolated, descriptive, and maintainable. Happy testing! 🎉
