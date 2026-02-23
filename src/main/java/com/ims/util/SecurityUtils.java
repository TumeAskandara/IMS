package com.ims.util;

import com.ims.entity.Branch;
import com.ims.entity.User;
import com.ims.enums.Role;
import com.ims.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;
    private User currentUser;

    public User getCurrentUser() {
        if (currentUser == null) {
            String username = SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            currentUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
        }
        return currentUser;
    }

    public Long getCurrentBranchId() {
        User user = getCurrentUser();
        if (user.getBranch() == null) return null;
        return user.getBranch().getId();
    }

    public Branch getCurrentBranch() {
        return getCurrentUser().getBranch();
    }

    public boolean isAdmin() {
        return getCurrentUser().getRole() == Role.ADMIN;
    }

    /**
     * Resolve the effective branchId for a request.
     * - ADMIN: pass through as-is (including null for "all branches")
     * - Non-admin with null: returns their own branchId (auto-scope)
     * - Non-admin with matching branchId: pass through
     * - Non-admin with different branchId: throw 403
     */
    public Long resolveBranchId(Long requestedBranchId) {
        if (isAdmin()) {
            return requestedBranchId;
        }
        Long userBranchId = getCurrentBranchId();
        if (requestedBranchId == null) {
            return userBranchId;
        }
        if (!requestedBranchId.equals(userBranchId)) {
            throw new AccessDeniedException(
                    "Access denied: you can only access data for your own branch");
        }
        return requestedBranchId;
    }

    /**
     * Validate that the current user has access to a specific branch.
     * Throws 403 if a non-admin tries to access another branch.
     */
    public void validateBranchAccess(Long branchId) {
        if (isAdmin()) return;
        Long userBranchId = getCurrentBranchId();
        if (!branchId.equals(userBranchId)) {
            throw new AccessDeniedException(
                    "Access denied: you can only access data for your own branch");
        }
    }

    /**
     * Validate transfer access — allows if user's branch is source OR destination.
     */
    public void validateTransferAccess(Long sourceBranchId, Long destBranchId) {
        if (isAdmin()) return;
        Long userBranchId = getCurrentBranchId();
        if (!userBranchId.equals(sourceBranchId) && !userBranchId.equals(destBranchId)) {
            throw new AccessDeniedException(
                    "Access denied: you can only access transfers involving your branch");
        }
    }
}
