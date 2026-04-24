package com.omnisource.draft.task;

public interface TaskManager {

    String submit(Object payload);

    TaskRecord get(String taskId);
}
