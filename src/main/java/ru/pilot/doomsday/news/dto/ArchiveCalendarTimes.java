package ru.pilot.doomsday.news.dto;

import java.util.List;

import lombok.Data;

/**
 * Dto для данных с archive.org
 */
@Data
public class ArchiveCalendarTimes {
    public List<List<String>> colls;
    public List<List<Object>> items;
}
