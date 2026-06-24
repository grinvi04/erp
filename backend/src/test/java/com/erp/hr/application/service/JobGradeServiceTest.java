package com.erp.hr.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.hr.application.dto.JobGradeCreateRequest;
import com.erp.hr.application.dto.JobGradeResponse;
import com.erp.hr.domain.model.JobGrade;
import com.erp.hr.domain.repository.EmployeeRepository;
import com.erp.hr.domain.repository.JobGradeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class JobGradeServiceTest {

    @Mock
    private JobGradeRepository jobGradeRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private JobGradeService jobGradeService;

    @Test
    void create_newCode_returnsCreatedJobGrade() {
        given(jobGradeRepository.existsByCode("G3")).willReturn(false);
        JobGrade grade = JobGrade.of("G3", "3급", 3,
            new BigDecimal("3000000"), new BigDecimal("5000000"));
        given(jobGradeRepository.save(any())).willReturn(grade);

        JobGradeResponse result = jobGradeService.create(
            new JobGradeCreateRequest("G3", "3급", 3,
                new BigDecimal("3000000"), new BigDecimal("5000000")));

        assertThat(result.code()).isEqualTo("G3");
        assertThat(result.gradeOrder()).isEqualTo(3);
    }

    @Test
    void create_duplicateCode_throwsDuplicateCode() {
        given(jobGradeRepository.existsByCode("G3")).willReturn(true);

        ErpException ex = assertThrows(ErpException.class, () ->
            jobGradeService.create(new JobGradeCreateRequest("G3", "3급", 3, null, null)));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_CODE);
    }

    @Test
    void delete_gradeInUse_throwsJobGradeInUse() {
        JobGrade grade = JobGrade.of("G3", "3급", 3, null, null);
        given(jobGradeRepository.findById(1L)).willReturn(Optional.of(grade));
        given(employeeRepository.existsByJobGradeId(1L)).willReturn(true);

        ErpException ex = assertThrows(ErpException.class, () -> jobGradeService.delete(1L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.JOB_GRADE_IN_USE);
    }

    @Test
    void delete_notInUse_callsSoftDelete() {
        JobGrade grade = JobGrade.of("G3", "3급", 3, null, null);
        given(jobGradeRepository.findById(1L)).willReturn(Optional.of(grade));
        given(employeeRepository.existsByJobGradeId(1L)).willReturn(false);

        jobGradeService.delete(1L);

        assertThat(grade.isDeleted()).isTrue();
    }
}
