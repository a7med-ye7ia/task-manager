package com.taskmanagesys.task;

import com.taskmanagesys.common.error.AppException;
import com.taskmanagesys.task.model.Task;
import com.taskmanagesys.task.model.TaskFile;
import com.taskmanagesys.task.model.Subtask;
import com.taskmanagesys.task.model.TaskStatus;
import com.taskmanagesys.task.repository.SubtaskRepository;
import com.taskmanagesys.task.repository.SubtaskAssigneeRepository;
import com.taskmanagesys.task.repository.TaskFileRepository;
import com.taskmanagesys.task.repository.TaskRepository;
import com.taskmanagesys.task.service.TaskAccessService;
import com.taskmanagesys.task.service.TaskActivityService;
import com.taskmanagesys.task.service.TaskFileService;
import com.taskmanagesys.user.model.User;
import com.taskmanagesys.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.internal.matchers.Null;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskFileServiceTest {
    @Mock
    TaskFileRepository files;
    @Mock
    TaskRepository tasks;
    @Mock
    SubtaskRepository subtasks;
    @Mock
    SubtaskAssigneeRepository assignees;
    @Mock
    UserRepository users;
    @Mock
    TaskAccessService access;
    @Mock
    TaskActivityService activity;

    private TaskFileService service(Path dir, TaskStatus status) {
        Task task = new Task();
        task.setId(10L);
        task.setTitle("Task");
        task.setStatus(status);
        when(tasks.findById(10L)).thenReturn(Optional.of(task));
        doNothing().when(access).requireMember(10L, 2L);
        User user = new User();
        user.setId(2L);
        user.setFirstName("A");
        user.setLastName("User");
        when(users.findById(2L)).thenReturn(Optional.of(user));
        when(files.save(any())).thenAnswer(inv -> inv.getArgument(0));
        return new TaskFileService(dir.toString(), files, tasks, subtasks, assignees, users, access, activity);
    }

    @Test
    void allowedSignaturesAreAccepted(@TempDir Path dir) {
        TaskFileService service = service(dir, TaskStatus.IN_PROGRESS);
        List<MultipartFile> uploads = List.of(
                file("a.jpg", new byte[] { (byte) 255, (byte) 216, (byte) 255, 1 }),
                file("b.png", new byte[] { (byte) 137, 80, 78, 71, 13, 10, 26, 10, 1 }),
                file("c.webp", bytes("RIFF1234WEBP")), file("d.pdf", bytes("%PDF-1.7")),
                file("e.txt", bytes("plain text")),
                file("f.csv", bytes("a,b\n1,2")), file("g.zip", zip(null)), file("h.docx", zip("word/")),
                file("i.xlsx", zip("xl/")), file("j.pptx", zip("ppt/")));
        when(files.countByTaskId(10L)).thenReturn(0L);
        when(files.findByTaskId(10L)).thenReturn(List.of());
        assertEquals(10, service.upload(10L, null, 2L, uploads).size());
    }

    @Test
    void renamedExecutableHtmlAndSvgAreRejected() {
        TaskFileService service = service(java.nio.file.Paths.get("target", "task-file-test"), TaskStatus.IN_PROGRESS);
        when(files.countByTaskId(10L)).thenReturn(0L);
        when(files.findByTaskId(10L)).thenReturn(List.of());
        assertThrows(AppException.class,
                () -> service.upload(10L, null, 2L, List.<MultipartFile>of(file("photo.jpg", bytes("MZ executable")))));
        assertThrows(AppException.class,
                () -> service.upload(10L, null, 2L, List.<MultipartFile>of(file("notes.txt", bytes("<html><script>bad</script>")))));
        assertThrows(AppException.class,
                () -> service.upload(10L, null, 2L, List.<MultipartFile>of(file("vector.svg", bytes("<svg></svg>")))));
    }

    @Test
    void detectsRealTypesIndependentOfDeclaredContentTypeAndReportsMismatches() {
        TaskFileService target = service(java.nio.file.Paths.get("target", "task-file-detection-test"), TaskStatus.IN_PROGRESS);
        when(files.countByTaskId(10L)).thenReturn(0L); when(files.findByTaskId(10L)).thenReturn(List.of());
        byte[] png = new byte[] {(byte)137,80,78,71,13,10,26,10,1};
        assertEquals("image/png", target.upload(10L, null, 2L, List.of(file("koko with shrimp sowrd.png", png))).get(0).contentType());
        AppException mismatch = assertThrows(AppException.class, () -> target.upload(10L, null, 2L, List.of(file("photo.png", new byte[] {(byte)255,(byte)216,(byte)255,1}))));
        assertTrue(mismatch.getMessage().contains("JPEG") && mismatch.getMessage().contains(".png"));
        assertThrows(AppException.class, () -> target.upload(10L, null, 2L, List.of(file("empty.png", new byte[0]))));
        assertThrows(AppException.class, () -> target.upload(10L, null, 2L, List.of(file("short.png", new byte[] {(byte)137,80,78,71}))));
    }

    @Test
    void sizeAndCountLimitsAreEnforced() {
        TaskFileService service = service(java.nio.file.Paths.get("target", "task-file-limit-test"),
                TaskStatus.IN_PROGRESS);
        when(files.countByTaskId(10L)).thenReturn(30L);
        when(files.findByTaskId(10L)).thenReturn(List.of());
        AppException count = assertThrows(AppException.class,
                () -> service.upload(10L, null, 2L, List.<MultipartFile>of(file("a.txt", bytes("x")))));
        assertEquals(HttpStatus.BAD_REQUEST, count.getStatus());
        when(files.countByTaskId(10L)).thenReturn(0L);
        when(files.findByTaskId(10L)).thenReturn(List.of());
        byte[] large = new byte[10 * 1024 * 1024 + 1];
        AppException size = assertThrows(AppException.class,
                () -> service.upload(10L, null, 2L, List.<MultipartFile>of(file("a.txt", large))));
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, size.getStatus());
    }

    @Test
    void outsiderCannotListOrUploadAndDoneTaskIsReadOnly() {
        TaskFileService service = service(java.nio.file.Paths.get("target", "task-file-permission-test"),
                TaskStatus.IN_PROGRESS);
        when(access.manager(2L)).thenReturn(false);
        when(access.lead(10L, 2L)).thenReturn(false);
        when(access.member(10L, 2L)).thenReturn(false);
        doThrow(new AppException(HttpStatus.FORBIDDEN, "TASK_ACCESS_DENIED", "denied")).when(access).requireMember(10L,
                2L);
        assertThrows(AppException.class, () -> service.list(10L, 2L));
        assertThrows(AppException.class, () -> service.upload(10L, null, 2L, List.<MultipartFile>of(file("a.txt", bytes("x")))));
        TaskFileService done = service(java.nio.file.Paths.get("target", "task-file-done-test"), TaskStatus.DONE);
        assertThrows(AppException.class, () -> done.upload(10L, null, 2L, List.<MultipartFile>of(file("a.txt", bytes("x")))));
    }

    @Test
    void memberOnlySeesTaskFilesAndAssignedSubtaskFiles(@TempDir Path dir) {
        TaskFileService service = service(dir, TaskStatus.IN_PROGRESS);
        TaskFile taskFile = new TaskFile();
        taskFile.setId(1L); taskFile.setTaskId(10L); taskFile.setOriginalName("task.pdf"); taskFile.setUploaderId(2L);
        TaskFile assignedSubtaskFile = new TaskFile();
        assignedSubtaskFile.setId(2L); assignedSubtaskFile.setTaskId(10L); assignedSubtaskFile.setSubtaskId(20L); assignedSubtaskFile.setOriginalName("assigned.pdf"); assignedSubtaskFile.setUploaderId(2L);
        TaskFile unassignedSubtaskFile = new TaskFile();
        unassignedSubtaskFile.setId(3L); unassignedSubtaskFile.setTaskId(10L); unassignedSubtaskFile.setSubtaskId(30L); unassignedSubtaskFile.setOriginalName("unassigned.pdf"); unassignedSubtaskFile.setUploaderId(2L);

        when(files.findByTaskIdOrderBySubtaskIdAscCreatedAtAsc(10L)).thenReturn(List.of(taskFile, assignedSubtaskFile, unassignedSubtaskFile));
        when(access.manager(2L)).thenReturn(false);
        when(access.lead(10L, 2L)).thenReturn(false);
        when(access.member(10L, 2L)).thenReturn(true);
        when(assignees.existsBySubtaskIdAndUserId(20L, 2L)).thenReturn(true);
        when(assignees.existsBySubtaskIdAndUserId(30L, 2L)).thenReturn(false);

        List<com.taskmanagesys.task.dto.TaskFileResponse> visible = service.list(10L, 2L);
        assertEquals(2, visible.size());
        assertEquals(1L, visible.get(0).id());
        assertEquals(2L, visible.get(1).id());
    }

    @Test void subtaskMultipartUploadStoresSubtaskIdAndIsReturnedOnFirstList(@TempDir Path dir) {
        Task task = new Task(); task.setId(10L); task.setStatus(TaskStatus.IN_PROGRESS); Subtask subtask = new Subtask(); subtask.setId(3L); subtask.setTask(task);
        when(tasks.findById(10L)).thenReturn(Optional.of(task)); when(subtasks.findById(3L)).thenReturn(Optional.of(subtask)); when(files.countByTaskId(10L)).thenReturn(0L); when(files.findByTaskId(10L)).thenReturn(List.of()); when(files.save(any())).thenAnswer(i -> i.getArgument(0)); doNothing().when(access).requireSubtaskAssigneeOrLead(10L,3L,2L);
        TaskFileService target = service(dir, TaskStatus.IN_PROGRESS); assertEquals(1, target.upload(10L,3L,2L,List.of(file("sub.txt",bytes("sub")))).size());
    }

    @Test void metadataRejectsFileFromAnotherTaskAsNotFound(@TempDir Path dir) {
        TaskFileService target = service(dir, TaskStatus.IN_PROGRESS);
        TaskFile file = new TaskFile(); file.setId(7L); file.setTaskId(99L); file.setOriginalName("other.txt"); file.setUploaderId(2L);
        when(files.findById(7L)).thenReturn(Optional.of(file));
        AppException error = assertThrows(AppException.class, () -> target.detail(10L, 7L, 2L, null));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
    }

    @Test void newVideoAndTextTypesAreAccepted(@TempDir Path dir) {
        when(files.countByTaskId(10L)).thenReturn(0L); when(files.findByTaskId(10L)).thenReturn(List.of());
        byte[] mp4 = new byte[]{0,0,0,24,'f','t','y','p',0,0,0,0,0}; byte[] webm = new byte[]{0x1a,0x45,(byte)0xdf,(byte)0xa3,1};
        assertEquals(4, service(dir,TaskStatus.IN_PROGRESS).upload(10L,null,2L,List.of(file("a.mp4",mp4),file("b.webm",webm),file("c.tex",bytes("\\section{}")),file("d.md",bytes("# doc")))).size());
    }

    @Test void unicodeNamesAndNewTextTypesAreAccepted(@TempDir Path dir) {
        when(files.countByTaskId(10L)).thenReturn(0L); when(files.findByTaskId(10L)).thenReturn(List.of());
        TaskFileService target = service(dir, TaskStatus.IN_PROGRESS);
        var saved = target.upload(10L, null, 2L, List.of(file("تقرير العمل 2026.md", bytes("# مرحبا")), file("query.SQL", bytes("select 1;")), file("config.yml", bytes("name: demo"))));
        assertEquals("تقرير العمل 2026.md", saved.get(0).originalName());
        assertEquals("text/plain", saved.get(1).contentType());
        assertEquals("text/plain", saved.get(2).contentType());
    }

    @Test void htmlAndInvalidUtf8TextAreRejected(@TempDir Path dir) {
        when(files.countByTaskId(10L)).thenReturn(0L); when(files.findByTaskId(10L)).thenReturn(List.of());
        TaskFileService target = service(dir, TaskStatus.IN_PROGRESS);
        assertThrows(AppException.class, () -> target.upload(10L, null, 2L, List.of(file("page.md", bytes("<!doctype html><script>bad</script>")))));
        assertThrows(AppException.class, () -> target.upload(10L, null, 2L, List.of(file("broken.json", new byte[]{(byte) 0xc3, (byte) 0x28}))));
    }

    @Test void uploaderCanEditTextAndDoneTaskCannot(@TempDir Path dir) throws Exception {
        TaskFileService target = service(dir, TaskStatus.IN_PROGRESS);
        TaskFile file = new TaskFile(); file.setId(8L); file.setTaskId(10L); file.setUploaderId(2L); file.setOriginalName("notes.md"); file.setContentType("text/plain"); file.setStoredName("notes.md"); file.setUpdatedAt(java.time.Instant.parse("2026-01-01T00:00:00Z"));
        when(files.findById(8L)).thenReturn(Optional.of(file)); when(files.save(any())).thenAnswer(inv -> inv.getArgument(0));
        java.nio.file.Files.write(dir.resolve("task-files").resolve("notes.md"), bytes("old"));
        var saved = target.editText(10L, 8L, 2L, "new", file.getUpdatedAt().toString());
        assertEquals(3, saved.sizeBytes()); assertEquals("new", java.nio.file.Files.readString(dir.resolve("task-files").resolve("notes.md")));
        TaskFileService done = service(dir, TaskStatus.DONE);
        when(files.findById(8L)).thenReturn(Optional.of(file));
        assertThrows(AppException.class, () -> done.editText(10L, 8L, 2L, "later", file.getUpdatedAt().toString()));
    }

    @Test void staleVersionAndOneMbLimitAreRejected(@TempDir Path dir) throws Exception {
        TaskFileService target = service(dir, TaskStatus.IN_PROGRESS);
        TaskFile file = new TaskFile(); file.setId(9L); file.setTaskId(10L); file.setUploaderId(2L); file.setOriginalName("notes.txt"); file.setContentType("text/plain"); file.setStoredName("notes.txt"); file.setUpdatedAt(java.time.Instant.parse("2026-01-01T00:00:00Z"));
        when(files.findById(9L)).thenReturn(Optional.of(file));
        assertEquals(HttpStatus.CONFLICT, assertThrows(AppException.class, () -> target.editText(10L, 9L, 2L, "changed", "2025-01-01T00:00:00Z")).getStatus());
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, assertThrows(AppException.class, () -> target.editText(10L, 9L, 2L, "x".repeat(1024 * 1024 + 1), file.getUpdatedAt().toString())).getStatus());
    }

    @Test void zipEntriesAreBoundedAndTraversalNamesAreSanitized(@TempDir Path dir) throws Exception {
        TaskFileService target = service(dir, TaskStatus.IN_PROGRESS);
        Path storage = dir.resolve("task-files");
        java.nio.file.Files.createDirectories(storage);
        Path archive = storage.resolve("archive.zip");
        try (ZipOutputStream zip = new ZipOutputStream(java.nio.file.Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("../secret.txt"));
            zip.write('x');
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("docs/readme.txt"));
            zip.write('y');
            zip.closeEntry();
        }
        TaskFile file = new TaskFile();
        file.setId(7L); file.setTaskId(10L); file.setStoredName("archive.zip"); file.setContentType("application/zip");
        when(files.findById(7L)).thenReturn(Optional.of(file));
        List<TaskFileService.ZipEntryInfo> entries = target.zipEntries(10L, 7L, 2L);
        assertEquals(2, entries.size());
        assertTrue(entries.get(0).name().contains("sanitized"));
    }

    @Test
    void renamePermissionsAllowUploaderLeadAndManager(@TempDir Path dir) {
        TaskFileService target = service(dir, TaskStatus.IN_PROGRESS);
        TaskFile file = new TaskFile();
        file.setId(5L);
        file.setTaskId(10L);
        file.setUploaderId(2L);
        file.setOriginalName("report.pdf");
        when(files.findById(5L)).thenReturn(Optional.of(file));
        when(files.save(any(TaskFile.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1. Uploader (id = 2) can rename
        var respUploader = target.rename(10L, 5L, 2L, "new_report");
        assertEquals("new_report.pdf", respUploader.originalName());
        verify(activity).logActivity(eq(10L), eq(2L), eq("FILE_RENAMED"), any());

        // 2. Lead (id = 3) can rename
        when(access.managerOrLead(10L, 3L)).thenReturn(true);
        var respLead = target.rename(10L, 5L, 3L, "lead_report");
        assertEquals("lead_report.pdf", respLead.originalName());

        // 3. Manager (id = 4) can rename
        when(access.managerOrLead(10L, 4L)).thenReturn(true);
        var respManager = target.rename(10L, 5L, 4L, "manager_report");
        assertEquals("manager_report.pdf", respManager.originalName());
    }

    @Test
    void renamePermissionsDenyNonUploaderMemberAndOutsiderWith403(@TempDir Path dir) {
        TaskFileService target = service(dir, TaskStatus.IN_PROGRESS);
        TaskFile file = new TaskFile();
        file.setId(5L);
        file.setTaskId(10L);
        file.setUploaderId(2L);
        file.setOriginalName("report.pdf");
        when(files.findById(5L)).thenReturn(Optional.of(file));

        // Non-uploader member / outsider (id = 8, not uploader, not lead/manager)
        when(access.managerOrLead(10L, 8L)).thenReturn(false);
        AppException ex = assertThrows(AppException.class, () -> target.rename(10L, 5L, 8L, "renamed"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void renameOnDoneTaskFailsWith403(@TempDir Path dir) {
        TaskFileService target = service(dir, TaskStatus.DONE);
        TaskFile file = new TaskFile();
        file.setId(5L);
        file.setTaskId(10L);
        file.setUploaderId(2L);
        file.setOriginalName("report.pdf");
        when(files.findById(5L)).thenReturn(Optional.of(file));

        AppException ex = assertThrows(AppException.class, () -> target.rename(10L, 5L, 2L, "renamed"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void renamePreservesExtensionAndKeepsArabicNamesAndWritesActivity(@TempDir Path dir) {
        TaskFileService target = service(dir, TaskStatus.IN_PROGRESS);
        TaskFile file = new TaskFile();
        file.setId(5L);
        file.setTaskId(10L);
        file.setUploaderId(2L);
        file.setOriginalName("old_document.docx");
        when(files.findById(5L)).thenReturn(Optional.of(file));
        when(files.save(any(TaskFile.class))).thenAnswer(inv -> inv.getArgument(0));

        // Arabic name without extension requested
        String arabicName = "تقرير_المشروع_الجديد";
        var resp = target.rename(10L, 5L, 2L, arabicName);

        // Extension is kept and Arabic characters survive
        assertEquals("تقرير_المشروع_الجديد.docx", resp.originalName());
        verify(activity).logActivity(eq(10L), eq(2L), eq("FILE_RENAMED"), argThat(map ->
            "تقرير_المشروع_الجديد.docx".equals(map.get("name"))
        ));
    }

    private static MockMultipartFile file(String name, byte[] bytes) {
        return new MockMultipartFile("files", name, "application/octet-stream", bytes);
    }

    private static byte[] bytes(String value) {
        return value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static byte[] zip(String family) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ZipOutputStream zip = new ZipOutputStream(output);
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write('<');
            zip.closeEntry();
            if (family != null) {
                zip.putNextEntry(new ZipEntry(family + "document.xml"));
                zip.write('<');
                zip.closeEntry();
            }
            zip.close();
            return output.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
