package com.utetea.backend.service;

import com.utetea.backend.dto.*;
import com.utetea.backend.exception.BadRequestException;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.mapper.DrinkMapper;
import com.utetea.backend.model.*;
import com.utetea.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoyaltyService {
    
    private final UserRepository userRepository;
    private final DrinkRepository drinkRepository;
    private final SpinRewardRepository spinRewardRepository;
    private final DrinkMapper drinkMapper;
    
    private static final int POINTS_TO_SPIN = 5;
    
    /**
     * Lấy thông tin điểm của user
     */
    public UserPointsDto getUserPoints(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        List<SpinRewardDto> rewards = spinRewardRepository.findByUserIdAndIsRedeemedFalse(user.getId())
                .stream()
                .map(this::toSpinRewardDto)
                .collect(Collectors.toList());
        
        return UserPointsDto.builder()
                .currentPoints(user.getPoints())
                .pointsToSpin(POINTS_TO_SPIN)
                .canSpin(user.getPoints() >= POINTS_TO_SPIN)
                .availableRewards(rewards)
                .build();
    }
    
    /**
     * Cộng điểm khi thanh toán thành công
     */
    @Transactional
    public Integer addPointForOrder(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        user.setPoints(user.getPoints() + 1);
        userRepository.save(user);
        
        return user.getPoints();
    }
    
    /**
     * Quay vòng xoay may mắn
     */
    @Transactional
    public SpinWheelResponse spinWheel(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        if (user.getPoints() < POINTS_TO_SPIN) {
            throw new BadRequestException("Không đủ điểm để quay. Cần " + POINTS_TO_SPIN + " điểm.");
        }
        
        // Lấy random 5 món nước active
        List<Drink> allDrinks = drinkRepository.findByIsActiveTrue();
        if (allDrinks.size() < 5) {
            throw new BadRequestException("Không đủ sản phẩm để quay thưởng");
        }
        
        // Shuffle và lấy 5 món
        Collections.shuffle(allDrinks);
        List<Drink> wheelDrinks = allDrinks.subList(0, 5);
        
        // Random vị trí trúng thưởng (0-4)
        Random random = new Random();
        int winIndex = random.nextInt(5);
        Drink wonDrink = wheelDrinks.get(winIndex);
        
        // Trừ điểm
        user.setPoints(user.getPoints() - POINTS_TO_SPIN);
        userRepository.save(user);
        
        // Lưu phần thưởng
        SpinReward reward = new SpinReward();
        reward.setUser(user);
        reward.setWonDrink(wonDrink);
        reward.setPointsUsed(POINTS_TO_SPIN);
        reward.setIsRedeemed(false);
        reward = spinRewardRepository.save(reward);
        
        // Convert to DTOs
        List<DrinkDto> wheelDrinkDtos = wheelDrinks.stream()
                .map(drinkMapper::toDto)
                .collect(Collectors.toList());
        
        return SpinWheelResponse.builder()
                .rewardId(reward.getId())
                .wonDrink(drinkMapper.toDto(wonDrink))
                .winIndex(winIndex)
                .wheelDrinks(wheelDrinkDtos)
                .remainingPoints(user.getPoints())
                .build();
    }
    
    /**
     * Lấy danh sách phần thưởng chưa sử dụng
     */
    public List<SpinRewardDto> getAvailableRewards(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        return spinRewardRepository.findByUserIdAndIsRedeemedFalse(user.getId())
                .stream()
                .map(this::toSpinRewardDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Sử dụng phần thưởng (đánh dấu đã dùng)
     */
    @Transactional
    public void redeemReward(String username, Long rewardId, Long orderId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        SpinReward reward = spinRewardRepository.findByIdAndUserIdAndIsRedeemedFalse(rewardId, user.getId())
                .orElseThrow(() -> new BadRequestException("Phần thưởng không tồn tại hoặc đã được sử dụng"));
        
        reward.setIsRedeemed(true);
        spinRewardRepository.save(reward);
    }
    
    /**
     * Kiểm tra user có phần thưởng cho drink này không
     */
    public SpinRewardDto getRewardForDrink(String username, Long drinkId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        return spinRewardRepository.findByUserIdAndIsRedeemedFalse(user.getId())
                .stream()
                .filter(r -> r.getWonDrink().getId().equals(drinkId))
                .findFirst()
                .map(this::toSpinRewardDto)
                .orElse(null);
    }
    
    private SpinRewardDto toSpinRewardDto(SpinReward reward) {
        return SpinRewardDto.builder()
                .id(reward.getId())
                .drinkId(reward.getWonDrink().getId())
                .drinkName(reward.getWonDrink().getName())
                .drinkImage(reward.getWonDrink().getImageUrl())
                .drinkPrice(reward.getWonDrink().getBasePrice().doubleValue())
                .isRedeemed(reward.getIsRedeemed())
                .createdAt(reward.getCreatedAt())
                .build();
    }
}
