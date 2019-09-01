package analysis;

import java.io.File;

import analysis.flow.CodeNode;
import trace.SparceMatrix;

public abstract class Importer {
	public void load(File file) throws Exception {
		if(file.isFile() && file.getPath().endsWith(".java"))
			importFile(file);
		else if(file.isDirectory()) 
		    for (File subfile : file.listFiles()) 
		        load(subfile);
	}
	protected abstract void importFile(File file) throws Exception;
	public abstract SparceMatrix createCallGraph() throws Exception;
	public abstract Object getMethod(int id);
}
