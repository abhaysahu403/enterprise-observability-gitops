package com.enterprise.employee.repository;

import com.enterprise.employee.entity.Department;
import com.enterprise.employee.entity.Employee;
import com.enterprise.employee.entity.EmployeeStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a dynamic JPA Specification from optional search/filter parameters
 * so the controller can support search + department filter + status filter
 * + manager filter simultaneously without an explosion of repository methods.
 */
public final class EmployeeSpecifications {

    private EmployeeSpecifications() {
    }

    public static Specification<Employee> withFilters(String search, Department department,
                                                        EmployeeStatus status, Long managerId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                Predicate byFirst = cb.like(cb.lower(root.get("firstName")), like);
                Predicate byLast = cb.like(cb.lower(root.get("lastName")), like);
                Predicate byEmail = cb.like(cb.lower(root.get("email")), like);
                Predicate byCode = cb.like(cb.lower(root.get("employeeCode")), like);
                predicates.add(cb.or(byFirst, byLast, byEmail, byCode));
            }

            if (department != null) {
                predicates.add(cb.equal(root.get("department"), department));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (managerId != null) {
                predicates.add(cb.equal(root.get("manager").get("id"), managerId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
