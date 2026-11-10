package com.taskmanagesys.task.service;

import com.taskmanagesys.common.error.AppException;
import com.taskmanagesys.task.dto.TaskFileResponse;
import com.taskmanagesys.task.dto.TaskFileDetailResponse;
import com.taskmanagesys.task.model.Task;
import com.taskmanagesys.task.model.TaskFile;
import com.taskmanagesys.task.model.Subtask;
import com.taskmanagesys.task.repository.TaskFileRepository;
import com.taskmanagesys.task.repository.SubtaskRepository;
import com.taskmanagesys.task.repository.SubtaskAssigneeRepository;
import com.taskmanagesys.task.repository.TaskRepository;
import com.taskmanagesys.user.model.User;
import com.taskmanagesys.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.file.*;
import java.time.Instant;
import java.time.Clock;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

@Service
public class TaskFileService {
    private static final long MAX_FILE = 10 * 1024 * 1024;
    private static final long MAX_VIDEO_FILE = 50 * 1024 * 1024;
    private static final long MAX_TOTAL = 150 * 1024 * 1024;
    private static final long MAX_COUNT = 30;
    private static final int MAX_ZIP_ENTRIES = 1000;
    private static final long MAX_ZIP_ENTRY_SIZE = 150 * 1024 * 1024L;
    private final Path directory;
    private final TaskFileRepository files;
    private final TaskRepository tasks;
    private final SubtaskRepository subtasks;
    private final SubtaskAssigneeRepository assignees;
    private final UserRepository users;
    private final TaskAccessService access;
    private final TaskActivityService activity;
    private final FileViewTokenService viewTokens;

    @Autowired
    public TaskFileService(@Value("${app.upload-dir:./uploads}") String uploadDir, TaskFileRepository files,
            TaskRepository tasks,
            SubtaskRepository subtasks, SubtaskAssigneeRepository assignees, UserRepository users,
            TaskAccessService access, TaskActivityService activity, FileViewTokenService viewTokens) {
        this.directory = Paths.get(uploadDir, "task-files").toAbsolutePath().normalize();
        this.files = files;
        this.tasks = tasks;
        this.subtasks = subtasks;
        this.assignees = assignees;
        this.users = users;
        this.access = access;
        this.activity = activity;
        this.viewTokens = viewTokens;
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create task file directory", e);
        }
    }

    public TaskFileService(String uploadDir, TaskFileRepository files, TaskRepository tasks, SubtaskRepository subtasks,
            SubtaskAssigneeRepository assignees, UserRepository users, TaskAccessService access, TaskActivityService activity) {
        this(uploadDir, files, tasks, subtasks, assignees, users, access, activity,
                new FileViewTokenService("test-file-view-secret-that-is-at-least-32-bytes", Clock.systemUTC()));
    }

    public String issueViewToken(Long taskId, Long fileId, Long userId) {
        TaskFile file = file(fileId);
        if (!taskId.equals(file.getTaskId())) throw error(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "File not found");
        requireFileReader(taskId, file, userId);
        return viewTokens.issue(userId, taskId, fileId);
    }

    @Transactional(readOnly = true)
    public TaskFileDetailResponse detail(Long taskId, Long fileId, Long userId, Long subtaskFilter) {
        Task task = task(taskId);
        TaskFile file = file(fileId);
        if (!taskId.equals(file.getTaskId())) throw error(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "File not found");
        requireFileReader(taskId, file, userId);
        boolean managerOrLead = access.manager(userId) || access.lead(taskId, userId);
        boolean isMember = access.member(taskId, userId);
        List<TaskFile> rows = files.findByTaskIdOrderBySubtaskIdAscCreatedAtAsc(taskId).stream()
                .filter(f -> managerOrLead || (f.getSubtaskId() == null ? isMember : assignees.existsBySubtaskIdAndUserId(f.getSubtaskId(), userId)))
                .filter(f -> subtaskFilter == null || Objects.equals(f.getSubtaskId(), subtaskFilter))
                .toList();
        int index = rows.indexOf(file);
        String uploader = users.findById(file.getUploaderId()).map(u -> u.getFirstName() + " " + u.getLastName()).orElse("Unknown user");
        String subtaskTitle = file.getSubtaskId() == null ? null : subtasks.findById(file.getSubtaskId()).map(Subtask::getTitle).orElse(null);
        return new TaskFileDetailResponse(file.getId(), taskId, file.getSubtaskId(), task.getTitle(), subtaskTitle,
                file.getOriginalName(), file.getContentType(), file.getSizeBytes(), file.getUploaderId(), uploader,
                file.getCreatedAt(), file.getUpdatedAt(), file.getEditedBy(), index > 0 ? rows.get(index - 1).getId() : null, index + 1 < rows.size() ? rows.get(index + 1).getId() : null);
    }

    @Transactional(readOnly = true)
    public List<ZipEntryInfo> zipEntries(Long taskId, Long fileId, Long userId) {
        TaskFile f = authorized(taskId, fileId, userId);
        if (!"application/zip".equals(f.getContentType())) throw error(HttpStatus.BAD_REQUEST, "NOT_ZIP", "File is not a ZIP archive");
        List<ZipEntryInfo> out = new ArrayList<>();
        try (ZipFile zip = new ZipFile(safe(f.getStoredName()).toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements() && out.size() < MAX_ZIP_ENTRIES) {
                ZipEntry entry = entries.nextElement();
                long size = entry.getSize();
                if (size > MAX_ZIP_ENTRY_SIZE) throw error(HttpStatus.BAD_REQUEST, "ZIP_TOO_LARGE", "The ZIP contains an entry that is too large");
                String name = entry.getName().replace('\\', '/');
                if (name.startsWith("/") || name.contains("../") || name.equals("..") || name.contains("/..")) name = "[sanitized path]";
                out.add(new ZipEntryInfo(name, size));
            }
            return out;
        } catch (AppException e) { throw e; }
        catch (IOException e) { throw error(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "File is no longer available"); }
    }
    @Transactional(readOnly = true)
    public TextData textPreview(Long taskId, Long fileId, Long userId) {
        TaskFile f = authorized(taskId, fileId, userId);
        String ext = extension(f.getOriginalName());
        if (!textExtension(ext)) throw error(HttpStatus.BAD_REQUEST, "NOT_TEXT", "File is not a supported text file");
        try {
            byte[] bytes = Files.readAllBytes(safe(f.getStoredName()));
            boolean truncated = bytes.length > 1024 * 1024;
            return new TextData(new String(bytes, 0, Math.min(bytes.length, 1024 * 1024), StandardCharsets.UTF_8), truncated);
        } catch (IOException e) { throw error(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "File is no longer available"); }
    }
    private TaskFile authorized(Long taskId, Long fileId, Long userId){ TaskFile f=file(fileId); if(!taskId.equals(f.getTaskId())) throw error(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "File not found"); requireFileReader(taskId,f,userId); return f; }

    @Transactional(readOnly = true)
    public ViewData signedContent(Long taskId, Long fileId, String token) {
        long userId = viewTokens.verify(token, taskId, fileId);
        TaskFile file = file(fileId);
        if (!taskId.equals(file.getTaskId())) throw error(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "File not found");
        requireFileReader(taskId, file, userId);
        try { return new ViewData(Files.readAllBytes(safe(file.getStoredName())), file.getContentType(), file.getOriginalName(), inlineContentType(file.getContentType())); }
        catch (IOException e) { throw error(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "File is no longer available"); }
    }

    @Transactional(readOnly = true)
    public List<TaskFileResponse> list(Long taskId, Long userId) {
        Task task = task(taskId);
        List<TaskFile> rows = files.findByTaskIdOrderBySubtaskIdAscCreatedAtAsc(taskId);
        boolean managerOrLead = access.manager(userId) || access.lead(taskId, userId);
        boolean isMember = access.member(taskId, userId);
        if (!managerOrLead && !isMember)
            deny();
        return rows.stream().filter(file -> {
            if (managerOrLead) return true;
            if (file.getSubtaskId() == null) return isMember;
            return assignees.existsBySubtaskIdAndUserId(file.getSubtaskId(), userId);
        }).map(this::response).toList();
    }

    @Transactional
    public List<TaskFileResponse> upload(Long taskId, Long subtaskId, Long userId, List<MultipartFile> uploads) {
        Task task = task(taskId);
        if (subtaskId == null)
            requireWritable(task, userId);
        else if (task.getStatus().name().equals("DONE"))
            throw error(HttpStatus.CONFLICT, "TASK_ARCHIVED", "Archived tasks are read-only until restored");
        if (subtaskId != null) {
            Subtask subtask = subtask(subtaskId);
            if (!subtask.getTask().getId().equals(taskId))
                deny();
            access.requireSubtaskAssigneeOrLead(taskId, subtaskId, userId);
        }
        if (subtaskId == null)
            access.requireMember(taskId, userId);
        if (uploads == null || uploads.isEmpty())
            throw error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Choose at least one file");
        long existingCount = files.countByTaskId(taskId),
                existingBytes = files.findByTaskId(taskId).stream().mapToLong(TaskFile::getSizeBytes).sum();
        if (existingCount + uploads.size() > MAX_COUNT)
            throw error(HttpStatus.BAD_REQUEST, "FILE_LIMIT", "A task can contain at most 30 files");
        long incomingBytes = uploads.stream().mapToLong(MultipartFile::getSize).sum();
        if (existingBytes + incomingBytes > MAX_TOTAL)
            throw error(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_LIMIT", "A task can contain at most 150 MB of files");
        User uploader = users.findById(userId).orElseThrow();
        List<TaskFile> saved = new ArrayList<>();
        for (MultipartFile upload : uploads)
            saved.add(saveOne(taskId, subtaskId, userId, uploader, upload));
        activity.logActivity(taskId, userId, "FILE_UPLOADED",
                Map.of("count", saved.size(), "subtaskId", String.valueOf(subtaskId)));
        return saved.stream().map(this::response).toList();
    }

    @Transactional
    public Download download(Long taskId, Long fileId, Long userId) {
        Task task = task(taskId);
        TaskFile file = file(fileId);
        if (!taskId.equals(file.getTaskId()))
            throw error(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "File not found");
        requireFileReader(taskId, file, userId);
        try {
            byte[] bytes = Files.readAllBytes(safe(file.getStoredName()));
            String type = inlineType(file.getContentType()) ? file.getContentType() : "application/octet-stream";
            return new Download(new ByteArrayResource(bytes), type, file.getOriginalName(),
                    inlineType(file.getContentType()));
        } catch (IOException e) {
            throw error(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "File is no longer available");
        }
    }

    @Transactional
    public void delete(Long taskId, Long fileId, Long userId) {
        Task task = task(taskId);
        TaskFile file = file(fileId);
        if (!taskId.equals(file.getTaskId()))
            throw error(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "File not found");
        if (task.getStatus().name().equals("DONE"))
            throw error(HttpStatus.CONFLICT, "TASK_ARCHIVED", "Archived tasks are read-only until restored");
        if (!access.managerOrLead(taskId, userId) && !Objects.equals(file.getUploaderId(), userId))
            deny();
        if (file.getSubtaskId() == null)
            access.requireMember(taskId, userId);
        else if (!access.managerOrLead(taskId, userId)
                && !assignees.existsBySubtaskIdAndUserId(file.getSubtaskId(), userId))
            deny();
        deleteDisk(file);
        files.delete(file);
        activity.logActivity(taskId, userId, "FILE_DELETED", Map.of("fileId", fileId));
    }

    @Transactional
    public TaskFileResponse rename(Long taskId, Long fileId, Long userId, String requested) {
        Task task = task(taskId);
        TaskFile file = file(fileId);
        if (!taskId.equals(file.getTaskId())) throw error(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "File not found");
        if (task.getStatus().name().equals("DONE"))
            throw error(HttpStatus.FORBIDDEN, "TASK_ARCHIVED", "Archived tasks are read-only until restored");
        if (!access.managerOrLead(taskId, userId) && !Objects.equals(file.getUploaderId(), userId)) deny();
        if (requested == null || requested.trim().isEmpty())
            throw error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "The file name cannot be empty");
        String name = sanitize(requested.trim());
        if (name.length() < 1 || name.length() > 150)
            throw error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "The file name must be 1 to 150 characters");
        String extension = extension(file.getOriginalName());
        if (!extension.isBlank() && !name.toLowerCase(Locale.ROOT).endsWith("." + extension))
            name = name + "." + extension;
        file.setOriginalName(name);
        TaskFile saved = files.save(file);
        activity.logActivity(taskId, userId, "FILE_RENAMED", Map.of("fileId", fileId, "name", name));
        return response(saved);
    }

    @Transactional
    public void deleteForTask(Long taskId) {
        for (TaskFile f : files.findByTaskId(taskId))
            deleteDisk(f);
        files.deleteByTaskId(taskId);
    }

    @Transactional
    public void deleteForSubtask(Long subtaskId) {
        for (TaskFile f : files.findBySubtaskId(subtaskId))
            deleteDisk(f);
        files.deleteBySubtaskId(subtaskId);
    }

    @Transactional
    public long cleanupOrphans() {
        long count = 0;
        try (var stream = Files.list(directory)) {
            Set<String> live = files.findAll().stream().map(TaskFile::getStoredName)
                    .collect(java.util.stream.Collectors.toSet());
            for (Path p : stream.toList())
                if (Files.isRegularFile(p) && !live.contains(p.getFileName().toString())) {
                    Files.deleteIfExists(p);
                    count++;
                }
        } catch (IOException ignored) {
        }
        return count;
    }

    private TaskFile saveOne(Long taskId, Long subtaskId, Long userId, User uploader, MultipartFile upload) {
        if (upload == null || upload.isEmpty())
            throw error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Files cannot be empty");
        String original = sanitize(upload.getOriginalFilename());
        String ext = extension(original);
        long limit = Set.of("mp4", "webm").contains(ext) ? MAX_VIDEO_FILE : MAX_FILE;
        if (upload.getSize() > limit)
            throw error(HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE", Set.of("mp4", "webm").contains(ext) ? "Videos must be 50 MB or smaller" : "Each file must be 10 MB or smaller");
        byte[] bytes;
        try {
            bytes = upload.getBytes();
        } catch (IOException e) {
            throw error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Could not read file");
        }
        DetectedType detected = detectType(bytes, ext);
        if (detected == DetectedType.UNKNOWN)
            throw error(HttpStatus.BAD_REQUEST, "INVALID_FILE", "The file content is empty, incomplete, invalid UTF-8, or not an allowed file type");
        boolean compatibleExtension = ext.equals(detected.extension)
                || (detected == DetectedType.JPEG && Set.of("jpg", "jpeg").contains(ext))
                || (detected == DetectedType.TEXT && textExtension(ext))
                || (detected == DetectedType.JSON && "json".equals(ext));
        if (!ext.isBlank() && !compatibleExtension)
            throw error(HttpStatus.BAD_REQUEST, "INVALID_FILE", "This file is a " + detected.label + " but is named ." + ext);
        String contentType = detected.contentType;
        String stored = UUID.randomUUID() + "." + detected.extension;
        try {
            Files.write(safe(stored), bytes, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw error(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_WRITE_ERROR", "File could not be stored");
        }
        TaskFile file = new TaskFile();
        file.setTaskId(taskId);
        file.setSubtaskId(subtaskId);
        file.setUploaderId(userId);
        file.setStoredName(stored);
        file.setOriginalName(original);
        file.setContentType(contentType);
        file.setSizeBytes(bytes.length);
        file.setCreatedAt(Instant.now());
        file.setUpdatedAt(file.getCreatedAt());
        try {
            return files.save(file);
        } catch (RuntimeException e) {
            deleteDisk(file);
            throw e;
        }
    }

    private enum DetectedType {
        PNG("PNG", "png", "image/png"), JPEG("JPEG", "jpg", "image/jpeg"), WEBP("WebP", "webp", "image/webp"), PDF("PDF", "pdf", "application/pdf"), ZIP("ZIP", "zip", "application/zip"), DOCX("DOCX", "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"), XLSX("XLSX", "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), PPTX("PPTX", "pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"), MP4("MP4", "mp4", "video/mp4"), WEBM("WebM", "webm", "video/webm"), TEXT("text file", "txt", "text/plain"), JSON("JSON", "json", MediaType.APPLICATION_JSON_VALUE), UNKNOWN("unknown", "bin", MediaType.APPLICATION_OCTET_STREAM_VALUE);
        final String label, extension, contentType;
        DetectedType(String label, String extension, String contentType) { this.label = label; this.extension = extension; this.contentType = contentType; }
    }

    private DetectedType detectType(byte[] b, String ext) {
        if (b == null || b.length == 0) return DetectedType.UNKNOWN;
        if (b.length >= 8 && (b[0] & 255) == 137 && b[1] == 80 && b[2] == 78 && b[3] == 71 && b[4] == 13 && b[5] == 10 && b[6] == 26 && b[7] == 10) return DetectedType.PNG;
        if (b.length >= 3 && (b[0] & 255) == 255 && (b[1] & 255) == 216 && (b[2] & 255) == 255) return DetectedType.JPEG;
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return DetectedType.WEBP;
        String text = new String(b, StandardCharsets.UTF_8);
        if (text.startsWith("%PDF-")) return DetectedType.PDF;
        if (b.length >= 12 && b[4] == 'f' && b[5] == 't' && b[6] == 'y' && b[7] == 'p') return DetectedType.MP4;
        if (b.length >= 4 && (b[0] & 255) == 0x1A && (b[1] & 255) == 0x45 && (b[2] & 255) == 0xDF && (b[3] & 255) == 0xA3) return DetectedType.WEBM;
        if (b.length >= 4 && b[0] == 'P' && b[1] == 'K') {
            if (zipContains(b, "word/")) return DetectedType.DOCX;
            if (zipContains(b, "xl/")) return DetectedType.XLSX;
            if (zipContains(b, "ppt/")) return DetectedType.PPTX;
            if (zipContains(b, null)) return DetectedType.ZIP;
        }
        if (textExtension(ext) && isText(b)) return "json".equals(ext) ? DetectedType.JSON : DetectedType.TEXT;
        if ("json".equals(ext) && isText(b)) return DetectedType.JSON;
        return DetectedType.UNKNOWN;
    }

    private boolean isText(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return false;
        for (byte value : bytes) if (value == 0) return false;
        String value;
        try { value = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString(); }
        catch (CharacterCodingException ex) { return false; }
        String lower = value.stripLeading().toLowerCase(Locale.ROOT);
        return !lower.startsWith("<html") && !lower.startsWith("<!doctype") && !lower.startsWith("<script") && !lower.startsWith("<?xml") && !lower.contains("<svg");
    }

    private boolean zipContains(byte[] bytes, String prefix) {
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) if (prefix == null || entry.getName().startsWith(prefix)) return true;
        } catch (IOException ignored) { return false; }
        return false;
    }

    private boolean validSignature(byte[] b, String ext) {
        String text = new String(b, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        if (text.contains("<svg") || text.contains("<html") || text.contains("<script") || text.contains("<?xml"))
            return false;
        if (Set.of("txt", "csv", "tex", "md").contains(ext)) {
            for (byte x : b)
                if (x == 0)
                    return false;
            return true;
        }
        if (ext.equals("jpg") || ext.equals("jpeg"))
            return b.length > 3 && (b[0] & 255) == 255 && (b[1] & 255) == 216 && (b[2] & 255) == 255;
        if (ext.equals("png"))
            return b.length > 8 && b[0] == (byte) 137 && b[1] == 80 && b[2] == 78 && b[3] == 71;
        if (ext.equals("webp"))
            return b.length > 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W'
                    && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
        if (ext.equals("pdf"))
            return text.startsWith("%pdf");
        if (ext.equals("mp4"))
            return b.length > 12 && b[4] == 'f' && b[5] == 't' && b[6] == 'y' && b[7] == 'p';
        if (ext.equals("webm"))
            return b.length > 4 && (b[0] & 255) == 0x1A && (b[1] & 255) == 0x45 && (b[2] & 255) == 0xDF && (b[3] & 255) == 0xA3;
        if (ext.equals("zip"))
            return zipBytes(b, null);
        if (Set.of("docx", "xlsx", "pptx").contains(ext))
            return zipBytes(b, ext);
        return false;
    }

    private boolean zipBytes(byte[] bytes, String officeType) {
        if (bytes.length < 4 || bytes[0] != 'P' || bytes[1] != 'K')
            return false;
        boolean content = false, family = officeType == null, hasEntry = false;
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                hasEntry = true;
                String name = entry.getName();
                if (name.equals("[Content_Types].xml"))
                    content = true;
                if (officeType != null && ((officeType.equals("docx") && name.startsWith("word/"))
                        || (officeType.equals("xlsx") && name.startsWith("xl/"))
                        || (officeType.equals("pptx") && name.startsWith("ppt/"))))
                    family = true;
            }
        } catch (IOException e) {
            return false;
        }
        // A normal ZIP only needs a valid archive entry; Office formats additionally require their content marker.
        return family && (officeType == null ? hasEntry : content);
    }

    private String sanitize(String name) {
        String raw = name == null ? "file" : name.replace('\\', '/');
        int slash = raw.lastIndexOf('/');
        String base = slash >= 0 ? raw.substring(slash + 1) : raw;
        StringBuilder cleaned = new StringBuilder();
        for (int i = 0; i < base.length(); i++) {
            char c = base.charAt(i);
            if (Character.isISOControl(c) || c == '"' || c == '\'' || c == ':' || c == '*' || c == '?' || c == '<' || c == '>' || c == '|' || (c >= '\u202A' && c <= '\u202E') || (c >= '\u2066' && c <= '\u2069')) continue;
            if (Character.isLetterOrDigit(c) || c == ' ' || c == '.' || c == '-' || c == '_') cleaned.append(c); else cleaned.append('_');
        }
        String result = cleaned.toString().trim();
        if (result.isBlank()) result = "file";
        if (result.length() <= 150) return result;
        String ext = extension(result);
        int maxBase = Math.max(1, 150 - (ext.isBlank() ? 0 : ext.length() + 1));
        String stem = ext.isBlank() ? result : result.substring(0, result.length() - ext.length() - 1);
        return stem.substring(0, Math.min(stem.length(), maxBase)) + (ext.isBlank() ? "" : "." + ext);
    }

    private String extension(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    private String detectedContentType(String ext) {
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "pdf" -> "application/pdf";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "txt", "log", "xml", "yaml", "yml", "sql", "java", "py", "js", "ts", "json", "md", "tex" -> "text/plain";
            case "csv" -> "text/plain";
            case "zip" -> "application/zip";
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            default -> MediaType.APPLICATION_OCTET_STREAM_VALUE;
        };
    }

    private Path safe(String name) {
        Path p = directory.resolve(name).normalize();
        if (!p.startsWith(directory))
            throw error(HttpStatus.BAD_REQUEST, "INVALID_FILE", "Invalid file name");
        return p;
    }

    private void deleteDisk(TaskFile f) {
        try {
            Files.deleteIfExists(safe(f.getStoredName()));
        } catch (IOException ignored) {
        }
    }

    private TaskFile file(Long id) {
        return files.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "File not found"));
    }

    private Task task(Long id) {
        return tasks.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "Task not found"));
    }

    private Subtask subtask(Long id) {
        return subtasks.findById(id)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "SUBTASK_NOT_FOUND", "Subtask not found"));
    }

    private void requireWritable(Task t, Long userId) {
        if (t.getStatus().name().equals("DONE"))
            throw error(HttpStatus.CONFLICT, "TASK_ARCHIVED", "Archived tasks are read-only until restored");
        access.requireMember(t.getId(), userId);
    }

    private void requireFileReader(Long taskId, TaskFile file, Long userId) {
        if (file.getSubtaskId() == null)
            access.requireMember(taskId, userId);
        else if (!access.manager(userId) && !access.lead(taskId, userId)
                && !assignees.existsBySubtaskIdAndUserId(file.getSubtaskId(), userId))
            deny();
    }

    private void deny() {
        throw error(HttpStatus.FORBIDDEN, "TASK_ACCESS_DENIED", "You do not have permission to use files on this task");
    }

    private AppException error(HttpStatus s, String c, String m) {
        return new AppException(s, c, m);
    }

    private TaskFileResponse response(TaskFile f) {
        String name = users.findById(f.getUploaderId()).map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse("Unknown user");
        return TaskFileResponse.from(f, name);
    }

    private boolean inlineType(String type) {
        return type != null && (type.startsWith("image/") || type.equals("application/pdf"));
    }

    private boolean inlineContentType(String type) {
        return type != null && (type.startsWith("image/") || type.equals("application/pdf") || type.startsWith("video/") || type.startsWith("audio/") || type.equals("text/plain"));
    }

    private boolean textExtension(String ext) { return Set.of("txt", "csv", "tex", "md", "json", "xml", "yaml", "yml", "log", "sql", "java", "py", "js", "ts").contains(ext); }

    @Transactional
    public TaskFileResponse editText(Long taskId, Long fileId, Long userId, String content, String expectedUpdatedAt) {
        Task task = task(taskId);
        TaskFile file = file(fileId);
        if (!taskId.equals(file.getTaskId())) throw error(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "File not found");
        if ("DONE".equals(task.getStatus().name())) throw error(HttpStatus.FORBIDDEN, "TASK_ARCHIVED", "Archived tasks are read-only");
        String ext = extension(file.getOriginalName());
        if (!textExtension(ext)) throw error(HttpStatus.BAD_REQUEST, "NOT_TEXT", "Only text files can be edited");
        if (!access.managerOrLead(taskId, userId) && !Objects.equals(file.getUploaderId(), userId)) deny();
        if (expectedUpdatedAt != null && file.getUpdatedAt() != null && !file.getUpdatedAt().toString().equals(expectedUpdatedAt))
            throw error(HttpStatus.CONFLICT, "FILE_CHANGED", "This file changed since it was opened. Reload it before saving.");
        if (content == null) content = "";
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 1024 * 1024) throw error(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_LIMIT", "Text files must be 1 MB or smaller");
        if (!isText(bytes)) throw error(HttpStatus.BAD_REQUEST, "INVALID_FILE", "Text must be valid UTF-8 and cannot contain HTML or binary data");
        Path target = safe(file.getStoredName());
        Path temp = directory.resolve(file.getStoredName() + ".tmp-" + UUID.randomUUID()).normalize();
        try { Files.write(temp, bytes, StandardOpenOption.CREATE_NEW); Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (IOException ex) { try { Files.deleteIfExists(temp); } catch (IOException ignored) {} throw error(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_WRITE_ERROR", "File could not be saved"); }
        file.setSizeBytes(bytes.length); file.setEditedBy(userId); file.setUpdatedAt(Instant.now());
        TaskFile saved = files.save(file);
        activity.logActivity(taskId, userId, "FILE_EDITED", Map.of("fileId", fileId));
        return response(saved);
    }

    public record Download(ByteArrayResource resource, String contentType, String filename, boolean inline) {
    }
    public record ViewData(byte[] bytes, String contentType, String filename, boolean inline) { }
    public record ZipEntryInfo(String name, long size) { }
    public record TextData(String text, boolean truncated) { }
}
