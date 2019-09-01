package analysis.compiled;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.bcel.classfile.ClassParser;

import trace.SparceMatrix;

public class CompiledProjectImporter extends analysis.Importer{
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
	
	public void importFile(File file) throws Exception {
		ClassParser cp = new ClassParser(file.getPath());
        ClassVisitor visitor = new ClassVisitor(cp.parse(), this);
        visitor.start();
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
	public String getMethod(int index) {
		return methodIndexes.get(index);
	}
	
	public SparceMatrix createCallGraph() {
		int n = methods.size();
		SparceMatrix A = new SparceMatrix(n);
		for(Entry<Integer, Integer> entry : calls)
			A.set(entry.getKey(), entry.getValue(), 1);
		return A;
	}

	public void registerVisit(String from, String to) {
		int f = getMethodIndex(from);
		int t = getMethodIndex(to);
		calls.add(new java.util.AbstractMap.SimpleEntry<Integer, Integer>(f, t));
	}
}
