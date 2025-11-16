package com.finance.app.service;

import com.finance.app.dto.UserProfileDTO;
import com.finance.app.model.UserProfile;
import com.finance.app.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    // 获取用户配置
    public UserProfileDTO getUserProfile(Long userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
        if (profile == null) {
            // 如果不存在，返回默认配置
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setEstimatedAnnualExpenses(BigDecimal.ZERO);
            profile.setEmergencyFundMonths(6);
            profile.setRiskTolerance(UserProfile.RiskTolerance.MODERATE);
        }
        return UserProfileDTO.fromEntity(profile);
    }

    // 创建或更新用户配置
    @Transactional
    public UserProfileDTO saveUserProfile(Long userId, UserProfileDTO dto) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElse(new UserProfile());

        profile.setUserId(userId);
        profile.setEstimatedAnnualExpenses(dto.getEstimatedAnnualExpenses());
        profile.setEmergencyFundMonths(dto.getEmergencyFundMonths());

        if (dto.getRiskTolerance() != null) {
            profile.setRiskTolerance(UserProfile.RiskTolerance.valueOf(dto.getRiskTolerance()));
        }

        profile.setNotes(dto.getNotes());

        UserProfile saved = userProfileRepository.save(profile);
        return UserProfileDTO.fromEntity(saved);
    }
}
