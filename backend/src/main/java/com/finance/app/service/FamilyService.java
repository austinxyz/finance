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
     * 获取默认家庭
     */
    @Transactional(readOnly = true)
    public FamilyDTO getDefaultFamily() {
        Family family = familyRepository.findByIsDefaultTrue()
                .orElseGet(() -> {
                    // 如果没有设置默认家庭，返回第一个家庭
                    List<Family> families = familyRepository.findAll();
                    if (families.isEmpty()) {
                        throw new RuntimeException("系统中没有任何家庭");
                    }
                    return families.get(0);
                });

        return convertToDTO(family);
    }

    /**
     * 设置默认家庭
     */
    @Transactional
    public void setDefaultFamily(Long familyId) {
        // 验证家庭存在
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new RuntimeException("家庭不存在"));

        // 取消所有家庭的默认设置
        List<Family> allFamilies = familyRepository.findAll();
        allFamilies.forEach(f -> f.setIsDefault(false));
        familyRepository.saveAll(allFamilies);

        // 设置新的默认家庭
        family.setIsDefault(true);
        familyRepository.save(family);
    }

    /**
     * 将Entity转换为DTO
     */
    private FamilyDTO convertToDTO(Family family) {
        FamilyDTO dto = new FamilyDTO();
        dto.setId(family.getId());
        dto.setFamilyName(family.getFamilyName());
        dto.setIsDefault(family.getIsDefault());
        dto.setAnnualExpenses(family.getAnnualExpenses());
        dto.setExpensesCurrency(family.getExpensesCurrency());
        dto.setEmergencyFundMonths(family.getEmergencyFundMonths());
        dto.setFinancialGoals(family.getFinancialGoals());
        dto.setDescription(family.getFinancialGoals()); // Use financial goals as description for now
        dto.setCreatedAt(family.getCreatedAt());

        // 查询该家庭的所有成员
        List<User> members = userRepository.findByFamilyId(family.getId());
        List<com.finance.app.dto.UserDTO> memberDTOs = members.stream()
                .map(this::convertUserToDTO)
                .collect(Collectors.toList());
        dto.setMembers(memberDTOs);

        return dto;
    }

    /**
     * 将User Entity转换为UserDTO
     */
    private com.finance.app.dto.UserDTO convertUserToDTO(User user) {
        com.finance.app.dto.UserDTO dto = new com.finance.app.dto.UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRole(user.getRole() != null ? user.getRole().name() : null);
        dto.setFamilyId(user.getFamilyId());
        dto.setIsActive(user.getIsActive());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setBirthDate(user.getBirthDate());
        dto.setAnnualIncome(user.getAnnualIncome());
        dto.setIncomeCurrency(user.getIncomeCurrency());
        dto.setRiskTolerance(user.getRiskTolerance() != null ? user.getRiskTolerance().name() : null);
        dto.setNotes(user.getNotes());
        return dto;
    }
}
