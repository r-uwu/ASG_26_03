package com.example.demo.controller.customerCenter;

import com.example.demo.entity.myPage.Inquiry;
import com.example.demo.repository.customerCenter.ReplyRepository;
import com.example.demo.repository.myPage.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import com.example.demo.entity.customerCenter.Reply;
import com.example.demo.repository.customerCenter.ReplyRepository;
import java.util.LinkedHashMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryRepository inquiryRepository;
    private final ReplyRepository replyRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * 사용자 문의 등록
     * POST /api/public/inquiries
     */
    @PostMapping(value = "/api/public/inquiries", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> create(
            @RequestParam("type")  String type,
            @RequestParam("email") String email,
            @RequestParam("title") String title,
            @RequestParam("body")  String body,
            @RequestParam(value = "files", required = false) MultipartFile[] files
    ) throws IOException {

        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        List<String> originalNames = new ArrayList<>();
        List<String> savedNames    = new ArrayList<>();

        if (files != null) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) continue;

                String originalName = file.getOriginalFilename();
                String savedName    = UUID.randomUUID() + "_" + originalName;

                file.transferTo(new File(dir, savedName));

                originalNames.add(originalName);
                savedNames.add(savedName);
            }
        }

        Inquiry inquiry = new Inquiry();
        inquiry.setType(type);
        inquiry.setEmail(email);
        inquiry.setTitle(title);
        inquiry.setBody(body);
        inquiry.setContent(body);   // myPage InquiryResponse 호환
        inquiry.setStatus("미처리");
        inquiry.setAttachmentNames(originalNames.isEmpty() ? null : String.join(",", originalNames));
        inquiry.setAttachmentSavedNames(savedNames.isEmpty()  ? null : String.join(",", savedNames));

        Inquiry saved = inquiryRepository.save(inquiry);

        return Map.of("result", "ok", "id", saved.getId());
    }

    /**
     * 사용자 본인 문의 조회
     * GET /api/inquiry?email=test@test.com
     */
    @GetMapping("/api/inquiry")
    public List<Map<String, Object>> getMyInquiries(@RequestParam("email") String email) {
        List<Inquiry> inquiries = inquiryRepository.findByEmailOrderByCreatedAtDesc(email);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Inquiry inquiry : inquiries) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id",     inquiry.getId());
            item.put("type",   inquiry.getType());
            item.put("email",  inquiry.getEmail());
            item.put("title",  inquiry.getTitle());
            item.put("body",   inquiry.getBody() != null ? inquiry.getBody() : inquiry.getContent());
            item.put("status", inquiry.getStatus());
            item.put("attachmentNames", parseAttachmentNames(inquiry.getAttachmentNames()));
            item.put("replies", replyRepository.findByInquiryIdOrderByIdAsc(inquiry.getId()));
            result.add(item);
        }
        return result;
    }

    /**
     * 관리자 전체 문의 조회
     * GET /api/admin/inquiries
     */
 // 수정 후
    @GetMapping("/api/admin/inquiries")
    public List<Map<String, Object>> getAll() {
        List<Inquiry> inquiries = inquiryRepository.findAllByOrderByIdDesc();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Inquiry inq : inquiries) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id",                  inq.getId());
            item.put("type",                inq.getType());
            item.put("email",               inq.getEmail());
            item.put("title",               inq.getTitle());
            item.put("body",                inq.getBody() != null ? inq.getBody() : inq.getContent());
            item.put("status",              inq.getStatus());
            item.put("createdAt",           inq.getCreatedAt());
            item.put("attachmentNames",     parseAttachmentNames(inq.getAttachmentNames()));
            item.put("replies",             replyRepository.findByInquiryIdOrderByIdAsc(inq.getId()));
            result.add(item);
        }
        return result;
    }

    /**
     * 관리자 상태 변경
     * PATCH /api/admin/inquiries/{id}/status
     */
    @PatchMapping("/api/admin/inquiries/{id}/status")
    public Inquiry updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("문의 없음"));

        inquiry.setStatus(body.get("status"));
        return inquiryRepository.save(inquiry);
    }

    /**
     * 관리자 문의 삭제
     * DELETE /api/admin/inquiries/{id}
     */
    @DeleteMapping("/api/admin/inquiries/{id}")
    @Transactional
    public Map<String, String> deleteInquiry(@PathVariable Long id) {
        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("문의 없음"));

        // 첨부파일 로컬 삭제
        if (inquiry.getAttachmentSavedNames() != null && !inquiry.getAttachmentSavedNames().isBlank()) {
            for (String savedName : inquiry.getAttachmentSavedNames().split(",")) {
                File file = new File(uploadDir, savedName.trim());
                if (file.exists()) file.delete();
            }
        }

        replyRepository.deleteByInquiryId(id);
        inquiryRepository.deleteById(id);

        return Map.of("result", "ok");
    }

    /**
     * 관리자 첨부파일 다운로드
     * GET /api/admin/inquiries/{id}/attachments/{index}
     */
    @GetMapping("/api/admin/inquiries/{id}/attachments/{index}")
    public ResponseEntity<InputStreamResource> downloadAttachment(
            @PathVariable Long id,
            @PathVariable int index
    ) throws IOException {

        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("문의 없음"));

        if (inquiry.getAttachmentNames() == null || inquiry.getAttachmentSavedNames() == null) {
            throw new RuntimeException("첨부파일 없음");
        }

        String[] originalNames = inquiry.getAttachmentNames().split(",");
        String[] savedNames    = inquiry.getAttachmentSavedNames().split(",");

        if (index < 0 || index >= savedNames.length) {
            throw new RuntimeException("파일 인덱스 범위 초과");
        }

        String savedName    = savedNames[index].trim();
        String originalName = originalNames[index].trim();

        File file = new File(uploadDir, savedName);
        if (!file.exists()) throw new RuntimeException("파일을 찾을 수 없음");

        String encodedName = URLEncoder.encode(originalName, StandardCharsets.UTF_8)
                .replace("+", "%20");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(encodedName, StandardCharsets.UTF_8).build()
        );
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(new FileInputStream(file)));
    }

    // ── 내부 유틸 ──────────────────────────────────────────
    private List<String> parseAttachmentNames(String names) {
        if (names == null || names.isBlank()) return List.of();
        return Arrays.stream(names.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
