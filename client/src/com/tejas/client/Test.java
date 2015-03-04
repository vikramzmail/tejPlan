package com.tejas.client;

public class Test {
	public static void main(String[] args){
			String a = "A^&*C^&*2000";
			String[] b = a.split("\\^\\&\\*");
			for(int i =0;i < b.length;i++){
				System.out.println("b of "+i+"is :"+b[i]);
			}
	}
}
