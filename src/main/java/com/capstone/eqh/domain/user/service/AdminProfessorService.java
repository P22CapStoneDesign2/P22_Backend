package com.capstone.eqh.domain.user.service;

import com.capstone.eqh.domain.user.dto.response.PendingProfessorResponseDto;
import com.capstone.eqh.domain.user.dto.response.ProfessorStatusResponseDto;
import com.capstone.eqh.domain.user.entity.User;
import com.capstone.eqh.domain.user.enums.Role;
import com.capstone.eqh.domain.user.enums.UserStatus;
import com.capstone.eqh.domain.user.repository.UserRepository;
import com.capstone.eqh.global.exception.CustomException;
import com.capstone.eqh.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProfessorService {

    private final UserRepository userRepository;

    public Page<PendingProfessorResponseDto> listPendingProfessors(Pageable pageable) {
        return userRepository.findByRoleAndStatus(Role.PROF, UserStatus.PENDING, pageable)
                .map(PendingProfessorResponseDto::from);
    }

    @Transactional
    public ProfessorStatusResponseDto approveProfessor(Long professorId) {
        User professor = findProfessor(professorId);
        professor.updateStatus(UserStatus.ACTIVE);
        return ProfessorStatusResponseDto.from(professor);
    }

    @Transactional
    public ProfessorStatusResponseDto rejectProfessor(Long professorId) {
        User professor = findProfessor(professorId);
        professor.updateStatus(UserStatus.REJECTED);
        return ProfessorStatusResponseDto.from(professor);
    }

    @Transactional
    public ProfessorStatusResponseDto changeProfessorStatus(Long professorId, UserStatus status) {
        User professor = findProfessor(professorId);
        professor.updateStatus(status);
        return ProfessorStatusResponseDto.from(professor);
    }

    private User findProfessor(Long professorId) {
        return userRepository.findById(professorId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
