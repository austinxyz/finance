package com.finance.app.service;

import com.finance.app.model.PropertyRecord;
import com.finance.app.repository.PropertyRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PropertyRecordService {

    @Autowired
    private PropertyRecordRepository propertyRecordRepository;

    /**
     * 创建房产记录
     */
    @Transactional
    public PropertyRecord create(PropertyRecord propertyRecord) {
        // 检查资产账户是否已存在房产记录
        Optional<PropertyRecord> existing = propertyRecordRepository
                .findByAssetAccountId(propertyRecord.getAssetAccountId());
        if (existing.isPresent()) {
            throw new RuntimeException("该资产账户已存在房产记录");
        }
        return propertyRecordRepository.save(propertyRecord);
    }

    /**
     * 更新房产记录
     */
    @Transactional
    public PropertyRecord update(Long id, PropertyRecord propertyRecord) {
        PropertyRecord existing = propertyRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("房产记录不存在"));

        // 更新字段
        existing.setPurchaseDate(propertyRecord.getPurchaseDate());
        existing.setDownPayment(propertyRecord.getDownPayment());
        existing.setMortgageAmount(propertyRecord.getMortgageAmount());
        existing.setPropertyValue(propertyRecord.getPropertyValue());
        existing.setCurrency(propertyRecord.getCurrency());
        existing.setNotes(propertyRecord.getNotes());

        PropertyRecord saved = propertyRecordRepository.save(existing);
        // 清除关联对象避免序列化问题
        saved.setAssetAccount(null);
        return saved;
    }

    /**
     * 删除房产记录
     */
    @Transactional
    public void delete(Long id) {
        propertyRecordRepository.deleteById(id);
    }

    /**
     * 根据ID查找
     */
    public Optional<PropertyRecord> findById(Long id) {
        Optional<PropertyRecord> record = propertyRecordRepository.findById(id);
        record.ifPresent(pr -> pr.setAssetAccount(null));
        return record;
    }

    /**
     * 根据资产账户ID查找
     */
    public Optional<PropertyRecord> findByAssetAccountId(Long assetAccountId) {
        Optional<PropertyRecord> record = propertyRecordRepository.findByAssetAccountId(assetAccountId);
        record.ifPresent(pr -> pr.setAssetAccount(null));
        return record;
    }

    /**
     * 查找家庭所有房产记录
     */
    public List<PropertyRecord> findByFamilyId(Long familyId) {
        List<PropertyRecord> records = propertyRecordRepository.findByFamilyId(familyId);
        records.forEach(pr -> pr.setAssetAccount(null));
        return records;
    }

    /**
     * 查找指定年份购买的房产记录
     */
    public List<PropertyRecord> findByFamilyIdAndPurchaseYear(Long familyId, Integer year) {
        List<PropertyRecord> records = propertyRecordRepository.findByFamilyIdAndPurchaseYear(familyId, year);
        records.forEach(pr -> pr.setAssetAccount(null));
        return records;
    }
}
