package com.looksee.auditService.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.auditService.models.Brand;
import com.looksee.auditService.models.repository.BrandRepository;


@Service
public class BrandService {

	@Autowired
	private BrandRepository brand_repo;
	
	public Brand save(Brand brand){
		return brand_repo.save(brand);
	}
}
