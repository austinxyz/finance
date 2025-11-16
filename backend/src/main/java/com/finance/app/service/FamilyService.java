package com.finance.app.service;

import com.finance.app.dto.FamilyDTO;
import com.finance.app.model.Family;
import com.finance.app.model.User;
import com.finance.app.repository.FamilyRepository;
import com.finance.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FamilyService {

    private final FamilyRepository familyRepository;
    private final UserRepository userRepository;

    /**
     * 获取所有家庭列表
     */
    @Transactional(readOnly = true)
    public List<FamilyDTO> getAllFamilies() {
        List<Family> families = familyRepository.findAll();
        return families.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据用户ID获取家庭信息
     * 由于User表中有family_id，需要先查user再查family
     */
    @Transactional(readOnly = true)
    public FamilyDTO getFamilyByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getFamilyId() == null) {
            throw new RuntimeException("用户未关联任何家庭");
        }

        Family family = familyRepository.findById(user.getFamilyId())
                .orElseThrow(() -> new RuntimeException("家庭不存在"));

        return convertToDTO(family);
    }

    /**
     * 根据家庭ID获取家庭信息
     */
    @Transactional(readOnly = true)
    public FamilyDTO getFamilyById(Long familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new RuntimeException("家庭不存在"));

        return convertToDTO(family);
    }

    /**
     * 保存或更新家庭信息
     */
    @Transactional
    public FamilyDTO saveFamily(Long familyId, FamilyDTO dto) {
        Family family;

        if (familyId != null) {
            // 更新现有家庭
            family = familyRepository.findById(familyId)
                    .orElseThrow(() -> new RuntimeException("家庭不存在"));

            family.setFamilyName(dto.getFamilyName());
            family.setAnnualExpenses(dto.getAnnualExpenses());
            family.setExpensesCurrency(dto.getExpensesCurrency());
            family.setEmergencyFundMonths(dto.getEmergencyFundMonths());
            family.setFinancialGoals(dto.getFinancialGoals());
        } else {
            // 创建新家庭
            family = new Family();
            family.setFamilyName(dto.getFamilyName());
            family.setAnnualExpenses(dto.getAnnualExpenses());
            family.setExpensesCurrency(dto.getExpensesCurrency());
            family.setEmergencyFundMonths(dto.getEmergencyFundMonths());
            family.setFinancialGoals(dto.getFinancialGoals());
        }

        Family saved = familyRepository.save(family);
        return convertToDTO(saved);
    }

    /**
     * 将Entity转换为DTO
     */
    private FamilyDTO convertToDTO(Family family) {
        FamilyDTO dto = new FamilyDTO();
        dto.setId(family.getId());
        dto.setFamilyName(family.getFamilyName());
        dto.setAnnualExpenses(family.getAnnualExpenses());
        dto.setExpensesCurrency(family.getExpensesCurrency());
        dto.setEmergencyFundMonths(family.getEmergencyFundMonths());
        dto.setFinancialGoals(family.getFinancialGoals());
        return dto;
    }
}
