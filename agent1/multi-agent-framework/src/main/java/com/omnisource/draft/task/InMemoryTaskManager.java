package com.omnisource.draft.task;

import com.omnisource.draft.model.TaskStatus;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTaskManager implements TaskManager {

    private final Map<String, TaskRecord> store = new ConcurrentHashMap<>();

    @Override
    public String submit(Object payload) {
        String taskId = "task_" + UUID.randomUUID();

        TaskRecord record = new TaskRecord();
        record.setTaskId(taskId);
        record.setStatus(TaskStatus.PENDING);
        record.setResult(payload);
        store.put(taskId, record);

        return taskId;
    }

    @Override
    public TaskRecord get(String taskId) {
        return store.get(taskId);
    }
}
