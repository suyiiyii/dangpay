package top.suyiiyii.service;

import top.suyiiyii.models.Event;
import top.suyiiyii.su.IOC.Proxy;

import java.util.List;

@Proxy
public interface EventService {
    void addEvent(Event event);

    List<Event> getEvents(int page, int size);
}
