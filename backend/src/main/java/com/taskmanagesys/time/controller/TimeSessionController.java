package com.taskmanagesys.time.controller;

import com.taskmanagesys.auth.service.UserPrincipal;
import com.taskmanagesys.time.dto.TimeSessionRequest;
import com.taskmanagesys.time.dto.TimeSessionResponse;
import com.taskmanagesys.time.dto.TimeRangeResponse;
import com.taskmanagesys.time.service.TimeSessionService;
import com.taskmanagesys.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/time-sessions")
public class TimeSessionController {
    private final TimeSessionService service;
    public TimeSessionController(TimeSessionService service){this.service=service;}
    @GetMapping public TimeRangeResponse list(@AuthenticationPrincipal UserPrincipal user,@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to){return service.list(user.getId(),from,to);}
    @GetMapping("/task-options") public PageResponse<com.taskmanagesys.time.dto.TimeTaskOptionResponse> taskOptions(@AuthenticationPrincipal UserPrincipal user,@RequestParam(defaultValue="") String query,@RequestParam(defaultValue="0") int page){return PageResponse.from(service.taskOptions(user.getId(),query,PageRequest.of(Math.max(0,page),20,Sort.by(Sort.Direction.DESC,"createdAt"))));}
    @GetMapping("/task-options/{taskId}/subtasks") public List<com.taskmanagesys.time.dto.TimeSubtaskOptionResponse> subtaskOptions(@AuthenticationPrincipal UserPrincipal user,@PathVariable Long taskId){return service.subtaskOptions(user.getId(),taskId);}
    @PostMapping("/start") public TimeSessionResponse start(@AuthenticationPrincipal UserPrincipal user,@RequestBody TimeSessionRequest request){return service.start(user.getId(),request.getTaskId(),request.getSubtaskId(),request.isConfirmSwitch(),request.isConfirmOvertime(),request.getStartNote());}
    @PostMapping("/manual") public TimeSessionResponse manual(@AuthenticationPrincipal UserPrincipal user,@Valid @RequestBody TimeSessionRequest request){return service.manual(user.getId(),request);}
    @PutMapping("/{id}/manual") public TimeSessionResponse edit(@AuthenticationPrincipal UserPrincipal user,@PathVariable Long id,@Valid @RequestBody TimeSessionRequest request){return service.editManual(user.getId(),id,request);}
    @DeleteMapping("/{id}/manual") public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal user,@PathVariable Long id){service.deleteManual(user.getId(),id);return ResponseEntity.noContent().build();}
    @PostMapping("/{id}/pause") public TimeSessionResponse pause(@AuthenticationPrincipal UserPrincipal user,@PathVariable Long id){return service.pause(user.getId(),id);}
    @PostMapping("/{id}/resume") public TimeSessionResponse resume(@AuthenticationPrincipal UserPrincipal user,@PathVariable Long id){return service.resume(user.getId(),id);}
    @PostMapping("/{id}/stop") public TimeSessionResponse stop(@AuthenticationPrincipal UserPrincipal user,@PathVariable Long id){return service.stop(user.getId(),id);}
    @PostMapping("/{id}/heartbeat") public TimeSessionResponse heartbeat(@AuthenticationPrincipal UserPrincipal user,@PathVariable Long id){return service.heartbeat(user.getId(),id);}
}
