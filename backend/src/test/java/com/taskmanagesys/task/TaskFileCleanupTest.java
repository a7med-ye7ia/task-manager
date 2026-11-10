package com.taskmanagesys.task;

import com.taskmanagesys.task.model.TaskFile;
import com.taskmanagesys.task.repository.TaskFileRepository;
import com.taskmanagesys.task.service.TaskFileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskFileCleanupTest {
    @Mock TaskFileRepository files;
    @Test void orphanCleanupRemovesDiskFilesNotInTheDatabase(@TempDir Path dir) throws Exception {
        Files.createDirectories(dir.resolve("task-files"));
        Files.writeString(dir.resolve("task-files/orphan.bin"), "orphan");
        TaskFileService service = new TaskFileService(dir.toString(), files, mock(com.taskmanagesys.task.repository.TaskRepository.class), mock(com.taskmanagesys.task.repository.SubtaskRepository.class), mock(com.taskmanagesys.task.repository.SubtaskAssigneeRepository.class), mock(com.taskmanagesys.user.repository.UserRepository.class), mock(com.taskmanagesys.task.service.TaskAccessService.class), mock(com.taskmanagesys.task.service.TaskActivityService.class));
        when(files.findAll()).thenReturn(List.of());
        service.cleanupOrphans();
        assertFalse(Files.exists(dir.resolve("task-files/orphan.bin")));
    }
}
