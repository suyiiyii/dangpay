package top.suyiiyii.service;

import top.suyiiyii.models.Event;
import top.suyiiyii.su.orm.core.Session;

public class EventService {
    Session db;

    public EventService(Session db) {
        this.db = db;
    }

    public void addEvent(Event event) {
        db.insert(event);
    }
}
