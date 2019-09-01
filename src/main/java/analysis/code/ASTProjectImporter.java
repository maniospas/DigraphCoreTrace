package analysis.code;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import trace.SparceMatrix;

/**
 * <h1>ASTProject</h1>
 * This class is used to manage projects using {@link ASTEntity}.
 * It contains functionality which allows scanning {@link ASTEntity#getImplementation()} for usage of {@link ASTEntity} instances
 * loaded. Code needs to be bug-free but files are allowed to missing.
 * @author Manios Kranasakis
 */
public class ASTProjectImporter extends analysis.Importer{
	private HashMap<String, ASTEntity> projectClasses = new HashMap<String, ASTEntity>();
	private ArrayList<ASTEntity> allMethods = new ArrayList<ASTEntity>();
	private HashMap<ASTEntity, Integer> allMethodsIds = new  HashMap<ASTEntity, Integer>();
	
	public ASTProjectImporter() {
	}
	/**
	 * <h1>updateProject</h1>
	 * Updates the given project metadata, such as available methdods, after adding new classes.
	 * @see #addClassObject(ClassObject)
	 * @see #importFile(String)
	 * @see #importPath(String)
	 */
	public void updateProject() {
		allMethodsIds.clear();
		allMethods.clear();
		for(ASTEntity projectClass : projectClasses.values()) {
			for(Node method : projectClass.collapse()) 
			if(((ASTEntity)method).isMethod()){
				allMethodsIds.put((ASTEntity)method, allMethods.size());
				allMethods.add((ASTEntity)method);
			}
		}
	}
	public SparceMatrix createCallGraph() throws Exception {
		updateProject();
		int N = allMethods.size(); 
		SparceMatrix M = new SparceMatrix(N);
		for(int i=0;i<N;i++) {
			M.set(i, i, 1);
			for(ASTEntity entity : getCalledMethodsBy(allMethods.get(i)))
				if(entity.isMethod() && allMethods.contains(entity)) {
					M.set(i, allMethodsIds.get(entity), 1);
				}
		}
		return M;
	}
	public Object getMethod(int i) {
		return allMethods.get(i);
	}
	/**
	 * <h1>getCalledMethodsBy</h1>
	 * Identifies class fields as variables before calling {@link #getStatementCalls} for each statement in the method's
	 * implementation.
	 * @param method a given method
	 * @return a list of {@link ASTEntity} instances in the project used by the designated method
	 */
	public ArrayList<ASTEntity> getCalledMethodsBy(ASTEntity method) {
		HashMap<String, ASTEntity> variableClasses = new HashMap<String, ASTEntity>();
		
		//identify variable declaration statements
		for(String classDeclarationStatement : CodeManipulation.getTopLevelStatements(((ASTEntity)method.getParent()).getImplementation(), ((ASTEntity)method.getParent()).getImplementation().indexOf('{')+1)) {
			if(classDeclarationStatement.indexOf("=")!=-1)
				classDeclarationStatement = classDeclarationStatement.substring(0, classDeclarationStatement.indexOf("="));
			if(!classDeclarationStatement.contains("(")) {
				String[] variableType = classDeclarationStatement.split("(\\s|\\<|\\>|\\,)+");
				if(variableType.length>=2) {
					String variableName = variableType[variableType.length-1];
					//handle hashmaps, lists, etc by selecting the first known variable reference
					for(int i=0;i<variableType.length-1;i++) {
						if(projectClasses.get(variableType[i])!=null) {
							variableClasses.put(variableName, projectClasses.get(variableType[i]));
							break;
						}
					}
				}
			}
		}

		ArrayList<ASTEntity> ret = new ArrayList<ASTEntity>();
		for(String statement : CodeManipulation.splitToStatements(method.getImplementation().trim(), 1))
			for(ASTEntity statementCall : getStatementCalls(statement, variableClasses, (ASTEntity)method.getParent(), (ASTEntity)method.getParent()))
				if(!ret.contains(statementCall))
					ret.add(statementCall);
		return ret;
	}
	/**
	 * <h1>getStatementCalls</h1>
	 * Identifies variable types, which are then added to the given map of variable classes. 
	 * Calls {@link #recognizeKnownEntity} to identify method calls, as well as method and operator arguments.
	 * @param statement the given statement
	 * @param variableClasses a map that maps variables to their closest {@link ASTEntity} defined in the project
	 * @param parentEntity
	 * @param defaultParentEntity
	 * @return a list of identified {@link ASTEntity} instances from the project used by the given statement
	 * @see #recognizeKnownEntity(String, ASTEntity, HashMap, ASTEntity)
	 */
	private ArrayList<ASTEntity> getStatementCalls(String statement, HashMap<String, ASTEntity> variableClasses, ASTEntity parentEntity, ASTEntity defaultParentEntity) {
		statement = statement.trim();
		ArrayList<ASTEntity> calls = new ArrayList<ASTEntity>();
		//System.out.println("--->"+statement);
		
		int pos = 0;
		while(pos<statement.length()) {
			int idxEquals = CodeManipulation.topLevelIndexOf(statement, '=', pos);
			if(idxEquals==-1) {
				idxEquals = CodeManipulation.topLevelIndexOf(statement, ':', pos);
				int posQ = CodeManipulation.topLevelIndexOf(statement, '?', pos);
				if(posQ!=-1 && posQ<idxEquals)
					idxEquals = -1;
			}
			if(idxEquals>0 && statement.charAt(idxEquals-1)=='!')//ommit != statements
				idxEquals = -1;
			if(idxEquals<statement.length()-1 && idxEquals<statement.length()-1 && statement.charAt(idxEquals+1)=='=')
				idxEquals = -1;
			if(idxEquals!=-1) {
				String LHStext = statement.substring(0, idxEquals).trim();
				ArrayList<ASTEntity> RHSCalls = getStatementCalls(statement.substring(idxEquals+1), variableClasses, parentEntity, defaultParentEntity);
				calls.addAll(getStatementCalls(LHStext, variableClasses, parentEntity, defaultParentEntity));
				calls.addAll(RHSCalls);
				String variableName = LHStext.substring(LHStext.lastIndexOf(' ')+1);
				if(RHSCalls.size()>=1)  {
					String variableType = RHSCalls.get(RHSCalls.size()-1).getType();
					if(projectClasses.get(variableType)!=null)
						variableClasses.put(variableName, projectClasses.get(variableType));
				}
				if(variableClasses.get(variableName)==null) {
					String[] variableType = LHStext.split("(\\s|\\<|\\>|\\,)+");
					if(variableType.length>=2) {
						variableName = variableType[variableType.length-1];
						//handle hashmaps, lists, etc by selecting the first known variable reference
						for(int i=0;i<variableType.length-1;i++) {
							if(projectClasses.get(variableType[i])!=null) {
								variableClasses.put(variableName, projectClasses.get(variableType[i]));
								break;
							}
						}
					}	
				}
				pos = idxEquals+1;
			}
			else {
				String entityText = statement.substring(pos);
				ArrayList<ASTEntity> found = recognizeKnownEntity(entityText, parentEntity, variableClasses, defaultParentEntity);
				if(!found.isEmpty())
					parentEntity = found.get(found.size()-1);
				calls.addAll(found);
				break;
			}
		}
		
		return calls;
	}
	/**
	 * <h1>recognizeKnownEntity</h1>
	 * Identified method calls, as well as method and operator arguments. In turn calls {@link #getStatementCalls} to handle sub-structures,
	 * such as arguments.
	 * @param callText
	 * @param parentEntity
	 * @param variableClasses
	 * @param defaultParentEntity
	 * @return a list of identified {@link ASTEntity} instances from the project used by the given statement
	 * @see #getStatementCalls(String, HashMap, ASTEntity, ASTEntity)
	 */
	synchronized private ArrayList<ASTEntity> recognizeKnownEntity(String callText, ASTEntity parentEntity,  HashMap<String, ASTEntity> variableClasses, ASTEntity defaultParentEntity) {
		ArrayList<ASTEntity> ret = new ArrayList<ASTEntity>();
		callText = callText.trim();
		//while(callText.startsWith("(") && callText.endsWith(")")) 
			//callText = callText.substring(1, callText.length()-1).trim();
		if(!callText.contains(")")) {//static class references
			if(projectClasses.get(callText)!=null)
				ret.add(projectClasses.get(callText));
		}
		else if(callText.startsWith("new ")) {//constructors
			int idx = callText.indexOf("(");
			int nArgs = CodeManipulation.topLevelCountOf(callText, ',', idx+1)+1;
			int last = CodeManipulation.topLevelIndexOf(callText,')', idx+1);
			try {
				if(idx<0 || last>=callText.length()-1 || idx+1>=callText.length()-1)
					nArgs = 0;
				else if(callText.substring(idx+1, last).trim().isEmpty())
					nArgs = 0;
			}
			catch(Exception e) {
				nArgs = 0;
			}
			String className = callText.substring(4, idx<0?callText.length():idx).trim();
			parentEntity = projectClasses.get(className);
			ASTEntity foundConstructor = null;
			if(parentEntity!=null)
				for(Node entity : parentEntity.getChildren())
					if(entity.getName().equals(className) && entity.getChildren().size()==nArgs)
						foundConstructor = (ASTEntity)entity;
			if(foundConstructor!=null)
				ret.add(foundConstructor);
		}
		else {
			int idx = CodeManipulation.topLevelIndexOf(callText, '(', 0);
			if(idx==-1)
				idx = callText.length()-1;
			int idxEnd = CodeManipulation.topLevelIndexOf(callText, ')', idx);
			int pos;
			if((pos = CodeManipulation.topLevelIndexOf(callText, "->", 0))!=-1) {
				if(pos<callText.length()-1) {
					ret.addAll(getStatementCalls(callText.substring(0, pos), variableClasses, defaultParentEntity, defaultParentEntity));
					ret.addAll(getStatementCalls(callText.substring(pos+2), variableClasses, defaultParentEntity, defaultParentEntity));
				}
			}
			else if((pos = CodeManipulation.topLevelIndexOf(callText, ':', 0))!=-1) {
				if(pos<callText.length()-1) {
					ret.addAll(getStatementCalls(callText.substring(0, pos), variableClasses, defaultParentEntity, defaultParentEntity));
					ret.addAll(getStatementCalls(callText.substring(pos+1), variableClasses, defaultParentEntity, defaultParentEntity));
				}
			}
			else if((pos = CodeManipulation.topLevelIndexOf(callText, '?', 0))!=-1) {
				if(pos<callText.length()-1) {
					ret.addAll(getStatementCalls(callText.substring(0, pos), variableClasses, defaultParentEntity, defaultParentEntity));
					ret.addAll(getStatementCalls(callText.substring(pos+1), variableClasses, defaultParentEntity, defaultParentEntity));
				}
			}
			else if((pos = CodeManipulation.topLevelIndexOf(callText, "==", 0))!=-1) {
				if(pos<callText.length()-1) {
					ret.addAll(getStatementCalls(callText.substring(0, pos), variableClasses, defaultParentEntity, defaultParentEntity));
					ret.addAll(getStatementCalls(callText.substring(pos+2), variableClasses, defaultParentEntity, defaultParentEntity));
				}
			}
			else if((pos = CodeManipulation.topLevelIndexOf(callText, '<', 0))!=-1) {
				if(pos<callText.length()-1) {
					ret.addAll(getStatementCalls(callText.substring(0, pos), variableClasses, defaultParentEntity, defaultParentEntity));
					ret.addAll(getStatementCalls(callText.substring(pos+1), variableClasses, defaultParentEntity, defaultParentEntity));
				}
			}
			else if((pos = CodeManipulation.topLevelIndexOf(callText, '>', 0))!=-1) {
				if(pos<callText.length()-1) {
					ret.addAll(getStatementCalls(callText.substring(0, pos), variableClasses, defaultParentEntity, defaultParentEntity));
					ret.addAll(getStatementCalls(callText.substring(pos+1), variableClasses, defaultParentEntity, defaultParentEntity));
				}
			}
			else if((pos = CodeManipulation.topLevelIndexOf(callText, "&&", 0))!=-1) {
				if(pos<callText.length()-1) {
					ret.addAll(getStatementCalls(callText.substring(0, pos), variableClasses, defaultParentEntity, defaultParentEntity));
					ret.addAll(getStatementCalls(callText.substring(pos+2), variableClasses, defaultParentEntity, defaultParentEntity));
				}
			}
			else if((pos = CodeManipulation.topLevelIndexOf(callText, "||", 0))!=-1) {
				if(pos<callText.length()-1) {
					ret.addAll(getStatementCalls(callText.substring(0, pos), variableClasses, defaultParentEntity, defaultParentEntity));
					ret.addAll(getStatementCalls(callText.substring(pos+2), variableClasses, defaultParentEntity, defaultParentEntity));
				}
			}
			else if((pos = CodeManipulation.topLevelIndexOf(callText, "!=", 0))!=-1) {
				if(pos<callText.length()-1) {
					ret.addAll(getStatementCalls(callText.substring(0, pos), variableClasses, defaultParentEntity, defaultParentEntity));
					ret.addAll(getStatementCalls(callText.substring(pos+2), variableClasses, defaultParentEntity, defaultParentEntity));
				}
			}
			else if((pos = CodeManipulation.topLevelIndexOf(callText, '+', 0))!=-1) {
				if(pos<callText.length()-1) {
					ret.addAll(getStatementCalls(callText.substring(0, pos), variableClasses, defaultParentEntity, defaultParentEntity));
					ret.addAll(getStatementCalls(callText.substring(pos+1), variableClasses, defaultParentEntity, defaultParentEntity));
				}
			}
			else if((pos = CodeManipulation.topLevelIndexOf(callText, '-', 0))!=-1) {
				if(pos<callText.length()-1) {
					ret.addAll(getStatementCalls(callText.substring(0, pos), variableClasses, defaultParentEntity, defaultParentEntity));
					ret.addAll(getStatementCalls(callText.substring(pos+1), variableClasses, defaultParentEntity, defaultParentEntity));
				}
			}
			else if((pos = CodeManipulation.topLevelIndexOf(callText, '*', 0))!=-1) {
				if(pos<callText.length()-1) {
					ret.addAll(getStatementCalls(callText.substring(0, pos), variableClasses, defaultParentEntity, defaultParentEntity));
					ret.addAll(getStatementCalls(callText.substring(pos+1), variableClasses, defaultParentEntity, defaultParentEntity));
				}
			}
			else if((pos = CodeManipulation.topLevelIndexOf(callText, '/', 0))!=-1) {
				if(pos<callText.length()-1) {
					ret.addAll(getStatementCalls(callText.substring(0, pos), variableClasses, defaultParentEntity, defaultParentEntity));
					ret.addAll(getStatementCalls(callText.substring(pos+1), variableClasses, defaultParentEntity, defaultParentEntity));
				}
			}
			else if((pos = CodeManipulation.topLevelIndexOf(callText, '!', 0))!=-1) {
				if(pos<callText.length()-1) {
					ret.addAll(getStatementCalls(callText.substring(0, pos), variableClasses, defaultParentEntity, defaultParentEntity));
					ret.addAll(getStatementCalls(callText.substring(pos+1), variableClasses, defaultParentEntity, defaultParentEntity));
				}
			}
			else if(idxEnd!=callText.length()-1 && CodeManipulation.topLevelIndexOf(callText, ' ', 0)!=-1) {
				ret.addAll(recognizeKnownEntity(callText.substring(0, idx), defaultParentEntity, variableClasses, defaultParentEntity));
				if(idxEnd!=-1) {
					ret.addAll(getStatementCalls(callText.substring(idx+1, idxEnd), variableClasses, defaultParentEntity, defaultParentEntity));
					ret.addAll(recognizeKnownEntity(callText.substring(idxEnd+1), defaultParentEntity, variableClasses, defaultParentEntity));
				}
			}
			else if((pos = CodeManipulation.topLevelIndexOf(callText, '.', 0))!=-1) {
				String entityText = callText.substring(0, pos).trim();
				if(variableClasses.get(entityText)!=null) {
					parentEntity = variableClasses.get(entityText);
					ret.add(parentEntity);
					//System.out.println("Variable : "+entityText+" -> "+parentEntity.getStackTrace());
				}
				else {
					ArrayList<ASTEntity> found = recognizeKnownEntity(entityText, parentEntity, variableClasses, defaultParentEntity);
					if(!found.isEmpty() && found.get(found.size()-1).isClass())
						parentEntity = found.get(found.size()-1);
					else if(!found.isEmpty() && projectClasses.get(found.get(found.size()-1).getType())!=null) {
						parentEntity = projectClasses.get(found.get(found.size()-1).getType());
						//System.out.println("Return : "+parentEntity.getStackTrace());
					}
					//else
						//System.out.println("Return : unchanged for "+entityText);
					ret.addAll(found);
				}
				ret.addAll(recognizeKnownEntity(callText.substring(pos+1), parentEntity, variableClasses, defaultParentEntity));
			}
			else {
				int nArgs = CodeManipulation.topLevelCountOf(callText, ',', idx+1)+1;
				if(idxEnd==-1 || idx>=callText.length()-1 || callText.substring(idx+1, idxEnd).trim().isEmpty())
					nArgs = 0;
				String methodName = callText.substring(0, idx).trim();
				ASTEntity foundMethod = null;
				if(parentEntity!=null)
					for(Node entity : parentEntity.getChildren())
						if(entity.getName().equals(methodName) && entity.getChildren().size()==nArgs)
							foundMethod = (ASTEntity)entity;
				for(int i=0;i<nArgs;i++) {
					pos = CodeManipulation.topLevelIndexOf(callText, ',', idx+1);
					if(pos==-1)
						pos = idxEnd;
					ret.addAll(getStatementCalls(callText.substring(idx+1, pos), variableClasses, defaultParentEntity, defaultParentEntity));
					idx = pos;
				}
				if(foundMethod!=null)
					ret.add(foundMethod);
				else
					ret.add(parentEntity);
			}
		}
		return ret;
	}
	@Override
	protected void importFile(File file) throws Exception {
		ClassObject obj = new ClassObject(file.getPath());
		addClassObject(obj);
	}
	/**
	 * <h1>addClassObject</h1>
	 * Adds a {@link ClassObject} to the project. {@link #updateProject} must be called afterwards.
	 * @param classObject
	 * @see #importFile(String)
	 * @see #updateProject()
	 */
	public void addClassObject(ClassObject classObject) {
		Node root = classObject.getRoot();
		if(root!=null)
			for(Node entity : root.collapse())
				if(((ASTEntity)entity).isClass())
					projectClasses.put(entity.getStackTrace(), (ASTEntity)entity);
	}
}