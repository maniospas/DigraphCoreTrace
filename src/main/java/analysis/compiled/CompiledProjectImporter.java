package analysis.compiled;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.bcel.classfile.ClassParser;

public class CompiledProjectImporter {
	private ArrayList<String> visited = new ArrayList<String>();
	public boolean singleTimeVisit(String id) {
		if(visited.contains(id))
			return false;
		visited.add(id);
		return true;
	}
	
	public CompiledProjectImporter() {
	}
	
	public void compileAndImport(String path) {
	}
	
	public void importPath(String path) {
	    File directory = new File(path);
	    File[] fList = directory.listFiles();
	    for (File file : fList) {
	        if (file.isFile()) {
	        	if(file.getPath().endsWith(".class"))
	        		importFile(file.getPath());
	        } 
	        else if (file.isDirectory()) {
	        	importPath(file.getPath());
	        }
	    }
	}
	public void importFile(String path) {
		try {
			ClassParser cp = new ClassParser(path);
	        ClassVisitor visitor = new ClassVisitor(cp.parse(), this);
	        visitor.start();
		}
		catch(Exception e) {
			System.err.println(path+": "+e.toString());
			//e.printStackTrace();
		}
	}
	
	private HashMap<String, Integer> methods = new HashMap<String, Integer>();
	private HashMap<Integer, String> methodIndexes = new HashMap<Integer, String>();
	private ArrayList<Entry<Integer, Integer>> calls = new ArrayList<Entry<Integer, Integer>>();
	
	public int getMethodIndex(String name) {
		Integer ret = methods.get(name);
		if(ret==null) {
			methods.put(name, ret = methods.size());
			methodIndexes.put(ret, name);
		}
		return (int)ret;
	}
	public String getMethodName(int index) {
		return methodIndexes.get(index);
	}
	
	public double[][] getCallMatrix() {
		int n = methods.size();
		double[][] A = new double[n][n];
		for(Entry<Integer, Integer> entry : calls)
			A[entry.getKey()][entry.getValue()] = 1;
		return A;
	}

	public void registerVisit(String from, String to) {
		int f = getMethodIndex(from);
		int t = getMethodIndex(to);
		calls.add(new java.util.AbstractMap.SimpleEntry<Integer, Integer>(f, t));
	}
}
