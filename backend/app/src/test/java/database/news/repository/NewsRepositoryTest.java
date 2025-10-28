package database.news.repository;

import database.news.entity.NewsEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

@JdbcTest
@Import(NewsRepository.class)
@ActiveProfiles("test")
class NewsRepositoryTest {

  @Autowired NewsRepository repo;

  @Test
  void create_and_get_delete_roundtrip() {
    // create
    repo.create("https://x", "hello");
    assertThat(repo.exists("https://x")).isTrue();

    // read
    NewsEntity e = repo.getNews("https://x");
    assertThat(e.getNewsLink()).isEqualTo("https://x");
    assertThat(e.getRemarks()).isEqualTo("hello");

    // delete
    int deleted = repo.delete("https://x");
    assertThat(deleted).isEqualTo(1);
    assertThat(repo.exists("https://x")).isFalse();
  }
}
