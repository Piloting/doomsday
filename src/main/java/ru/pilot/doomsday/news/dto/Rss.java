package ru.pilot.doomsday.news.dto;

import java.util.List;

import com.rometools.rome.feed.WireFeed;
import com.rometools.rome.feed.atom.Feed;
import com.rometools.rome.feed.rss.Channel;
import com.rometools.rome.feed.rss.Item;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Rss {

    private final WireFeed wireFeed;

    public List<Item> getItems() {
        if (wireFeed instanceof Channel) {
            return ((Channel) wireFeed).getItems();
        } if (wireFeed instanceof Feed) {
            throw new RuntimeException("wireFeed instanceof Feed DETECTED!!");
            //return ((Feed) wireFeed).getEntries();
        } else {
            throw new RuntimeException("wireFeed unexpected");
        }
    }
}
