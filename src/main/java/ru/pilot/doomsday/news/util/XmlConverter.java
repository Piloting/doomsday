package ru.pilot.doomsday.news.util;

import java.io.StringReader;

import com.rometools.rome.feed.WireFeed;
import com.rometools.rome.io.WireFeedInput;
import ru.pilot.doomsday.news.dto.Rss;

public class XmlConverter {

    /**
     * Преобразование строки xml к rss dto из com.rometools.rome
     */
    public static Rss xmlToDto(String xml) {
        try {
            WireFeedInput input = new WireFeedInput();
            WireFeed wireFeed = input.build(new StringReader(xml));
            return new Rss(wireFeed);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
