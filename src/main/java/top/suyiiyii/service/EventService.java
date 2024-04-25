package top.suyiiyii.service;

import top.suyiiyii.models.Event;
import top.suyiiyii.su.orm.core.Session;

public class EventService {
    Session db;

    public EventService(Session db) {
        this.db = db;
    }

    public void insertEvent(int uid, String method, String subjectId, String ip, String UA, int createTime) {
        Event event = new Event();
        event.setUid(uid);
        event.setMethod(method);
        event.setSubjectId(subjectId);
        event.setIp(ip);
        event.setUA(UA);
        event.setCreateTime(createTime);
        db.insert(event);
    }
}
