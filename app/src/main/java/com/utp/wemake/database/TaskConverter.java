package com.utp.wemake.database;

import com.utp.wemake.database.entities.SubtaskEntity;
import com.utp.wemake.database.entities.TaskEntity;
import com.utp.wemake.models.Subtask;
import com.utp.wemake.models.TaskModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class TaskConverter {

    // Convertir TaskModel a TaskEntity
    public static TaskEntity toEntity(TaskModel taskModel) {
        TaskEntity entity = new TaskEntity();
        entity.setId(taskModel.getId());
        entity.setTitle(taskModel.getTitle());
        entity.setDescription(taskModel.getDescription());
        entity.setDeadline(taskModel.getDeadline() != null ? taskModel.getDeadline().getTime() : null);
        entity.setPriority(taskModel.getPriority());
        entity.setBoardId(taskModel.getBoardId());
        entity.setCreatedBy(taskModel.getCreatedBy());
        entity.setCreatedAt(taskModel.getCreatedAt() != null ? taskModel.getCreatedAt().getTime() : null);
        entity.setStatus(taskModel.getStatus());
        entity.setRewardPoints(taskModel.getRewardPoints());
        entity.setPenaltyPoints(taskModel.getPenaltyPoints());
        entity.setApprovedBy(taskModel.getApprovedBy());
        entity.setApprovedAt(taskModel.getApprovedAt() != null ? taskModel.getApprovedAt().getTime() : null);
        entity.setReviewerId(taskModel.getReviewerId());

        // Convertir List<String> a JSON
        if (taskModel.getAssignedMembers() != null) {
            Converters converters = new Converters();
            entity.setAssignedMembersJson(converters.fromStringList(taskModel.getAssignedMembers()));
        }

        entity.setCompletedAt(taskModel.getCompletedAt() != null ? taskModel.getCompletedAt().getTime() : null);
        entity.setPenaltyApplied(taskModel.isPenaltyApplied());
        entity.setLastModified(System.currentTimeMillis());

        // Convertir subtareas
        if (taskModel.getSubtasks() != null) {
            List<SubtaskEntity> subtaskEntities = new ArrayList<>();
            for (Subtask subtask : taskModel.getSubtasks()) {
                SubtaskEntity subtaskEntity = new SubtaskEntity();
                subtaskEntity.setId(subtask.getId() != null ? subtask.getId() : UUID.randomUUID().toString());
                subtaskEntity.setTaskId(taskModel.getId());
                subtaskEntity.setText(subtask.getText());
                subtaskEntity.setCompleted(subtask.isCompleted());
                subtaskEntity.setCompletedAt(subtask.getCompletedAt() != null ? subtask.getCompletedAt().getTime() : null);
                subtaskEntities.add(subtaskEntity);
            }
            entity.setSubtasks(subtaskEntities);
        }

        return entity;
    }

    // Convertir TaskEntity a TaskModel
    public static TaskModel toModel(TaskEntity entity, List<SubtaskEntity> subtasks) {
        TaskModel model = new TaskModel();
        model.setId(entity.getId());
        model.setTitle(entity.getTitle());
        model.setDescription(entity.getDescription());
        model.setDeadline(entity.getDeadline() != null ? new Date(entity.getDeadline()) : null);
        model.setPriority(entity.getPriority());
        model.setBoardId(entity.getBoardId());
        model.setCreatedBy(entity.getCreatedBy());
        model.setCreatedAt(entity.getCreatedAt() != null ? new Date(entity.getCreatedAt()) : null);
        model.setStatus(entity.getStatus());
        model.setRewardPoints(entity.getRewardPoints());
        model.setPenaltyPoints(entity.getPenaltyPoints());
        model.setApprovedBy(entity.getApprovedBy());
        model.setApprovedAt(entity.getApprovedAt() != null ? new Date(entity.getApprovedAt()) : null);
        model.setReviewerId(entity.getReviewerId());

        // Convertir JSON a List<String>
        if (entity.getAssignedMembersJson() != null) {
            Converters converters = new Converters();
            model.setAssignedMembers(converters.toStringList(entity.getAssignedMembersJson()));
        }

        model.setCompletedAt(entity.getCompletedAt() != null ? new Date(entity.getCompletedAt()) : null);
        model.setPenaltyApplied(entity.isPenaltyApplied());

        // Convertir subtareas
        if (subtasks != null) {
            List<Subtask> subtaskModels = new ArrayList<>();
            for (SubtaskEntity subtaskEntity : subtasks) {
                Subtask subtask = new Subtask(subtaskEntity.getText());
                subtask.setId(subtaskEntity.getId());
                subtask.setCompleted(subtaskEntity.isCompleted());
                subtask.setCompletedAt(subtaskEntity.getCompletedAt() != null ? new Date(subtaskEntity.getCompletedAt()) : null);
                subtaskModels.add(subtask);
            }
            model.setSubtasks(subtaskModels);
        }

        return model;
    }
}