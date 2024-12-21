package ru.pilot.doomsday.news.dto;

/**
 * Ошибки при загрузке RSS, при которых не нужно останавливать процесс,
 * делаем пропуск ссылки, по которой грузили RSS.
 */
public class SkipException extends RuntimeException {
    public SkipException(String message) {
        super(message);
    }
}
