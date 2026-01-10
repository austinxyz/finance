package com.finance.app.service;

import com.finance.app.exception.ProtectedDataException;
import com.finance.app.model.Family;
import com.finance.app.repository.FamilyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 数据保护服务
 * 用于检查和验证对受保护家庭数据的操作
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataProtectionService {

    private final FamilyRepository familyRepository;

    /**
     * 检查家庭是否受保护
     */
    public boolean isFamilyProtected(Long familyId) {
        if (familyId == null) {
            return false;
        }

        return familyRepository.findById(familyId)
            .map(family -> Boolean.TRUE.equals(family.getIsProtected()))
            .orElse(false);
    }

    /**
     * 验证删除操作
     * 如果家庭受保护，抛出异常
     *
     * @param familyId 家庭ID
     * @param operation 操作描述
     * @throws ProtectedDataException 如果家庭受保护
     */
    public void validateDeleteOperation(Long familyId, String operation) {
        if (isFamilyProtected(familyId)) {
            log.warn("尝试删除受保护的家庭数据: familyId={}, operation={}", familyId, operation);
            throw new ProtectedDataException(familyId, operation);
        }
    }

    /**
     * 验证批量删除操作
     * 如果任何家庭受保护，抛出异常
     *
     * @param familyId 家庭ID
     * @param operation 操作描述
     * @param recordCount 影响的记录数
     * @throws ProtectedDataException 如果家庭受保护且影响多条记录
     */
    public void validateBatchDeleteOperation(Long familyId, String operation, int recordCount) {
        if (isFamilyProtected(familyId) && recordCount > 0) {
            log.warn("尝试批量删除受保护的家庭数据: familyId={}, operation={}, recordCount={}",
                familyId, operation, recordCount);
            throw new ProtectedDataException(
                familyId,
                operation,
                String.format("受保护的家庭数据不允许批量删除 %d 条记录 (操作: %s)", recordCount, operation)
            );
        }
    }

    /**
     * 记录保护验证日志
     */
    public void logProtectionCheck(Long familyId, String operation, boolean allowed) {
        if (isFamilyProtected(familyId)) {
            log.info("受保护家庭数据操作检查: familyId={}, operation={}, allowed={}",
                familyId, operation, allowed);
        }
    }
}
