package database.news.controller;

import database.news.service.NewsService;
import database.news.dto.NewsResponse; // adjust if you have

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = NewsController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class NewsControllerTest {

  @Autowired MockMvc mvc;
  @MockBean NewsService newsService;

  @Test
  void createNews_returns201() throws Exception {
    Mockito.when(newsService.createNews(Mockito.any()))
           .thenReturn(new NewsResponse("https://link", "ok"));

    mvc.perform(post("/api/v1/news")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"newsLink\":\"https://link\",\"remarks\":\"ok\"}"))
       .andExpect(status().isCreated())
       .andExpect(jsonPath("$.newsLink").value("https://link"));
  }

  @Test
  void getNews_notFound() throws Exception {
    Mockito.when(newsService.getNews("missing"))
           .thenThrow(new database.news.exception.NewsNotFoundException("missing"));

    mvc.perform(get("/api/v1/news/{newsLink}", "missing"))
       .andExpect(status().isNotFound());
  }
}
