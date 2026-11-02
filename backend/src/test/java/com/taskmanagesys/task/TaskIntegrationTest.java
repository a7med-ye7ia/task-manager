package com.taskmanagesys.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagesys.audit.repository.AuditLogRepository;
import com.taskmanagesys.auth.service.JwtService;
import com.taskmanagesys.task.dto.ChangeTaskStatusRequest;
import com.taskmanagesys.task.dto.CreateCheckpointRequest;
import com.taskmanagesys.task.dto.CreateSubtaskRequest;
import com.taskmanagesys.task.dto.CreateTaskRequest;
import com.taskmanagesys.task.dto.UpdateTaskRequest;
import com.taskmanagesys.task.model.Task;
import com.taskmanagesys.task.model.TaskMemberRole;
import com.taskmanagesys.task.model.TaskPriority;
import com.taskmanagesys.task.model.TaskStatus;
import com.taskmanagesys.task.repository.CheckpointRepository;
import com.taskmanagesys.task.repository.CommentRepository;
import com.taskmanagesys.task.repository.CommentMentionRepository;
import com.taskmanagesys.task.repository.SubtaskRepository;
import com.taskmanagesys.task.repository.TaskActivityRepository;
import com.taskmanagesys.task.repository.TaskMemberRepository;
import com.taskmanagesys.task.repository.TaskRepository;
import com.taskmanagesys.task.service.TaskReadStateService;
import com.taskmanagesys.user.model.Role;
import com.taskmanagesys.user.model.User;
import com.taskmanagesys.user.model.UserStatus;
import com.taskmanagesys.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TaskIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private SubtaskRepository subtaskRepository;

    @Autowired
    private TaskMemberRepository taskMemberRepository;

    @Autowired
    private CheckpointRepository checkpointRepository;

    @Autowired
    private TaskActivityRepository taskActivityRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentMentionRepository commentMentionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private TaskReadStateService taskReadStateService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User manager;
    private User employee1;
    private User employee2;
    private String managerToken;
    private String employeeToken;
    private String employee2Token;

    @BeforeEach
    void setUp() {
        checkpointRepository.deleteAll();
        commentMentionRepository.deleteAll();
        commentRepository.deleteAll();
        taskActivityRepository.deleteAll();
        subtaskRepository.deleteAll();
        taskMemberRepository.deleteAll();
        taskRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.deleteAll();

        manager = new User();
        manager.setFirstName("Alice");
        manager.setLastName("Manager");
        manager.setEmail("alice.manager@example.com");
        manager.setPhone("+1234567890");
        manager.setPasswordHash(passwordEncoder.encode("Password123#"));
        manager.setRole(Role.MANAGER);
        manager.setStatus(UserStatus.ACTIVE);
        manager.setHiredAt(LocalDate.now());
        manager = userRepository.save(manager);

        employee1 = new User();
        employee1.setFirstName("Bob");
        employee1.setLastName("Worker");
        employee1.setEmail("bob.worker@example.com");
        employee1.setPhone("+1234567891");
        employee1.setPasswordHash(passwordEncoder.encode("Password123#"));
        employee1.setRole(Role.EMPLOYEE);
        employee1.setStatus(UserStatus.ACTIVE);
        employee1.setHiredAt(LocalDate.now());
        employee1 = userRepository.save(employee1);

        employee2 = new User();
        employee2.setFirstName("Charlie");
        employee2.setLastName("Helper");
        employee2.setEmail("charlie.helper@example.com");
        employee2.setPhone("+1234567892");
        employee2.setPasswordHash(passwordEncoder.encode("Password123#"));
        employee2.setRole(Role.EMPLOYEE);
        employee2.setStatus(UserStatus.ACTIVE);
        employee2.setHiredAt(LocalDate.now());
        employee2 = userRepository.save(employee2);

        managerToken = jwtService.generateAccessToken(manager);
        employeeToken = jwtService.generateAccessToken(employee1);
        employee2Token = jwtService.generateAccessToken(employee2);
    }

    @Test
    void managerCanCreateTaskAndSubtaskAndEmployeeGets403() throws Exception {
        // 1. Employee trying to create task gets 403 Forbidden
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("Employee Task Try");
        req.setPriority(TaskPriority.HIGH);
        req.setDeadline(LocalDate.now().plusDays(5));
        req.setMemberUserIds(List.of(employee1.getId()));

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        // 2. Manager creates task
        CreateTaskRequest validReq = new CreateTaskRequest();
        validReq.setTitle("Launch Product");
        validReq.setDescription("Main product release task");
        validReq.setPriority(TaskPriority.HIGH);
        validReq.setDeadline(LocalDate.now().plusDays(10));
        validReq.setMemberUserIds(List.of(employee1.getId()));
        validReq.setLeadUserId(employee1.getId());

        String resJson = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Launch Product"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.lead.firstName").value("Bob"))
                .andExpect(jsonPath("$.lead.email").doesNotExist())
                .andExpect(jsonPath("$.progress").value(0))
                .andReturn().getResponse().getContentAsString();

        Long taskId = objectMapper.readTree(resJson).get("id").asLong();

        // Check task activity logged
        assertEquals(1, taskActivityRepository.findByTaskIdOrderByCreatedAtDesc(taskId).size());

        // 3. Manager creates subtask assigned to employee2 (BR-14: auto-adds employee2 to task members)
        CreateSubtaskRequest subtaskReq = new CreateSubtaskRequest();
        subtaskReq.setTitle("Build landing page");
        subtaskReq.setDeadline(LocalDate.now().plusDays(7));
        subtaskReq.setAssigneeUserIds(List.of(employee2.getId()));
        subtaskReq.setConfirmAddMember(true);

        mockMvc.perform(post("/api/tasks/" + taskId + "/subtasks")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subtaskReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Build landing page"))
                .andExpect(jsonPath("$.assignees[0].firstName").value("Charlie"))
                .andExpect(jsonPath("$.assignees[0].email").doesNotExist());

        // BR-14 verification: employee2 is now in task_members
        assertTrue(taskMemberRepository.existsByTaskIdAndUserId(taskId, employee2.getId()));

        // 4. Checkpoint addition & toggle
        CreateCheckpointRequest cpReq = new CreateCheckpointRequest();
        cpReq.setTitle("Approve design mockups");

        String cpResJson = mockMvc.perform(post("/api/tasks/" + taskId + "/checkpoints")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cpReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Approve design mockups"))
                .andExpect(jsonPath("$.done").value(false))
                .andReturn().getResponse().getContentAsString();

        Long cpId = objectMapper.readTree(cpResJson).get("id").asLong();

        // Toggle checkpoint
        mockMvc.perform(patch("/api/tasks/" + taskId + "/checkpoints/" + cpId + "/toggle")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done").value(true));

        // 5. Manager changes task status (BR-21)
        ChangeTaskStatusRequest statusReq = new ChangeTaskStatusRequest();
        statusReq.setStatus(TaskStatus.IN_PROGRESS);

        mockMvc.perform(patch("/api/tasks/" + taskId + "/status")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // 6. Employee can view tasks list and task details
        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Launch Product"));

        mockMvc.perform(get("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId));
    }

    @Test
    void taskResponseIncludesCreatedFieldsAndObjectPermissionIsEnforced() throws Exception {
        CreateTaskRequest req = new CreateTaskRequest(); req.setTitle("Created fields task"); req.setPriority(TaskPriority.MEDIUM); req.setDeadline(LocalDate.now().plusDays(4)); req.setMemberUserIds(List.of(employee1.getId()));
        String body = mockMvc.perform(post("/api/tasks").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated()).andExpect(jsonPath("$.createdAt", notNullValue())).andExpect(jsonPath("$.updatedAt", notNullValue())).andExpect(jsonPath("$.createdBy.firstName", is("Alice"))).andExpect(jsonPath("$.email").doesNotExist()).andExpect(jsonPath("$.createdBy.email").doesNotExist()).andExpect(jsonPath("$.createdBy.hiredAt").doesNotExist()).andExpect(jsonPath("$.members[0].user.email").doesNotExist()).andReturn().getResponse().getContentAsString();
        Long taskId = objectMapper.readTree(body).get("id").asLong();
        CreateSubtaskRequest subtask = new CreateSubtaskRequest(); subtask.setTitle("Privacy subtask"); subtask.setDeadline(LocalDate.now().plusDays(2)); subtask.setAssigneeUserIds(List.of(employee1.getId()));
        mockMvc.perform(post("/api/tasks/" + taskId + "/subtasks").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(subtask))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/tasks/" + taskId + "/comments").header("Authorization", "Bearer " + employeeToken).contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Privacy comment\"}")).andExpect(status().isCreated()).andExpect(jsonPath("$.author.email").doesNotExist()).andExpect(jsonPath("$.author.hiredAt").doesNotExist());
        mockMvc.perform(get("/api/tasks/" + taskId).header("Authorization", "Bearer " + employeeToken)).andExpect(status().isOk()).andExpect(jsonPath("$.members[0].user.hiredAt").doesNotExist()).andExpect(jsonPath("$.subtasks[0].assignees[0].email").doesNotExist()).andExpect(jsonPath("$.subtasks[0].assignees[0].hiredAt").doesNotExist());
        mockMvc.perform(put("/api/tasks/" + taskId).header("Authorization", "Bearer " + employeeToken).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Nope\"}")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/tasks/" + taskId).header("Authorization", "Bearer " + employeeToken)).andExpect(status().isForbidden());
    }

    private Long createPermissionTask() throws Exception {
        CreateTaskRequest req = new CreateTaskRequest(); req.setTitle("Permission matrix task"); req.setPriority(TaskPriority.MEDIUM); req.setDeadline(LocalDate.now().plusDays(5)); req.setMemberUserIds(List.of(employee1.getId(), employee2.getId())); req.setLeadUserId(employee1.getId());
        String body = mockMvc.perform(post("/api/tasks").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    @Test void editTaskFields_allowedLead() throws Exception { Long id = createPermissionTask(); UpdateTaskRequest req = new UpdateTaskRequest(); req.setTitle("Lead edited"); mockMvc.perform(put("/api/tasks/" + id).header("Authorization", "Bearer " + employeeToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isOk()); }
    @Test void editTaskFields_disallowedMember() throws Exception { Long id = createPermissionTask(); UpdateTaskRequest req = new UpdateTaskRequest(); req.setTitle("Member cannot edit"); mockMvc.perform(put("/api/tasks/" + id).header("Authorization", "Bearer " + employee2Token).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isForbidden()); }
    @Test void changeLead_allowedManager() throws Exception { Long id = createPermissionTask(); mockMvc.perform(put("/api/tasks/" + id + "/lead/" + employee2.getId()).header("Authorization", "Bearer " + managerToken)).andExpect(status().isOk()); }
    @Test void changeLead_disallowedLead() throws Exception { Long id = createPermissionTask(); mockMvc.perform(put("/api/tasks/" + id + "/lead/" + employee2.getId()).header("Authorization", "Bearer " + employeeToken)).andExpect(status().isForbidden()); }
    @Test void createSubtask_allowedLead() throws Exception { Long id = createPermissionTask(); CreateSubtaskRequest req = new CreateSubtaskRequest(); req.setTitle("Lead subtask"); req.setDeadline(LocalDate.now().plusDays(2)); req.setAssigneeUserIds(List.of(employee2.getId())); mockMvc.perform(post("/api/tasks/" + id + "/subtasks").header("Authorization", "Bearer " + employeeToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated()); }
    @Test void createSubtask_disallowedMember() throws Exception { Long id = createPermissionTask(); CreateSubtaskRequest req = new CreateSubtaskRequest(); req.setTitle("No"); req.setDeadline(LocalDate.now().plusDays(2)); req.setAssigneeUserIds(List.of(employee2.getId())); mockMvc.perform(post("/api/tasks/" + id + "/subtasks").header("Authorization", "Bearer " + employee2Token).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isForbidden()); }
    @Test void createCheckpoint_allowedLead() throws Exception { Long id = createPermissionTask(); mockMvc.perform(post("/api/tasks/" + id + "/checkpoints").header("Authorization", "Bearer " + employeeToken).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Lead checkpoint\"}")).andExpect(status().isCreated()); }
    @Test void createCheckpoint_disallowedMember() throws Exception { Long id = createPermissionTask(); mockMvc.perform(post("/api/tasks/" + id + "/checkpoints").header("Authorization", "Bearer " + employee2Token).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"No\"}")).andExpect(status().isForbidden()); }
    @Test void changeSubtaskStatus_allowedAssignee() throws Exception { Long id = createPermissionTask(); CreateSubtaskRequest req = new CreateSubtaskRequest(); req.setTitle("Assigned"); req.setDeadline(LocalDate.now().plusDays(2)); req.setAssigneeUserIds(List.of(employee2.getId())); Long subtask = objectMapper.readTree(mockMvc.perform(post("/api/tasks/" + id + "/subtasks").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong(); mockMvc.perform(put("/api/tasks/" + id + "/subtasks/" + subtask).header("Authorization", "Bearer " + employee2Token).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"IN_PROGRESS\"}")).andExpect(status().isOk()); }
    @Test void changeSubtaskStatus_disallowedUnassignedMember() throws Exception { Long id = createPermissionTask(); CreateSubtaskRequest req = new CreateSubtaskRequest(); req.setTitle("Assigned"); req.setDeadline(LocalDate.now().plusDays(2)); req.setAssigneeUserIds(List.of(employee1.getId())); Long subtask = objectMapper.readTree(mockMvc.perform(post("/api/tasks/" + id + "/subtasks").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong(); mockMvc.perform(put("/api/tasks/" + id + "/subtasks/" + subtask).header("Authorization", "Bearer " + employee2Token).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"IN_PROGRESS\"}")).andExpect(status().isForbidden()); }
    @Test void addOrRemoveMembers_allowedLead() throws Exception { Long id = createPermissionTask(); mockMvc.perform(post("/api/tasks/" + id + "/members/" + manager.getId()).header("Authorization", "Bearer " + employeeToken)).andExpect(status().isOk()); mockMvc.perform(delete("/api/tasks/" + id + "/members/" + manager.getId()).header("Authorization", "Bearer " + employeeToken)).andExpect(status().isOk()); }
    @Test void addOrRemoveMembers_disallowedMember() throws Exception { Long id = createPermissionTask(); mockMvc.perform(post("/api/tasks/" + id + "/members/" + manager.getId()).header("Authorization", "Bearer " + employee2Token)).andExpect(status().isForbidden()); }
    @Test void tickCheckpoint_allowedMember() throws Exception { Long id = createPermissionTask(); Long checkpoint = objectMapper.readTree(mockMvc.perform(post("/api/tasks/" + id + "/checkpoints").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Tick\"}")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong(); mockMvc.perform(patch("/api/tasks/" + id + "/checkpoints/" + checkpoint + "/toggle").header("Authorization", "Bearer " + employee2Token)).andExpect(status().isOk()); }
    @Test void tickCheckpoint_disallowedOutsider() throws Exception { Long id = createPermissionTask(); Long checkpoint = objectMapper.readTree(mockMvc.perform(post("/api/tasks/" + id + "/checkpoints").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Tick\"}")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong(); User outsider = new User(); outsider.setFirstName("Out"); outsider.setLastName("Sider"); outsider.setEmail("outsider@example.com"); outsider.setPhone("+10000000000"); outsider.setPasswordHash(passwordEncoder.encode("Password123#")); outsider.setRole(Role.EMPLOYEE); outsider.setStatus(UserStatus.ACTIVE); outsider.setHiredAt(LocalDate.now()); outsider = userRepository.save(outsider); String token = jwtService.generateAccessToken(outsider); mockMvc.perform(patch("/api/tasks/" + id + "/checkpoints/" + checkpoint + "/toggle").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden()); }
    @Test void markDone_allowedManager() throws Exception { Long id = createPermissionTask(); ChangeTaskStatusRequest req = new ChangeTaskStatusRequest(); req.setStatus(TaskStatus.DONE); mockMvc.perform(patch("/api/tasks/" + id + "/status").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isOk()); }
    @Test void markDone_disallowedMember() throws Exception { Long id = createPermissionTask(); ChangeTaskStatusRequest req = new ChangeTaskStatusRequest(); req.setStatus(TaskStatus.DONE); mockMvc.perform(patch("/api/tasks/" + id + "/status").header("Authorization", "Bearer " + employee2Token).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isForbidden()); }
    @Test void restoreArchive_allowedManager() throws Exception { Long id = createPermissionTask(); ChangeTaskStatusRequest done = new ChangeTaskStatusRequest(); done.setStatus(TaskStatus.DONE); mockMvc.perform(patch("/api/tasks/" + id + "/status").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(done))).andExpect(status().isOk()); mockMvc.perform(post("/api/tasks/" + id + "/restore").param("targetStatus", "TODO").header("Authorization", "Bearer " + managerToken)).andExpect(status().isOk()); }
    @Test void restoreArchive_disallowedMember() throws Exception { Long id = createPermissionTask(); ChangeTaskStatusRequest done = new ChangeTaskStatusRequest(); done.setStatus(TaskStatus.DONE); mockMvc.perform(patch("/api/tasks/" + id + "/status").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(done))).andExpect(status().isOk()); mockMvc.perform(post("/api/tasks/" + id + "/restore").param("targetStatus", "TODO").header("Authorization", "Bearer " + employee2Token)).andExpect(status().isForbidden()); }
    @Test void deleteAnyoneComment_allowedManager() throws Exception { Long id = createPermissionTask(); Long comment = objectMapper.readTree(mockMvc.perform(post("/api/tasks/" + id + "/comments").header("Authorization", "Bearer " + employeeToken).contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Comment\"}")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong(); mockMvc.perform(delete("/api/tasks/" + id + "/comments/" + comment).header("Authorization", "Bearer " + managerToken)).andExpect(status().isNoContent()); }
    @Test void deleteAnyoneComment_disallowedLead() throws Exception { Long id = createPermissionTask(); Long comment = objectMapper.readTree(mockMvc.perform(post("/api/tasks/" + id + "/comments").header("Authorization", "Bearer " + employee2Token).contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Comment\"}")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong(); mockMvc.perform(delete("/api/tasks/" + id + "/comments/" + comment).header("Authorization", "Bearer " + employeeToken)).andExpect(status().isForbidden()); }
    @Test void managerNonMemberCanReadAndWriteComments() throws Exception { Long id = createPermissionTask(); mockMvc.perform(get("/api/tasks/" + id + "/comments").header("Authorization", "Bearer " + managerToken)).andExpect(status().isOk()); mockMvc.perform(post("/api/tasks/" + id + "/comments").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Manager note\"}")).andExpect(status().isCreated()); }

    @Test
    void membersLeadAndManagerCanPostCommentsButOtherEmployeeIsRejected() throws Exception {
        CreateTaskRequest req = new CreateTaskRequest(); req.setTitle("Comment permissions"); req.setPriority(TaskPriority.MEDIUM); req.setDeadline(LocalDate.now().plusDays(5)); req.setMemberUserIds(List.of(employee1.getId())); req.setLeadUserId(employee1.getId());
        String taskJson = mockMvc.perform(post("/api/tasks").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long taskId = objectMapper.readTree(taskJson).get("id").asLong();
        String payload = "{\"body\":\"A valid comment\"}";
        mockMvc.perform(post("/api/tasks/" + taskId + "/comments").header("Authorization", "Bearer " + employeeToken).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isCreated()).andExpect(jsonPath("$.body").value("A valid comment"));
        mockMvc.perform(post("/api/tasks/" + taskId + "/comments").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isCreated());
        mockMvc.perform(post("/api/tasks/" + taskId + "/comments").header("Authorization", "Bearer " + employee2Token).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("TASK_ACCESS_DENIED"));
    }

    @Test
    void onlyCommentAuthorCanEditOrDelete() throws Exception {
        CreateTaskRequest req = new CreateTaskRequest(); req.setTitle("Author comment task"); req.setPriority(TaskPriority.MEDIUM); req.setDeadline(LocalDate.now().plusDays(5)); req.setMemberUserIds(List.of(employee1.getId(), employee2.getId())); req.setLeadUserId(employee1.getId());
        String taskJson = mockMvc.perform(post("/api/tasks").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long taskId = objectMapper.readTree(taskJson).get("id").asLong();
        String commentJson = mockMvc.perform(post("/api/tasks/" + taskId + "/comments").header("Authorization", "Bearer " + employeeToken).contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Author text\"}")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long commentId = objectMapper.readTree(commentJson).get("id").asLong();
        mockMvc.perform(put("/api/tasks/" + taskId + "/comments/" + commentId).header("Authorization", "Bearer " + employee2Token).contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Member edit\"}")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/tasks/" + taskId + "/comments/" + commentId).header("Authorization", "Bearer " + employee2Token)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/tasks/" + taskId + "/comments/" + commentId).header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Manager edit\"}")).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/tasks/" + taskId + "/comments/" + commentId).header("Authorization", "Bearer " + employeeToken).contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Edited text\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.edited").value(true));
        String managerDeleteComment = mockMvc.perform(post("/api/tasks/" + taskId + "/comments").header("Authorization", "Bearer " + employeeToken).contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Manager may delete\"}")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long managerDeleteId = objectMapper.readTree(managerDeleteComment).get("id").asLong();
        mockMvc.perform(delete("/api/tasks/" + taskId + "/comments/" + managerDeleteId).header("Authorization", "Bearer " + managerToken)).andExpect(status().isNoContent());
        String leadTargetComment = mockMvc.perform(post("/api/tasks/" + taskId + "/comments").header("Authorization", "Bearer " + employee2Token).contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Lead cannot moderate\"}")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long leadTargetId = objectMapper.readTree(leadTargetComment).get("id").asLong();
        mockMvc.perform(put("/api/tasks/" + taskId + "/comments/" + leadTargetId).header("Authorization", "Bearer " + employeeToken).contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Lead edit\"}")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/tasks/" + taskId + "/comments/" + leadTargetId).header("Authorization", "Bearer " + employeeToken)).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/tasks/" + taskId + "/comments/" + commentId).header("Authorization", "Bearer " + employeeToken)).andExpect(status().isNoContent());
    }

    @Test
    void commentsStoreOnlyVerifiedTaskMentionsAndUpdateThemOnEdit() throws Exception {
        CreateTaskRequest req = new CreateTaskRequest(); req.setTitle("Mention task"); req.setPriority(TaskPriority.MEDIUM); req.setDeadline(LocalDate.now().plusDays(5)); req.setMemberUserIds(List.of(employee1.getId(), employee2.getId()));
        String taskJson = mockMvc.perform(post("/api/tasks").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long taskId = objectMapper.readTree(taskJson).get("id").asLong();
        String body = "{\"body\":\"Hello @Charlie Helper\",\"mentionedUserIds\":[" + employee2.getId() + ",999999]}";
        String commentJson = mockMvc.perform(post("/api/tasks/" + taskId + "/comments").header("Authorization", "Bearer " + employeeToken).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long commentId = objectMapper.readTree(commentJson).get("id").asLong();
        mockMvc.perform(get("/api/tasks/" + taskId + "/comments").header("Authorization", "Bearer " + employeeToken)).andExpect(status().isOk()).andExpect(jsonPath("$[0].mentionedUserIds", contains(employee2.getId().intValue())));
        mockMvc.perform(put("/api/tasks/" + taskId + "/comments/" + commentId).header("Authorization", "Bearer " + employeeToken).contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"No mention\",\"mentionedUserIds\":[" + employee2.getId() + "]}")).andExpect(status().isOk()).andExpect(jsonPath("$.mentionedUserIds", hasSize(0)));
    }

    @Test
    void commentsCannotMentionAnEmployeeOutsideTheTask() throws Exception {
        CreateTaskRequest req = new CreateTaskRequest(); req.setTitle("Mention boundary"); req.setPriority(TaskPriority.MEDIUM); req.setDeadline(LocalDate.now().plusDays(5)); req.setMemberUserIds(List.of(employee1.getId()));
        String taskJson = mockMvc.perform(post("/api/tasks").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long taskId = objectMapper.readTree(taskJson).get("id").asLong();
        mockMvc.perform(post("/api/tasks/" + taskId + "/comments").header("Authorization", "Bearer " + employeeToken).contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Hi Charlie\",\"mentionedUserIds\":[" + employee2.getId() + "]}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.mentionedUserIds", hasSize(0)));
    }

    @Test
    void mentionsHaveUnseenCountAndOpeningCommentsMarksThemSeen() throws Exception {
        CreateTaskRequest req = new CreateTaskRequest(); req.setTitle("Mention seen task"); req.setPriority(TaskPriority.MEDIUM); req.setDeadline(LocalDate.now().plusDays(5)); req.setMemberUserIds(List.of(employee1.getId(), employee2.getId()));
        String taskJson = mockMvc.perform(post("/api/tasks").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long taskId = objectMapper.readTree(taskJson).get("id").asLong();
        mockMvc.perform(post("/api/tasks/" + taskId + "/comments").header("Authorization", "Bearer " + employeeToken).contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Hi @Charlie Helper\",\"mentionedUserIds\":[" + employee2.getId() + "]}" )).andExpect(status().isCreated());
        mockMvc.perform(get("/api/mentions/unread").header("Authorization", "Bearer " + employee2Token)).andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1));
        mockMvc.perform(get("/api/tasks/" + taskId + "/comments").header("Authorization", "Bearer " + employee2Token)).andExpect(status().isOk());
        mockMvc.perform(get("/api/mentions/unread").header("Authorization", "Bearer " + employee2Token)).andExpect(status().isOk()).andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void unreadCountsUseCurrentUserTaskAndBatchBoundaries() throws Exception {
        Long taskA = createPermissionTask();
        Long taskB = createPermissionTask();
        String ownComment = "{\"body\":\"My own comment\"}";
        String mentionCurrent = "{\"body\":\"Mention @Bob Worker\",\"mentionedUserIds\":[" + employee1.getId() + "]}";
        String mentionOther = "{\"body\":\"Mention @Charlie Helper\",\"mentionedUserIds\":[" + employee2.getId() + "]}";
        mockMvc.perform(post("/api/tasks/" + taskA + "/comments").header("Authorization", "Bearer " + employeeToken).contentType(MediaType.APPLICATION_JSON).content(ownComment)).andExpect(status().isCreated());
        mockMvc.perform(post("/api/tasks/" + taskA + "/comments").header("Authorization", "Bearer " + employee2Token).contentType(MediaType.APPLICATION_JSON).content(mentionCurrent)).andExpect(status().isCreated());
        mockMvc.perform(post("/api/tasks/" + taskA + "/comments").header("Authorization", "Bearer " + employeeToken).contentType(MediaType.APPLICATION_JSON).content(mentionOther)).andExpect(status().isCreated());
        mockMvc.perform(post("/api/tasks/" + taskB + "/comments").header("Authorization", "Bearer " + employee2Token).contentType(MediaType.APPLICATION_JSON).content(mentionCurrent)).andExpect(status().isCreated());

        var counts = taskReadStateService.unreadCounts(List.of(taskA, taskB), employee1.getId());
        var mentionCounts = taskReadStateService.unreadMentionCounts(List.of(taskA, taskB), employee1.getId());
        assertEquals(3L, counts.get(taskA));
        assertEquals(3L, counts.get(taskB));
        assertEquals(1L, mentionCounts.get(taskA));
        assertEquals(1L, mentionCounts.get(taskB));
    }

    @Test
    void signedInUserGetsPublicProfileWithoutEmailAndWorksOnFilter() throws Exception {
        mockMvc.perform(get("/api/users/" + employee1.getId() + "/public").header("Authorization", "Bearer " + employee2Token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.firstName").value("Bob"))
                .andExpect(jsonPath("$.email").doesNotExist());
        mockMvc.perform(get("/api/users/" + employee1.getId() + "/public/tasks?filter=COMMON").header("Authorization", "Bearer " + employee2Token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/users/" + employee1.getId() + "/public")).andExpect(status().isUnauthorized());
    }

    @Test
    void userDetailsAndTasksEndpointsEnforceManagerRoleAndReturnCorrectProgress() throws Exception {
        // Create a task with employee1 as member
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("Employee1 Assignment");
        req.setPriority(TaskPriority.MEDIUM);
        req.setDeadline(LocalDate.now().plusDays(3));
        req.setMemberUserIds(List.of(employee1.getId()));
        req.setLeadUserId(employee1.getId());

        String res = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long taskId = objectMapper.readTree(res).get("id").asLong();

        // 1. Employee trying to access GET /api/users/{id} gets 403
        mockMvc.perform(get("/api/users/" + employee1.getId())
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());

        // 2. Unauthenticated gets 401
        mockMvc.perform(get("/api/users/" + employee1.getId()))
                .andExpect(status().isUnauthorized());

        // 3. Manager accesses GET /api/users/{id}
        mockMvc.perform(get("/api/users/" + employee1.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("bob.worker@example.com"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));

        // 4. Manager accesses GET /api/users/{id}/tasks
        mockMvc.perform(get("/api/users/" + employee1.getId() + "/tasks")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(taskId))
                .andExpect(jsonPath("$[0].title").value("Employee1 Assignment"))
                .andExpect(jsonPath("$[0].userRole").value("LEAD"))
                .andExpect(jsonPath("$[0].progress").value(0))
                .andExpect(jsonPath("$[0].overdue").value(false));

        // 5. Manager safety rules: cannot deactivate or delete self
        mockMvc.perform(post("/api/admin/users/" + manager.getId() + "/deactivate")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_DEACTIVATE_SELF"));

        mockMvc.perform(delete("/api/admin/users/" + manager.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_DELETE_SELF"));
    }

    @Test
    void sendToReviewAndSendBackEnforceRules() throws Exception {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("Review flow"); req.setPriority(TaskPriority.MEDIUM); req.setDeadline(LocalDate.now().plusDays(4));
        req.setMemberUserIds(List.of(employee1.getId())); req.setLeadUserId(employee1.getId());
        String body = mockMvc.perform(post("/api/tasks").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long taskId = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(post("/api/tasks/" + taskId + "/send-to-review").header("Authorization", "Bearer " + employee2Token))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/tasks/" + taskId + "/send-to-review").header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REVIEW"));

        mockMvc.perform(post("/api/tasks/" + taskId + "/send-back").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Needs changes\",\"subtaskIds\":[],\"checkpointIds\":[]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void taskLeadCanSearchCandidatesButMembersAndOutsidersCannot() throws Exception {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("Candidate search"); req.setPriority(TaskPriority.LOW); req.setDeadline(LocalDate.now().plusDays(3));
        req.setMemberUserIds(List.of(employee1.getId())); req.setLeadUserId(employee1.getId());
        String body = mockMvc.perform(post("/api/tasks").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long taskId = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(get("/api/tasks/" + taskId + "/member-candidates").param("query", "Charlie").header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.size").value(20)).andExpect(jsonPath("$.totalElements").isNumber()).andExpect(jsonPath("$.totalPages").isNumber()).andExpect(jsonPath("$.content[0].id").value(employee2.getId()))
                .andExpect(jsonPath("$.content[0].email").doesNotExist());
        mockMvc.perform(get("/api/tasks/" + taskId + "/member-candidates").header("Authorization", "Bearer " + managerToken)).andExpect(status().isOk());
        mockMvc.perform(get("/api/tasks/" + taskId + "/member-candidates").header("Authorization", "Bearer " + employee2Token)).andExpect(status().isForbidden());
        User otherLead = new User();
        otherLead.setFirstName("Other"); otherLead.setLastName("Lead"); otherLead.setEmail("other.lead@example.com"); otherLead.setPhone("+1234567893"); otherLead.setPasswordHash(passwordEncoder.encode("Password123#")); otherLead.setRole(Role.EMPLOYEE); otherLead.setStatus(UserStatus.ACTIVE); otherLead.setHiredAt(LocalDate.now());
        otherLead = userRepository.save(otherLead);
        String otherToken = jwtService.generateAccessToken(otherLead);
        mockMvc.perform(get("/api/tasks/" + taskId + "/member-candidates").header("Authorization", "Bearer " + otherToken)).andExpect(status().isForbidden());
    }

    @Test
    void doneTaskCommentsAreReadOnly() throws Exception {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("Done read only"); req.setPriority(TaskPriority.LOW); req.setDeadline(LocalDate.now().plusDays(3)); req.setMemberUserIds(List.of(employee1.getId()));
        String body = mockMvc.perform(post("/api/tasks").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long taskId = objectMapper.readTree(body).get("id").asLong();
        ChangeTaskStatusRequest done = new ChangeTaskStatusRequest(); done.setStatus(TaskStatus.DONE);
        mockMvc.perform(patch("/api/tasks/" + taskId + "/status").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(done))).andExpect(status().isOk());
        mockMvc.perform(post("/api/tasks/" + taskId + "/comments").header("Authorization", "Bearer " + employeeToken).contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Not writable\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("COMMENTS_READ_ONLY"));
        mockMvc.perform(get("/api/tasks/" + taskId + "/comments").header("Authorization", "Bearer " + employeeToken)).andExpect(status().isOk());
    }

    @Test
    void sendBackCanReopenDoneSubtask() throws Exception {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("Reopen flow"); req.setPriority(TaskPriority.MEDIUM); req.setDeadline(LocalDate.now().plusDays(5)); req.setMemberUserIds(List.of(employee1.getId())); req.setLeadUserId(employee1.getId());
        Long taskId = objectMapper.readTree(mockMvc.perform(post("/api/tasks").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        CreateSubtaskRequest subtask = new CreateSubtaskRequest(); subtask.setTitle("Reopen me"); subtask.setDeadline(LocalDate.now().plusDays(2)); subtask.setAssigneeUserIds(List.of(employee1.getId()));
        Long subtaskId = objectMapper.readTree(mockMvc.perform(post("/api/tasks/" + taskId + "/subtasks").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(subtask))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(put("/api/tasks/" + taskId + "/subtasks/" + subtaskId).header("Authorization", "Bearer " + employeeToken).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DONE\"}")).andExpect(status().isOk());
        mockMvc.perform(get("/api/tasks/" + taskId).header("Authorization", "Bearer " + employeeToken)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REVIEW"));
        mockMvc.perform(post("/api/tasks/" + taskId + "/send-back").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Please revise\",\"subtaskIds\":[" + subtaskId + "],\"checkpointIds\":[]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mockMvc.perform(get("/api/tasks/" + taskId).header("Authorization", "Bearer " + employeeToken)).andExpect(status().isOk()).andExpect(jsonPath("$.subtasks[0].status").value("TODO"));
    }

    @Test
    void completingTwoSubtasksTransitionsThroughInProgressToReview() throws Exception {
        CreateTaskRequest req = new CreateTaskRequest();
        req.setTitle("Two subtask flow"); req.setPriority(TaskPriority.MEDIUM); req.setDeadline(LocalDate.now().plusDays(5)); req.setMemberUserIds(List.of(employee1.getId())); req.setLeadUserId(employee1.getId());
        Long taskId = objectMapper.readTree(mockMvc.perform(post("/api/tasks").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        CreateSubtaskRequest first = new CreateSubtaskRequest(); first.setTitle("First"); first.setDeadline(LocalDate.now().plusDays(2)); first.setAssigneeUserIds(List.of(employee1.getId()));
        CreateSubtaskRequest second = new CreateSubtaskRequest(); second.setTitle("Second"); second.setDeadline(LocalDate.now().plusDays(3)); second.setAssigneeUserIds(List.of(employee1.getId()));
        Long firstId = objectMapper.readTree(mockMvc.perform(post("/api/tasks/" + taskId + "/subtasks").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(first))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        Long secondId = objectMapper.readTree(mockMvc.perform(post("/api/tasks/" + taskId + "/subtasks").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(second))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(put("/api/tasks/" + taskId + "/subtasks/" + firstId).header("Authorization", "Bearer " + employeeToken).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DONE\"}")).andExpect(status().isOk());
        mockMvc.perform(get("/api/tasks/" + taskId).header("Authorization", "Bearer " + employeeToken)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mockMvc.perform(put("/api/tasks/" + taskId + "/subtasks/" + secondId).header("Authorization", "Bearer " + employeeToken).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DONE\"}")).andExpect(status().isOk());
        mockMvc.perform(get("/api/tasks/" + taskId).header("Authorization", "Bearer " + employeeToken)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REVIEW"));
    }

    @Test
    void sendBackRequiresReason() throws Exception {
        CreateTaskRequest req = new CreateTaskRequest(); req.setTitle("Reason required"); req.setPriority(TaskPriority.LOW); req.setDeadline(LocalDate.now().plusDays(2)); req.setMemberUserIds(List.of(employee1.getId()));
        Long taskId = objectMapper.readTree(mockMvc.perform(post("/api/tasks").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        ChangeTaskStatusRequest review = new ChangeTaskStatusRequest(); review.setStatus(TaskStatus.REVIEW);
        mockMvc.perform(patch("/api/tasks/" + taskId + "/status").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(review))).andExpect(status().isOk());
        mockMvc.perform(post("/api/tasks/" + taskId + "/send-back").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"\",\"subtaskIds\":[],\"checkpointIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("reason"))
                .andExpect(jsonPath("$.fieldErrors[0].message").isNotEmpty());
    }

    @Test
    void doneTaskGetsArchiveFieldsAndManagerCanRestoreEachTargetStatus() throws Exception {
        Long taskId = createPermissionTask();
        ChangeTaskStatusRequest done = new ChangeTaskStatusRequest(); done.setStatus(TaskStatus.DONE);
        mockMvc.perform(patch("/api/tasks/" + taskId + "/status").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(done)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.doneAt").isNotEmpty()).andExpect(jsonPath("$.purgeAt").isNotEmpty());
        mockMvc.perform(post("/api/tasks/" + taskId + "/restore").param("targetStatus", "REVIEW").header("Authorization", "Bearer " + managerToken)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REVIEW")).andExpect(jsonPath("$.doneAt").doesNotExist()).andExpect(jsonPath("$.purgeAt").doesNotExist());
    }

    @Test
    void archivedTaskRejectsMemberStatusChangeAndExpiredRestore() throws Exception {
        Long taskId = createPermissionTask();
        ChangeTaskStatusRequest done = new ChangeTaskStatusRequest(); done.setStatus(TaskStatus.DONE);
        mockMvc.perform(patch("/api/tasks/" + taskId + "/status").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(done))).andExpect(status().isOk());
        mockMvc.perform(patch("/api/tasks/" + taskId + "/status").header("Authorization", "Bearer " + employee2Token).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"TODO\"}")).andExpect(status().isForbidden());
        Task archived = taskRepository.findById(taskId).orElseThrow(); archived.setPurgeAt(java.time.Instant.now().minusSeconds(60)); taskRepository.save(archived);
        mockMvc.perform(post("/api/tasks/" + taskId + "/restore").param("targetStatus", "TODO").header("Authorization", "Bearer " + managerToken)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TASK_PURGE_EXPIRED"));
    }

    @Test
    void managerCanDeleteArchivedTaskAndAuditIsWritten() throws Exception {
        Long id = createPermissionTask();
        ChangeTaskStatusRequest done = new ChangeTaskStatusRequest(); done.setStatus(TaskStatus.DONE);
        mockMvc.perform(patch("/api/tasks/" + id + "/status").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(done))).andExpect(status().isOk());
        mockMvc.perform(delete("/api/tasks/" + id).header("Authorization", "Bearer " + managerToken)).andExpect(status().isNoContent());
        assertFalse(taskRepository.existsById(id));
        assertTrue(auditLogRepository.findAll().stream().anyMatch(a ->
                "TASK_DELETED".equals(a.getAction()) && a.getDetails() != null
                        && a.getDetails().replaceAll("\\s+", "").contains("\"taskId\":" + id)));
    }

    @Test
    void taskPoolUsesStablePageShape() throws Exception {
        mockMvc.perform(get("/api/tasks/pool").header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    @Test
    void onlyManagerCanDeleteArchivedTask() throws Exception {
        Long id = createPermissionTask();
        ChangeTaskStatusRequest done = new ChangeTaskStatusRequest(); done.setStatus(TaskStatus.DONE);
        mockMvc.perform(patch("/api/tasks/" + id + "/status").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(done))).andExpect(status().isOk());
        mockMvc.perform(delete("/api/tasks/" + id).header("Authorization", "Bearer " + employeeToken)).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/tasks/" + id).header("Authorization", "Bearer " + employee2Token)).andExpect(status().isForbidden());
        assertTrue(taskRepository.existsById(id));
    }

    @Test
    void deletingMissingTaskReturnsNotFound() throws Exception {
        mockMvc.perform(delete("/api/tasks/999999999").header("Authorization", "Bearer " + managerToken)).andExpect(status().isNotFound());
    }
}
