package com.looksee.auditService.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.auditService.models.repository.DesignSystemRepository;
import com.looksee.auditService.models.DesignSystem;

/**
 * Contains business logic for interacting with and managing accounts
 *
 */
@Service
public class DesignSystemService {

	@Autowired
	private DesignSystemRepository design_system_repo;

	public DesignSystem save(DesignSystem design_system) {
		return design_system_repo.save(design_system);
	}

	public Optional<DesignSystem> findById(long id) {
		return design_system_repo.findById(id);
	}
}
