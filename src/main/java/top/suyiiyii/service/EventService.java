package top.suyiiyii.service;

import top.suyiiyii.models.Event;

import java.util.List;

public interface EventService {
    void addEvent(Event event);

    List<Event> getEvents(int page, int size);
}
