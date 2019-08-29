package FEC.common;
//======================================================== define FEC type =====

import java.io.Serializable;

// FEC 종류
public enum TypeFEC implements Serializable{
	//============ fountain code with conventional source symbol selection =====
//	fountainC, 
	//================ fountain code with proposed source symbol selection =====
	fountainP,  // LT 코드
	//============================================ Reed-Solomon (223, 255) =====
	RS223
}
