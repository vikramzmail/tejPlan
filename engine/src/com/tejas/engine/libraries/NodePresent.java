package com.tejas.engine.libraries;

public class NodePresent {
	private String name;
	private String type;
	private int sfp1, sfp2, xfp1, xfp2;

	public NodePresent(String name, String type) {
		this.name = name;
		this.type = type;
		this.sfp1 = 0;
		this.sfp2 = 0;
		this.xfp1 = 0;
		this.xfp2 = 0;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public void modifysfp1(int a) {
		this.sfp1 = this.sfp1 + a;
	}

	public void modifysfp2(int b) {
		this.sfp2 = this.sfp2 + b;
	}

	public void modifyxfp1(int c) {
		this.xfp1 = this.xfp1 + c;
	}

	public void modifyxfp2(int d) {
		this.xfp2 = this.xfp2 + d;
	}

	public int getsfp1() {
		return this.sfp1;
	}

	public int getsfp2() {
		return this.sfp2;
	}

	public int getxfp1() {
		return this.xfp1;
	}

	public int getxfp2() {
		return this.xfp2;
	}
}
