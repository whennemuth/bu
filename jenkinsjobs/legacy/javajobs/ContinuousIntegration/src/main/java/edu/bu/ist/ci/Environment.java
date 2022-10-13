package edu.bu.ist.ci;

import java.util.Map;

public interface Environment {

	Map<String, String> getVariables();

	String getVariable(String name);

}