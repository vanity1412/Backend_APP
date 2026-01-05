package com.utetea.backend.service;

import com.utetea.backend.dto.*;
import com.utetea.backend.exception.ResourceNotFoundException;
import com.utetea.backend.model.*;
import com.utetea.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChallengeService {
    
    private final ChallengeRepository challengeRepository;
    private final ChallengeCompletionRepository challengeCompletionRepository;
    private final UserRepository userRepository;
    
    /**
     * Kiểm tra và xử lý challenge khi đơn hàng hoàn thành
     * Challenge: Mua 3 sản phẩm giống nhau trong 1 đơn hàng được cộng 5 điểm
     */
    @Transactional
    public List<ChallengeCompletionDto> processOrderChallenges(Order order) {
        List<ChallengeCompletionDto> completions = new ArrayList<>();
        User user = order.getUser();
        
        // Lấy challenge SAME_PRODUCT_IN_ORDER đang active
        Optional<Challenge> sameProductChallenge = challengeRepository
                .findByChallengeTypeAndIsActiveTrue(ChallengeType.SAME_PRODUCT_IN_ORDER);
        
        if (sameProductChallenge.isEmpty()) {
            log.info("No active SAME_PRODUCT_IN_ORDER challenge found");
            return completions;
        }
        
        Challenge challenge = sameProductChallenge.get();
        int requiredQty = challenge.getRequiredQuantity();
        
        // Kiểm tra từng item trong đơn hàng
        for (OrderItem item : order.getItems()) {
            if (item.getQuantity() >= requiredQty) {
                // Đạt điều kiện challenge!
                ChallengeCompletion completion = new ChallengeCompletion();
                completion.setUser(user);
                completion.setChallenge(challenge);
                completion.setOrder(order);
                completion.setPointsEarned(challenge.getRewardPoints());
                completion.setDrinkName(item.getDrinkNameSnapshot());
                completion.setQuantityAchieved(item.getQuantity());
                
                challengeCompletionRepository.save(completion);
                
                // Cộng điểm cho user
                int newPoints = user.getPoints() + challenge.getRewardPoints();
                userRepository.addPoints(user.getId(), challenge.getRewardPoints());
                
                log.info("🎯 Challenge completed! User {} bought {} x {} - earned {} points",
                        user.getUsername(), item.getQuantity(), item.getDrinkNameSnapshot(), 
                        challenge.getRewardPoints());
                
                completions.add(toCompletionDto(completion, challenge));
            }
        }
        
        return completions;
    }
    
    /**
     * Lấy trạng thái challenge của user
     */
    @Transactional(readOnly = true)
    public ChallengeStatusDto getUserChallengeStatus(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Lấy danh sách challenge đang active
        List<ChallengeDto> activeChallenges = challengeRepository.findByIsActiveTrue()
                .stream()
                .map(this::toChallengeDto)
                .collect(Collectors.toList());
        
        // Lấy lịch sử hoàn thành
        List<ChallengeCompletionDto> history = challengeCompletionRepository
                .findByUserIdWithChallenge(user.getId())
                .stream()
                .map(cc -> toCompletionDto(cc, cc.getChallenge()))
                .collect(Collectors.toList());
        
        // Tổng điểm từ challenge
        Integer totalPoints = challengeCompletionRepository.getTotalPointsEarnedByUserId(user.getId());
        
        return ChallengeStatusDto.builder()
                .activeChallenges(activeChallenges)
                .completionHistory(history)
                .totalPointsFromChallenges(totalPoints != null ? totalPoints : 0)
                .build();
    }
    
    /**
     * Lấy danh sách tất cả challenge (cho admin)
     */
    @Transactional(readOnly = true)
    public List<ChallengeDto> getAllChallenges() {
        return challengeRepository.findAll()
                .stream()
                .map(this::toChallengeDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Tạo challenge mới (cho admin)
     */
    @Transactional
    public ChallengeDto createChallenge(ChallengeDto dto) {
        Challenge challenge = new Challenge();
        challenge.setName(dto.getName());
        challenge.setDescription(dto.getDescription());
        challenge.setChallengeType(dto.getChallengeType());
        challenge.setRequiredQuantity(dto.getRequiredQuantity());
        challenge.setRewardPoints(dto.getRewardPoints());
        challenge.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        
        challenge = challengeRepository.save(challenge);
        log.info("Created new challenge: {}", challenge.getName());
        
        return toChallengeDto(challenge);
    }
    
    /**
     * Cập nhật challenge (cho admin)
     */
    @Transactional
    public ChallengeDto updateChallenge(Long id, ChallengeDto dto) {
        Challenge challenge = challengeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Challenge", "id", id));
        
        if (dto.getName() != null) challenge.setName(dto.getName());
        if (dto.getDescription() != null) challenge.setDescription(dto.getDescription());
        if (dto.getRequiredQuantity() != null) challenge.setRequiredQuantity(dto.getRequiredQuantity());
        if (dto.getRewardPoints() != null) challenge.setRewardPoints(dto.getRewardPoints());
        if (dto.getIsActive() != null) challenge.setIsActive(dto.getIsActive());
        
        challenge = challengeRepository.save(challenge);
        log.info("Updated challenge: {}", challenge.getName());
        
        return toChallengeDto(challenge);
    }
    
    /**
     * Khởi tạo challenge mặc định nếu chưa có
     */
    @Transactional
    public void initDefaultChallenges() {
        // Kiểm tra xem đã có challenge SAME_PRODUCT_IN_ORDER chưa
        List<Challenge> existing = challengeRepository.findByChallengeType(ChallengeType.SAME_PRODUCT_IN_ORDER);
        
        if (existing.isEmpty()) {
            Challenge defaultChallenge = new Challenge();
            defaultChallenge.setName("Mua 3 sản phẩm giống nhau");
            defaultChallenge.setDescription("Mua 3 sản phẩm giống nhau trong 1 đơn hàng để nhận 5 điểm thưởng");
            defaultChallenge.setChallengeType(ChallengeType.SAME_PRODUCT_IN_ORDER);
            defaultChallenge.setRequiredQuantity(3);
            defaultChallenge.setRewardPoints(5);
            defaultChallenge.setIsActive(true);
            
            challengeRepository.save(defaultChallenge);
            log.info("✅ Created default challenge: Mua 3 sản phẩm giống nhau");
        }
    }
    
    private ChallengeDto toChallengeDto(Challenge challenge) {
        return ChallengeDto.builder()
                .id(challenge.getId())
                .name(challenge.getName())
                .description(challenge.getDescription())
                .challengeType(challenge.getChallengeType())
                .requiredQuantity(challenge.getRequiredQuantity())
                .rewardPoints(challenge.getRewardPoints())
                .isActive(challenge.getIsActive())
                .build();
    }
    
    private ChallengeCompletionDto toCompletionDto(ChallengeCompletion cc, Challenge challenge) {
        return ChallengeCompletionDto.builder()
                .id(cc.getId())
                .challengeId(challenge.getId())
                .challengeName(challenge.getName())
                .orderId(cc.getOrder().getId())
                .pointsEarned(cc.getPointsEarned())
                .drinkName(cc.getDrinkName())
                .quantityAchieved(cc.getQuantityAchieved())
                .completedAt(cc.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime())
                .build();
    }
}
