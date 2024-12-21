package ru.pilot.doomsday.news;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.pilot.doomsday.news.service.DictionaryService;

/**
 * Загрузка заголовков новостей из RSS (в т.ч. исторических), сохранение в БД,
 * Подсчет разных метрик на основе слов этих заголовков
 */
@SpringBootApplication
public class DoomsdayNewsApplication {

	public static void main(String[] args) {
		SpringApplication.run(DoomsdayNewsApplication.class, args);
		DictionaryService.load();
	}

}
