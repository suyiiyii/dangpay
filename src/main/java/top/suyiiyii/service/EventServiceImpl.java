package top.suyiiyii.service;

import top.suyiiyii.models.Event;
import top.suyiiyii.su.IOC.Repository;
import top.suyiiyii.su.orm.core.Session;

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
}
