package analysis.flow;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import trace.SparceMatrix;

public class Importer extends analysis.Importer {
	private HashMap<CodeNode, ArrayList<String>> unresolvedCalls = new HashMap<CodeNode, ArrayList<String>>();
	private HashMap<CodeNode, ArrayList<String>> unresolvedDeclarations = new HashMap<CodeNode, ArrayList<String>>();
	private HashMap<CodeNode, MethodDeclaration> unresolvedMethodDeclarations = new HashMap<CodeNode, MethodDeclaration>();
	private ArrayList<String> imports = new ArrayList<String>();
	private ArrayList<CodeNode> datatypes = new ArrayList<CodeNode>();
	private HashMap<CodeNode, Integer> methods = new HashMap<CodeNode, Integer>();
	private HashMap<CodeNode, ArrayList<CodeNode>> resolvedCalls = new HashMap<CodeNode, ArrayList<CodeNode>>();
	
	private VoidVisitorAdapter<CodeNode> methodVisitor = new VoidVisitorAdapter<CodeNode>() {
        @Override
        public void visit(ClassOrInterfaceDeclaration n, CodeNode arg) {
            CodeNode classNode = new CodeNode(n.getNameAsString(), null);
            if(arg!=null)
            	arg.add(classNode);
            datatypes.add(classNode);
            super.visit(n, classNode);
            for(ClassOrInterfaceType type : n.getExtendedTypes())
				try {
					classNode.addParameter(lookupAndAddClass(type.getNameAsString()));
				} catch (Exception e) {
					e.printStackTrace();
				}
        }
        @Override
        public void visit(ConstructorDeclaration n, CodeNode arg) {
        	CodeNode constructorNode = new CodeNode(n.getNameAsString(), arg);
        	arg.add(constructorNode);
            super.visit(n, constructorNode);
        }
        @Override
        public void visit(MethodDeclaration n, CodeNode arg) {
        	if(!arg.isClass())
        		arg = arg.getParent();
        	CodeNode methodNode = new CodeNode(n.getNameAsString(), CodeNode.UNRESOLVED);
        	unresolvedMethodDeclarations.put(methodNode, n);
        	arg.add(methodNode);
            super.visit(n, methodNode);
        }
        @Override
        public void visit(FieldDeclaration n, CodeNode arg) {
        	if(!unresolvedDeclarations.containsKey(arg))
        		unresolvedDeclarations.put(arg, new ArrayList<String>());
        	unresolvedDeclarations.get(arg).add(n.toString());
            //System.out.println(arg.toString()+" : "+n.toString());
            super.visit(n, arg);
        }
        @Override
        public void visit(VariableDeclarationExpr n, CodeNode arg) {
        	if(!unresolvedDeclarations.containsKey(arg))
        		unresolvedDeclarations.put(arg, new ArrayList<String>());
        	unresolvedDeclarations.get(arg).add(n.toString());
            //System.out.println(arg.toString()+" : "+n.toString());
            super.visit(n, arg);
        }
        @Override
        public void visit(MethodCallExpr n, CodeNode arg) {
            if(!unresolvedCalls.containsKey(arg))
            	unresolvedCalls.put(arg, new ArrayList<String>());
            unresolvedCalls.get(arg).add(n.toString());
            //System.out.println(arg.toString()+" : "+n.toString());
            super.visit(n, arg);
        }
	};
	
	protected void importFile(File file) throws Exception {
		CompilationUnit compilationUnit = JavaParser.parse(file);
		//add imports
		for(ImportDeclaration imp : compilationUnit.findAll(ImportDeclaration.class)) 
			if(!imports.contains(imp.getNameAsString()))
				imports.add(imp.getNameAsString());
		//find package declaration
		CodeNode packageNode = compilationUnit.findAll(PackageDeclaration.class).size()!=0?new CodeNode(compilationUnit.findAll(PackageDeclaration.class).get(0).getNameAsString(),null):null;
		//traverse methods
		compilationUnit.accept(methodVisitor, packageNode);
	}
	
	protected CodeNode lookupAndAddClass(String query) throws Exception {
		//name should belong either to a package class or to an imported class and therefore it needs not be inferred in other ways (e.g. through function calls)
		for(CodeNode datatype : datatypes) {
			String name = datatype.getName();
			if(query.equals(name))
				return datatype;
		}
		String[] strSplitQuery = query.split("\\.");
		for(CodeNode datatype : datatypes) {
			String name = datatype.getName();
			String[] strSplit = name.split("\\.");
			if(strSplit[strSplit.length-1].equals(strSplitQuery[strSplitQuery.length-1]))
				return datatype;
		}
		/*
		//search in imports
		for(String name : imports) {
			String[] strSplit = name.split("\\.");
			if(strSplit[strSplit.length-1].equals(query)) {
				CodeNode datatype = new CodeNode(name, null);
				datatypes.add(datatype);
				return datatype;
			}
		}*/
		//otherwise, assume that it's a native type (this case also handles classes that have not been imported)
		CodeNode datatype = new CodeNode(strSplitQuery[strSplitQuery.length-1], null);
		datatypes.add(datatype);
		return datatype;
	}
	
	protected void resolveAll() throws Exception {
		for(CodeNode node : unresolvedMethodDeclarations.keySet()) {
			node.changeType(lookupAndAddClass(unresolvedMethodDeclarations.get(node).getTypeAsString()));
			for(Parameter param : unresolvedMethodDeclarations.get(node).getParameters())
				node.addParameter(new VariableNode(param.getNameAsString(), lookupAndAddClass(param.getTypeAsString())));
		}
		for(CodeNode node : unresolvedDeclarations.keySet())
			for(String declaration : unresolvedDeclarations.get(node)) {
				if(declaration.endsWith(";"))
					declaration = declaration.substring(0, declaration.length()-1);
				int pos = declaration.indexOf('=');
				if(pos==-1)
					pos = declaration.length();
				declaration = declaration.substring(0, pos).trim().replace("public","").replace("private", "").replace("protected", "").replace("final", "").replace("static", "");
				pos = declaration.lastIndexOf(' ');
				CodeNode declarationNode = new VariableNode(declaration.substring(pos).trim(), lookupAndAddClass(declaration.substring(0, pos).trim()));
				node.add(declarationNode);
				if(declaration.contains("trace"))
					System.out.println(node.toString()+" "+declaration+" "+declarationNode.toString()+" "+declarationNode.getType().toString());
			}
		
		for(CodeNode node : unresolvedCalls.keySet()) {
			Collections.sort(unresolvedCalls.get(node), Comparator.comparing(String::length));//start from shorter calls, because they may be required for longer calls
			HashMap<String, CodeNode> expressionTypes = new HashMap<String, CodeNode>();
			CodeNode par = node;
			for(CodeNode child : par.getParameters())
				expressionTypes.put(child.getName(), child.getType());
			while(par!=null) {
				for(CodeNode child : par.getChildren())
					if(child.isVariable()) {
						expressionTypes.put(child.getName(), child.getType());
					}
				par = par.getParent();
			}
			for(CodeNode datatype : datatypes) {
				String[] split = datatype.getTrace().split("\\.");
				expressionTypes.put(split[split.length-1], datatype);
				expressionTypes.put(datatype.getTrace(), datatype);
			}
				
			for(String call : unresolvedCalls.get(node)) {
				int pos = call.length()-1;
				int posPar = call.length()-1;
				int countCommas = 0;
				int depth = 0;
				boolean hasArgs = false;
				boolean text = false;
				while(pos>0) {
					char c = call.charAt(pos);
					if(c=='\"' || c=='\'')
						text = !text;
					if(!text) {
						if(c==')' || c=='>' || c==']')
							depth++;
						if(c=='(' || c=='<' || c=='[')
							depth--;
						if(c=='(' && depth==0)
							posPar = pos;
						if(c==',' && depth==1)
							countCommas++;
						if(c=='.' && depth==0)
							break;
					}
					if(c!=' ' && c!='\t' && c!='\n' && c!=')' && depth!=0) 
						hasArgs = true;
					pos--;
				}
				int numArgs = hasArgs?countCommas + 1:0;
				String typePart = call.substring(0, pos);
				String methodPart = call.substring(pos==0?0:pos+1, posPar);
				CodeNode discoveredType = null;
				CodeNode discoveredMethod = null;
				if(typePart.isEmpty() || typePart.equals("this"))
					discoveredType = node.getParent();

				if(typePart.equals("super") && !node.getParent().getParameters().isEmpty())
					discoveredType = node.getParent().getParameters().get(0);
				if(discoveredType==null)
					discoveredType = expressionTypes.get(typePart);
				if(typePart.contains("trace") && discoveredType!=null)
					System.out.println("F0OOOOOOOOOOOOOOOOOOOOOOOOO "+typePart+" "+discoveredType.getName()+" "+discoveredType.toString());
				if(discoveredType==null) {
					discoveredType = lookupAndAddClass(typePart);
				}
				if(discoveredType!=null)
					discoveredMethod = searchAndRegisterMethod(discoveredType, methodPart, numArgs);
						
				if(discoveredMethod==null) {
					System.out.println("FAILED TO LINK CALL  "+call+"  @  "+node.toString());
					continue;
				}
				//System.out.println("DISCOVERED  "+discoveredMethod.toString()+"  @  "+node.toString());
				expressionTypes.put(call, discoveredType);
				if(!resolvedCalls.containsKey(node))
					resolvedCalls.put(node, new ArrayList<CodeNode>());
				resolvedCalls.get(node).add(discoveredMethod);
			}
		}
		
		unresolvedCalls.clear();
		unresolvedDeclarations.clear();
		unresolvedMethodDeclarations.clear();
		
		methods.clear();
		for(CodeNode node : datatypes)
			for(CodeNode method : node.pack())
				if(method.isMethod() && !methods.containsKey(method)) {
					//System.out.println(method.getParent().getName()+" "+ methods.size());
					//System.out.println(method.toString()+" "+ methods.size());
					methods.put(method, methods.size());
				}
		for(CodeNode method : resolvedCalls.keySet())
			if(method.isMethod() && !methods.containsKey(method)) {
				//System.out.println(method.getParent().getName()+" "+ methods.size());
				//System.out.println(method.toString()+" "+ methods.size());
				methods.put(method, methods.size());
			}
		
	}
	
	private CodeNode searchAndRegisterMethod(CodeNode type, String methodName, int args) {
		if(methodName.startsWith("new ")) 
			methodName = methodName.substring(4).trim();
		for(CodeNode method : type.getChildren())
			if(method.getName().equals(methodName) && method.getParameters().size()==args)
				return method;
		CodeNode method = new CodeNode(methodName, CodeNode.UNRESOLVED);
		for(int i=0;i<args;i++)
			method.addParameter(new VariableNode("var"+i, CodeNode.UNRESOLVED));
		type.add(method);
		return method;
	}

	public SparceMatrix createCallGraph() throws Exception {
		resolveAll();
		SparceMatrix matrix = new SparceMatrix(methods.size());
		for(CodeNode method : methods.keySet())
			if(resolvedCalls.containsKey(method))
				for(CodeNode method2 : resolvedCalls.get(method))
					matrix.set(methods.get(method), methods.get(method2), 1);
		return matrix;
	}
	
	public CodeNode getMethod(int id) {
		for(CodeNode method : methods.keySet())
			if(methods.get(method)==id)
				return method;
		return null;
	}
}
