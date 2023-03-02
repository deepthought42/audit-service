package com.looksee.auditService.models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Brand extends LookseeObject {
	private List<String> colors;
	
	public Brand() {
		setColors(new ArrayList<String>());
	}
	
	public Brand( List<String> colors ) {
		super();
		setColors(colors);
	}
	
	@Override
	public String generateKey() {
		return "brand"+UUID.randomUUID();
	}

	public List<String> getColors() {
		return colors;
	}

	public void setColors(List<String> colors) {
		this.colors = colors;
	}

}
