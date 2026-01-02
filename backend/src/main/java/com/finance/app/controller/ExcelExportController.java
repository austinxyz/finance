package com.finance.app.controller;

import com.finance.app.service.ExcelExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Excel导出控制器
 */
@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class ExcelExportController {

    private final ExcelExportService excelExportService;

    /**
     * 导出年度财务报表
     *
     * @param familyId 家庭ID
     * @param year     年份
     * @return Excel文件
     */
    @GetMapping("/annual-report")
    public ResponseEntity<byte[]> exportAnnualReport(
            @RequestParam Long familyId,
            @RequestParam Integer year) {

        log.info("收到导出年度报表请求: familyId={}, year={}", familyId, year);

        try {
            byte[] excelData = excelExportService.generateAnnualReport(familyId, year);

            String filename = String.format("财务报表_%d年_%s.xlsx",
                    year,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelData.length);

            log.info("年度报表导出成功: familyId={}, year={}, 文件大小={}bytes", familyId, year, excelData.length);

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (IOException e) {
            log.error("导出年度报表失败: familyId={}, year={}", familyId, year, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("导出年度报表发生未知错误: familyId={}, year={}", familyId, year, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
