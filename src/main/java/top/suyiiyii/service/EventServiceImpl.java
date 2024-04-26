package top.suyiiyii.service;

import top.suyiiyii.models.Event;
import top.suyiiyii.su.IOC.Repository;
import top.suyiiyii.su.orm.core.Session;

import java.util.List;

@Repository
public class EventServiceImpl implements EventService {
    Session db;

    public EventServiceImpl(Session db) {
        this.db = db;
    }

    @Override
    public void addEvent(Event event) {
        db.insert(event);
    }

    @Override
    public List<Event> getEvents(int page, int size) {
        return db.query(Event.class).limit(page, size).orderBy("id", true).all();
    }
}
