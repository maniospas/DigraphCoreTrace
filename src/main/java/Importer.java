import analysis.code.ASTProject;
import analysis.compiled.CompiledProjectImporter;

public class Importer {
	private ASTProject project;
	private CompiledProjectImporter compiledProject;
	public Importer(String path, boolean importSources, boolean importBinary) {
		importProject(path, importSources, importBinary);
	}
	public void importProject(String path, boolean importSources, boolean importBinary) {
		compiledProject = null;
		if(importSources)
			project = new ASTProject(path);
		if(importBinary && (project==null || project.getMethods().isEmpty())) {
			compiledProject = new CompiledProjectImporter();
			compiledProject.importPath(path); 
			project = null;
		}
	}
	public double[][] getCallMatrix() {
		if(project!=null)
			return project.generateTraversalMatrix();
		else if(compiledProject!=null)
			return compiledProject.getCallMatrix();
		else
			return null;
	}
	public String getMethodName(int id) {
		if(project!=null)
			return project.getMethodByIndexInTraversalMatrix(id).getStackTrace();
		else if(compiledProject!=null)
			return compiledProject.getMethodName(id);
		else
			return "";
	}
	public String getMethodDetails(int id) {
		if(project!=null) {
			String desc = project.getMethodByIndexInTraversalMatrix(id).getImplementation();
			StringBuilder ret = new StringBuilder();
			int nested = 0;
			String intend = "";
			boolean inComment = false;
			for(char c : desc.toCharArray()) {
				if(c=='"' || c=='\'')
					inComment = !inComment;
				if(inComment) {
					ret.append(c);
					continue;
				}
				if(c=='(')
					nested++;
				if(c==')')
					nested--;
				ret.append(c);
				if(c=='{')
					intend += "    ";
				if(c=='}' && !intend.isEmpty())
					intend = intend.substring(4);
				if(c=='\n')
					ret.append(intend);
				if(c==';' && nested==0)
					ret.append("\n").append(intend);
				if(c=='{' && nested==0)
					ret.append("\n").append(intend);
				if(c=='}' && nested==0)
					ret.append("\n").append(intend);
			}
			return ret.toString().replace("    }", "}");
		}
		else {
			return "";
		}
	}
}
