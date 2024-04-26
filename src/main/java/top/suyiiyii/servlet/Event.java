package top.suyiiyii.servlet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.suyiiyii.service.EventService;

import java.util.List;

public class Event {
    private final EventService eventService;

    public Event(EventService eventService) {
        this.eventService = eventService;
    }


    public List<top.suyiiyii.models.Event> doGet(HttpServletRequest req, HttpServletResponse resp) {
        int page = Integer.parseInt(req.getParameter("page"));
        int size = Integer.parseInt(req.getParameter("size"));
        return eventService.getEvents(page, size);
    }
}
