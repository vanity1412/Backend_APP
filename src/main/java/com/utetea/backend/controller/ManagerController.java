package com.utetea.backend.controller;

import com.utetea.backend.dto.ApiResponse;
import com.utetea.backend.dto.DashboardSummaryDto;
import com.utetea.backend.dto.ForecastDto;
import com.utetea.backend.dto.OrderDto;
import com.utetea.backend.model.OrderStatus;
import com.utetea.backend.service.ForecastService;
import com.utetea.backend.service.ManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "👔 Manager", description = "API quản lý cho Manager")
@Slf4j
public class ManagerController {
    
    private final ManagerService managerService;
    private final ForecastService forecastService;
    private final com.utetea.backend.service.ReviewService reviewService;
    
    @GetMapping("/test")
    @Operation(summary = "Test Auth", description = "Test authentication và role")
    public ResponseEntity<ApiResponse<String>> testAuth() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        log.info("========== TEST AUTH ==========");
        log.info("Authenticated: " + (auth != null));
        if (auth != null) {
            log.info("Principal: " + auth.getPrincipal());
            log.info("Authorities: " + auth.getAuthorities());
        }
        
        String message = auth != null ? 
            "Authenticated as: " + auth.getName() + ", Authorities: " + auth.getAuthorities() :
            "Not authenticated";
        
        return ResponseEntity.ok(ApiResponse.success(message));
    }
    
    @GetMapping("/summary")
    @Operation(summary = "Dashboard Summary", description = "Lấy tổng quan doanh thu, đơn hàng")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DashboardSummaryDto>> getDashboardSummary() {
        log.info("========== GET /api/manager/summary ==========");
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        log.info("User authenticated: " + auth);
        log.info("Authorities: " + auth.getAuthorities());
        
        DashboardSummaryDto summary = managerService.getDashboardSummary();
        return ResponseEntity.ok(ApiResponse.success("Dashboard loaded", summary));
    }
    
    @GetMapping("/orders")
    @Operation(summary = "Get Orders", description = "Lấy danh sách đơn hàng theo status")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderDto>>> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("GET /api/manager/orders - status: {}, page: {}", status, page);
        
        OrderStatus orderStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                orderStatus = OrderStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid status: " + status));
            }
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderDto> orders = managerService.getOrdersByStatus(orderStatus, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(orders));
    }
    
    @PutMapping("/orders/{orderId}/status")
    @Operation(summary = "Update Order Status", description = "Cập nhật trạng thái đơn hàng")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderDto>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status) {
        
        log.info("PUT /api/manager/orders/{}/status - new status: {}", orderId, status);
        
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Invalid status: " + status));
        }
        
        OrderDto updatedOrder = managerService.updateOrderStatus(orderId, newStatus);
        return ResponseEntity.ok(ApiResponse.success("Order status updated", updatedOrder));
    }
    
    // ==================== USER MANAGEMENT ====================
    
    @GetMapping("/users")
    @Operation(summary = "Get All Users", description = "Lấy danh sách tất cả người dùng")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<com.utetea.backend.dto.UserDto>>> getAllUsers(
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("GET /api/manager/users - role: {}, page: {}", role, page);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<com.utetea.backend.dto.UserDto> users = managerService.getAllUsers(role, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(users));
    }
    
    @GetMapping("/users/{userId}")
    @Operation(summary = "Get User Detail", description = "Lấy thông tin chi tiết người dùng")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<com.utetea.backend.dto.UserDto>> getUserById(
            @PathVariable Long userId) {
        
        log.info("GET /api/manager/users/{}", userId);
        
        com.utetea.backend.dto.UserDto user = managerService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }
    
    @PutMapping("/users/{userId}/block")
    @Operation(summary = "Block/Unblock User", description = "Khóa hoặc mở khóa tài khoản người dùng")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<com.utetea.backend.dto.UserDto>> toggleUserBlock(
            @PathVariable Long userId,
            @RequestParam boolean blocked) {
        
        log.info("PUT /api/manager/users/{}/block - blocked: {}", userId, blocked);
        
        com.utetea.backend.dto.UserDto user = managerService.toggleUserBlock(userId, blocked);
        String message = blocked ? "Đã khóa tài khoản" : "Đã mở khóa tài khoản";
        return ResponseEntity.ok(ApiResponse.success(message, user));
    }
    
    @GetMapping("/users/search")
    @Operation(summary = "Search Users", description = "Tìm kiếm người dùng theo từ khóa")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<com.utetea.backend.dto.UserDto>>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("GET /api/manager/users/search - keyword: {}", keyword);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<com.utetea.backend.dto.UserDto> users = managerService.searchUsers(keyword, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(users));
    }
    
    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Delete User", description = "Xóa tài khoản người dùng (ADMIN có thể xóa Manager)")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long userId) {
        log.info("DELETE /api/manager/users/{}", userId);
        
        managerService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa tài khoản người dùng thành công"));
    }
    
    @PutMapping("/users/{userId}/promote")
    @Operation(summary = "Promote to Manager", description = "Nâng cấp User lên làm Manager (CHỈ ADMIN)")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<com.utetea.backend.dto.UserDto>> promoteToManager(
            @PathVariable Long userId) {
        
        log.info("PUT /api/manager/users/{}/promote", userId);
        
        com.utetea.backend.dto.UserDto user = managerService.promoteToManager(userId);
        return ResponseEntity.ok(ApiResponse.success("Đã nâng cấp thành Manager thành công", user));
    }
    
    @PutMapping("/users/{userId}/demote")
    @Operation(summary = "Demote to User", description = "Hạ cấp Manager xuống User thường (CHỈ ADMIN)")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<com.utetea.backend.dto.UserDto>> demoteToUser(
            @PathVariable Long userId) {
        
        log.info("PUT /api/manager/users/{}/demote", userId);
        
        com.utetea.backend.dto.UserDto user = managerService.demoteToUser(userId);
        return ResponseEntity.ok(ApiResponse.success("Đã hạ cấp xuống User thành công", user));
    }
    
    // ==================== STORE ASSIGNMENT FOR MANAGER ====================
    
    @GetMapping("/my-stores")
    @Operation(summary = "Get My Managed Stores", description = "Lấy danh sách cửa hàng mà Manager hiện tại được quản lý")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<java.util.List<com.utetea.backend.dto.StoreDto>>> getMyManagedStores() {
        log.info("GET /api/manager/my-stores");
        
        var stores = managerService.getMyManagedStores();
        return ResponseEntity.ok(ApiResponse.success("Stores loaded", stores));
    }
    
    @GetMapping("/users/{userId}/stores")
    @Operation(summary = "Get Manager's Stores", description = "Lấy danh sách cửa hàng mà Manager được gán quản lý")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<java.util.List<com.utetea.backend.dto.StoreDto>>> getManagedStores(
            @PathVariable Long userId) {
        
        log.info("GET /api/manager/users/{}/stores", userId);
        
        var stores = managerService.getManagedStores(userId);
        return ResponseEntity.ok(ApiResponse.success("Stores loaded", stores));
    }
    
    @PostMapping("/users/{userId}/stores/{storeId}")
    @Operation(summary = "Assign Store to Manager", description = "Gán cửa hàng cho Manager quản lý (CHỈ ADMIN)")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<com.utetea.backend.dto.UserDto>> assignStoreToManager(
            @PathVariable Long userId,
            @PathVariable Long storeId) {
        
        log.info("POST /api/manager/users/{}/stores/{}", userId, storeId);
        
        com.utetea.backend.dto.UserDto user = managerService.assignStoreToManager(userId, storeId);
        return ResponseEntity.ok(ApiResponse.success("Đã gán cửa hàng cho Manager thành công", user));
    }
    
    @PutMapping("/users/{userId}/stores")
    @Operation(summary = "Assign Multiple Stores to Manager", description = "Gán nhiều cửa hàng cho Manager (CHỈ ADMIN)")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<com.utetea.backend.dto.UserDto>> assignStoresToManager(
            @PathVariable Long userId,
            @RequestBody java.util.List<Long> storeIds) {
        
        log.info("PUT /api/manager/users/{}/stores - storeIds: {}", userId, storeIds);
        
        com.utetea.backend.dto.UserDto user = managerService.assignStoresToManager(userId, storeIds);
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật danh sách cửa hàng cho Manager", user));
    }
    
    @DeleteMapping("/users/{userId}/stores/{storeId}")
    @Operation(summary = "Remove Store from Manager", description = "Bỏ gán cửa hàng khỏi Manager (CHỈ ADMIN)")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<com.utetea.backend.dto.UserDto>> removeStoreFromManager(
            @PathVariable Long userId,
            @PathVariable Long storeId) {
        
        log.info("DELETE /api/manager/users/{}/stores/{}", userId, storeId);
        
        com.utetea.backend.dto.UserDto user = managerService.removeStoreFromManager(userId, storeId);
        return ResponseEntity.ok(ApiResponse.success("Đã bỏ gán cửa hàng khỏi Manager", user));
    }
    
    @GetMapping("/users/{userId}/has-stores")
    @Operation(summary = "Check Has Assigned Stores", description = "Kiểm tra Manager có được gán store nào không")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> hasAssignedStores(@PathVariable Long userId) {
        log.info("GET /api/manager/users/{}/has-stores", userId);
        
        boolean hasStores = managerService.hasAssignedStores(userId);
        return ResponseEntity.ok(ApiResponse.success(hasStores));
    }
    
    // ==================== REVENUE STATISTICS ====================
    
    @GetMapping("/statistics/revenue")
    @Operation(summary = "Revenue Statistics", description = "Thống kê doanh thu theo ngày/tháng và top sản phẩm")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<com.utetea.backend.dto.RevenueStatisticsDto>> getRevenueStatistics(
            @RequestParam(defaultValue = "7") Integer days,
            @RequestParam(defaultValue = "6") Integer months) {
        
        log.info("GET /api/manager/statistics/revenue - days: {}, months: {}", days, months);
        
        com.utetea.backend.dto.RevenueStatisticsDto stats = managerService.getRevenueStatistics(days, months);
        return ResponseEntity.ok(ApiResponse.success("Statistics loaded", stats));
    }
    
    // ==================== CATEGORY MANAGEMENT ====================
    
    @PostMapping("/categories")
    @Operation(summary = "Create Category", description = "Tạo danh mục mới")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<com.utetea.backend.dto.DrinkCategoryDto>> createCategory(
            @RequestBody java.util.Map<String, String> categoryData) {
        
        log.info("POST /api/manager/categories - name: {}", categoryData.get("name"));
        
        com.utetea.backend.dto.DrinkCategoryDto dto = new com.utetea.backend.dto.DrinkCategoryDto();
        dto.setName(categoryData.get("name"));
        dto.setDescription(categoryData.get("description"));
        dto.setImageUrl(categoryData.get("imageUrl"));
        dto.setIsActive(true);
        dto.setDisplayOrder(0);
        
        com.utetea.backend.dto.DrinkCategoryDto created = managerService.createCategory(dto);
        return ResponseEntity.ok(ApiResponse.success("Category created", created));
    }
    
    @PutMapping("/categories/{id}")
    @Operation(summary = "Update Category", description = "Cập nhật danh mục")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<com.utetea.backend.dto.DrinkCategoryDto>> updateCategory(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> categoryData) {
        
        log.info("PUT /api/manager/categories/{} - name: {}", id, categoryData.get("name"));
        
        com.utetea.backend.dto.DrinkCategoryDto dto = new com.utetea.backend.dto.DrinkCategoryDto();
        dto.setName(categoryData.get("name"));
        dto.setDescription(categoryData.get("description"));
        dto.setImageUrl(categoryData.get("imageUrl"));
        dto.setIsActive(true);
        dto.setDisplayOrder(0);
        
        com.utetea.backend.dto.DrinkCategoryDto updated = managerService.updateCategory(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Category updated", updated));
    }
    
    @DeleteMapping("/categories/{id}")
    @Operation(summary = "Delete Category", description = "Xóa danh mục (soft delete)")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteCategory(@PathVariable Long id) {
        log.info("DELETE /api/manager/categories/{}", id);
        
        managerService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Category deleted"));
    }
    
    // ==================== FORECAST & ANALYTICS ====================
    
    @GetMapping("/forecast")
    @Operation(summary = "Full Forecast", description = "Dự báo doanh thu, giờ cao điểm, nhân sự và cảnh báo quá tải")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ForecastDto>> getFullForecast() {
        log.info("GET /api/manager/forecast");
        ForecastDto forecast = forecastService.getFullForecast();
        return ResponseEntity.ok(ApiResponse.success("Forecast loaded", forecast));
    }
    
    @GetMapping("/forecast/revenue")
    @Operation(summary = "Revenue Forecast", description = "Dự báo doanh thu theo ngày/tuần/tháng")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ForecastDto.RevenueForecast>> getRevenueForecast() {
        log.info("GET /api/manager/forecast/revenue");
        ForecastDto.RevenueForecast forecast = forecastService.calculateRevenueForecast();
        return ResponseEntity.ok(ApiResponse.success("Revenue forecast loaded", forecast));
    }
    
    @GetMapping("/forecast/peak-hours")
    @Operation(summary = "Peak Hours Analysis", description = "Phân tích giờ cao điểm")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<java.util.List<ForecastDto.PeakHourAnalysis>>> getPeakHours() {
        log.info("GET /api/manager/forecast/peak-hours");
        var peakHours = forecastService.analyzePeakHours();
        return ResponseEntity.ok(ApiResponse.success("Peak hours loaded", peakHours));
    }
    
    @GetMapping("/forecast/low-stock")
    @Operation(summary = "Low Stock Warnings", description = "Cảnh báo món sắp hết dựa trên tốc độ bán")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<java.util.List<ForecastDto.LowStockWarning>>> getLowStockWarnings() {
        log.info("GET /api/manager/forecast/low-stock");
        var warnings = forecastService.analyzeLowStock();
        return ResponseEntity.ok(ApiResponse.success("Low stock warnings loaded", warnings));
    }
    
    @GetMapping("/forecast/staffing")
    @Operation(summary = "Staffing Recommendations", description = "Đề xuất nhân sự theo ngày")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<java.util.List<ForecastDto.StaffingRecommendation>>> getStaffingRecommendations() {
        log.info("GET /api/manager/forecast/staffing");
        var recommendations = forecastService.generateStaffingRecommendations();
        return ResponseEntity.ok(ApiResponse.success("Staffing recommendations loaded", recommendations));
    }
    
    @GetMapping("/forecast/overload")
    @Operation(summary = "Overload Warnings", description = "Cảnh báo nguy cơ quá tải")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<java.util.List<ForecastDto.OverloadWarning>>> getOverloadWarnings() {
        log.info("GET /api/manager/forecast/overload");
        var warnings = forecastService.detectOverloadRisks();
        return ResponseEntity.ok(ApiResponse.success("Overload warnings loaded", warnings));
    }
    
    // ==================== REVIEW MANAGEMENT ====================
    
    @GetMapping("/reviews")
    @Operation(summary = "Get All Reviews", description = "Lấy tất cả đánh giá sản phẩm (bao gồm backup từ user đã xóa)")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<com.utetea.backend.dto.ReviewManagementDto>>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("GET /api/manager/reviews - page: {}, size: {}", page, size);
        
        org.springframework.data.domain.Pageable pageable = PageRequest.of(page, size);
        Page<com.utetea.backend.dto.ReviewManagementDto> reviews = reviewService.getAllReviewsForAdmin(pageable);
        
        return ResponseEntity.ok(ApiResponse.success("Reviews loaded", reviews));
    }
    
    @GetMapping("/reviews/drink/{drinkId}")
    @Operation(summary = "Get Reviews by Drink", description = "Lấy đánh giá theo sản phẩm (có thể bao gồm backup)")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<com.utetea.backend.dto.ReviewManagementDto>>> getReviewsByDrink(
            @PathVariable Long drinkId,
            @RequestParam(defaultValue = "true") boolean includeBackup,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("GET /api/manager/reviews/drink/{} - includeBackup: {}", drinkId, includeBackup);
        
        org.springframework.data.domain.Pageable pageable = PageRequest.of(page, size);
        Page<com.utetea.backend.dto.ReviewManagementDto> reviews = 
            reviewService.getReviewsByDrinkForAdmin(drinkId, includeBackup, pageable);
        
        return ResponseEntity.ok(ApiResponse.success("Reviews loaded", reviews));
    }
    
    @GetMapping("/reviews/drink/{drinkId}/statistics")
    @Operation(summary = "Get Review Statistics", description = "Lấy thống kê đánh giá tổng hợp của sản phẩm (bao gồm backup)")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<com.utetea.backend.dto.ReviewManagementDto.ReviewStatistics>> getReviewStatistics(
            @PathVariable Long drinkId) {
        
        log.info("GET /api/manager/reviews/drink/{}/statistics", drinkId);
        
        com.utetea.backend.dto.ReviewManagementDto.ReviewStatistics stats = 
            reviewService.getReviewStatisticsForAdmin(drinkId);
        
        return ResponseEntity.ok(ApiResponse.success("Statistics loaded", stats));
    }
    
    @GetMapping("/reviews/backup")
    @Operation(summary = "Get Backup Reviews", description = "Lấy tất cả đánh giá backup từ user đã xóa")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<com.utetea.backend.dto.ReviewManagementDto>>> getBackupReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("GET /api/manager/reviews/backup - page: {}, size: {}", page, size);
        
        org.springframework.data.domain.Pageable pageable = PageRequest.of(page, size);
        Page<com.utetea.backend.dto.ReviewManagementDto> reviews = reviewService.getBackupReviews(pageable);
        
        return ResponseEntity.ok(ApiResponse.success("Backup reviews loaded", reviews));
    }
    
    @DeleteMapping("/reviews/{reviewId}")
    @Operation(summary = "Delete Review", description = "Xóa đánh giá (chỉ xóa review hiện tại, không xóa backup)")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteReview(@PathVariable Long reviewId) {
        log.info("DELETE /api/manager/reviews/{}", reviewId);
        
        reviewService.deleteReviewByAdmin(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa đánh giá thành công"));
    }

}
