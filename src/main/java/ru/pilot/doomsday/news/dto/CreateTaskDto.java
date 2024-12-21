package ru.pilot.doomsday.news.dto;

import java.time.LocalDate;

public record CreateTaskDto (LocalDate from, LocalDate to){}
