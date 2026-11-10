package com.taskmanagesys.task.controller;

import com.taskmanagesys.auth.service.UserPrincipal;
import com.taskmanagesys.task.dto.TaskFileResponse;
import com.taskmanagesys.task.dto.TaskFileDetailResponse;
import com.taskmanagesys.task.service.TaskFileService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/tasks/{taskId}/files")
public class TaskFileController {
    private final TaskFileService service;
    public TaskFileController(TaskFileService service) { this.service=service; }
    @GetMapping public List<TaskFileResponse> list(@PathVariable Long taskId,@AuthenticationPrincipal UserPrincipal user){return service.list(taskId,user.getId());}
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public List<TaskFileResponse> upload(@PathVariable Long taskId,@RequestParam(required=false) Long subtaskId,@RequestPart("files") List<MultipartFile> files,@AuthenticationPrincipal UserPrincipal user){return service.upload(taskId,subtaskId,user.getId(),files);}
    @GetMapping("/{fileId}/download") public ResponseEntity<ByteArrayResource> download(@PathVariable Long taskId,@PathVariable Long fileId,@AuthenticationPrincipal UserPrincipal user){var d=service.download(taskId,fileId,user.getId()); return ResponseEntity.ok().contentType(MediaType.parseMediaType(d.contentType())).header(HttpHeaders.CONTENT_DISPOSITION,disposition(d.filename(),d.inline())).header("X-Content-Type-Options","nosniff").body(d.resource());}
    @GetMapping("/{fileId}/view-token") public String viewToken(@PathVariable Long taskId,@PathVariable Long fileId,@AuthenticationPrincipal UserPrincipal user){ return service.issueViewToken(taskId,fileId,user.getId()); }
    @GetMapping("/{fileId}/metadata") public TaskFileDetailResponse metadata(@PathVariable Long taskId,@PathVariable Long fileId,@RequestParam(required=false) Long subtaskId,@AuthenticationPrincipal UserPrincipal user){ return service.detail(taskId,fileId,user.getId(),subtaskId); }
    @GetMapping("/{fileId}/zip-entries") public List<TaskFileService.ZipEntryInfo> zipEntries(@PathVariable Long taskId,@PathVariable Long fileId,@AuthenticationPrincipal UserPrincipal user){ return service.zipEntries(taskId,fileId,user.getId()); }
    @GetMapping("/{fileId}/text") public ResponseEntity<String> text(@PathVariable Long taskId,@PathVariable Long fileId,@AuthenticationPrincipal UserPrincipal user){ var data=service.textPreview(taskId,fileId,user.getId()); return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/plain;charset=UTF-8")).header("X-Content-Truncated", Boolean.toString(data.truncated())).body(data.text()); }
    @GetMapping("/{fileId}/content") public ResponseEntity<byte[]> content(@PathVariable Long taskId,@PathVariable Long fileId,@RequestParam String token,@RequestHeader(value=HttpHeaders.RANGE, required=false) String range){ var d=service.signedContent(taskId,fileId,token); byte[] body=d.bytes(); HttpHeaders headers=new HttpHeaders(); headers.setContentType(MediaType.parseMediaType(d.contentType())); headers.set("X-Content-Type-Options","nosniff"); headers.set("X-Frame-Options","SAMEORIGIN"); headers.set(HttpHeaders.CONTENT_DISPOSITION,disposition(d.filename(),d.inline())); if(range==null || !range.startsWith("bytes=")) return ResponseEntity.ok().headers(headers).body(body); try { String[] p=range.substring(6).split("-",2); long start=Long.parseLong(p[0]), end=p.length>1&&!p[1].isBlank()?Long.parseLong(p[1]):body.length-1; if(start<0||start>=body.length||end<start) throw new IllegalArgumentException(); end=Math.min(end,body.length-1); byte[] part=java.util.Arrays.copyOfRange(body,(int)start,(int)end+1); headers.set(HttpHeaders.CONTENT_RANGE,"bytes "+start+"-"+end+"/"+body.length); headers.setContentLength(part.length); return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).headers(headers).body(part); } catch(Exception e) { return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).headers(headers).build(); } }
    @PutMapping("/{fileId}/content") public TaskFileResponse editContent(@PathVariable Long taskId,@PathVariable Long fileId,@RequestBody EditContentRequest request,@AuthenticationPrincipal UserPrincipal user){ return service.editText(taskId,fileId,user.getId(),request.content(),request.expectedUpdatedAt()); }
    @DeleteMapping("/{fileId}") public ResponseEntity<Void> delete(@PathVariable Long taskId,@PathVariable Long fileId,@AuthenticationPrincipal UserPrincipal user){service.delete(taskId,fileId,user.getId());return ResponseEntity.noContent().build();}
    @PatchMapping("/{fileId}") public TaskFileResponse rename(@PathVariable Long taskId,@PathVariable Long fileId,@RequestBody RenameRequest request,@AuthenticationPrincipal UserPrincipal user){return service.rename(taskId,fileId,user.getId(),request.name());}
    public record RenameRequest(String name) {}
    public record EditContentRequest(String content, String expectedUpdatedAt) {}
    private static String disposition(String filename, boolean inline) {
        String ascii = filename.replaceAll("[^A-Za-z0-9._ -]", "_").replace('"', '_');
        String encoded = java.net.URLEncoder.encode(filename, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
        return (inline ? "inline" : "attachment") + "; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded;
    }
}
