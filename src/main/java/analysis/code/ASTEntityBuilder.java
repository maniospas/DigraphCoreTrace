package analysis.code;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;

/**
 * <h1>ASTEntityBuilder</h1>
 * This class is used to generate and manage the comments a tree of {@link ASTEntity} nodes related to the source code
 * of a {@link ClassObject}. Changes to node comments are able to be passed back as changes to the source code.
 * 
 * @author Manios Krasanakis
 */
public class ASTEntityBuilder {//don't serialize AST information
	
	private ASTEntity rootEntity = null;
	private ClassObject targetObject;
	private ASTNode node;
	private HashMap<ASTEntity, ASTNode> entityNodes = new HashMap<ASTEntity, ASTNode>();
	
	public ASTEntityBuilder(ClassObject object) {
		targetObject = object;
	}
	
	/**
	 * <h1>getNodeChildren</h1>
	 * @param node a given Java ASTNode node
	 * @return a list of the given Java ASTNode's children
	 */
	@SuppressWarnings("unchecked")
	private static ArrayList<ASTNode> getNodeChildren(ASTNode node) {
		ArrayList<ASTNode> flist = new ArrayList<ASTNode>();
		List<Object> list = node.structuralPropertiesForType();
		for (int i = 0; i < list.size(); i++) {
			StructuralPropertyDescriptor curr = (StructuralPropertyDescriptor) list.get(i);
			Object child = node.getStructuralProperty(curr);
			if (child instanceof List) {
				flist.addAll((Collection<? extends ASTNode>) child);
			} else if (child instanceof ASTNode) {
				flist.add((ASTNode) child);
			} else {
			}
		}
		return flist;
	}
	
	/**
	 * <h1>generateAST</h1>
	 * Generates the Abstract Syntax Tree of the raw textual content.
	 */
	protected void generateAST() {
		// GENERATE AST
		entityNodes.clear();
		ASTParser parser = ASTParser.newParser(AST.JLS9);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setResolveBindings(false); 
		parser.setSource(targetObject.getContent().toCharArray());
		node = (ASTNode) parser.createAST(null);
		ASTEntity documentRootEntity = new ASTEntity("","","","",-1);
		createChildNodes(documentRootEntity, node);
		if(documentRootEntity.getChildren().size()==0)
			throw new RuntimeException("No top-level class declarations in single document");
		if(documentRootEntity.getChildren().size()>1)
			throw new RuntimeException("Multiple top-level class declarations in single document");
		rootEntity = (ASTEntity) documentRootEntity.getChildren().get(0);
		documentRootEntity.removeChild(rootEntity);
	}
	
	protected void createChildNodes(ASTEntity parent, ASTNode node) {
		ArrayList<ASTNode> children = getNodeChildren(node);
		for(ASTNode child : children) {
			ASTEntity childEntity = generateEntityForNode(parent, child);
			if(childEntity!=null) {
				createChildNodes(childEntity, child);
				parent.addChild(childEntity);
			}
		}
	}
	/**
	 * <h1>generateEntityForNode</h1>
	 * @param parent the generated entity's parent
	 * @param node the given node
	 * @return an {@link ASTEntity} with the designated parent which includes information about 
	 * structural children of the given ASTNode
	 */
	protected ASTEntity generateEntityForNode(ASTEntity parent, ASTNode node) {
		ASTEntity entity = null;
		if(node instanceof AbstractTypeDeclaration) {
			int startPosition = ((AbstractTypeDeclaration) node).getStartPosition();
			int endPosition = startPosition + ((AbstractTypeDeclaration) node).getLength();
			String implementation = targetObject.getContent().substring(startPosition, endPosition);
			entity = new ASTEntity("",  ((AbstractTypeDeclaration) node).getName().toString(), "", implementation, startPosition);
		}
		else if(node instanceof MethodDeclaration) {
			int startPosition = ((MethodDeclaration) node).getStartPosition();
			int endPosition = startPosition + ((MethodDeclaration) node).getLength();
			String name = ((MethodDeclaration) node).getName().toString();
			String implementation = targetObject.getContent().substring(startPosition, endPosition);
			String unparemetrizedImplementation = CodeManipulation.removeComments(implementation).replaceAll("(\\<.*\\>)", "").replaceAll("\n", " ").replaceAll("\\s+", " ");
			int typeDeclarationEnd = unparemetrizedImplementation.indexOf(" "+name);
			String returnType = typeDeclarationEnd==-1?"constructor":unparemetrizedImplementation.substring(0,typeDeclarationEnd);
			String[] returnTypes = returnType.split("\\s+");
			returnType = returnTypes[returnTypes.length-1];
			if(returnType.equals("public") || returnType.equals("private") || returnType.equals("protected"))
				returnType = "constructor";
			if(implementation.contains("<"+returnType+">"))
				returnType = "Variable";
			int bodyStart = unparemetrizedImplementation.indexOf("{");
			while(bodyStart!=-1  && unparemetrizedImplementation.substring(bodyStart).startsWith("{@"))
				bodyStart = unparemetrizedImplementation.indexOf("{", bodyStart+1);
			String bodyImplementation = bodyStart==-1?"":unparemetrizedImplementation.substring(bodyStart);
			entity = new ASTEntity(returnType, name, "", bodyImplementation, startPosition);
			{
				//add argument to structure
				String argumentDeclaration = bodyStart==-1?unparemetrizedImplementation:unparemetrizedImplementation.substring(0,bodyStart);
				//System.out.println(argumentDeclaration);//TODO: occasionally, some small comments slip by
				if(argumentDeclaration.lastIndexOf(")")!=-1)
					argumentDeclaration = argumentDeclaration.substring(argumentDeclaration.indexOf("(")+1, argumentDeclaration.lastIndexOf(")")).trim();
				else
					argumentDeclaration = "";
				if(!argumentDeclaration.isEmpty()) {
					for(String argument : argumentDeclaration.split("\\,")) {
						String[] arg = argument.trim().split("\\s+");
						ASTEntity argumentEntity;
						String argumentName = "";
						String argumentType = "";
						if(arg.length==1)
							argumentType = arg[0];
						else if(arg.length>=2) {
							argumentType = arg[arg.length-2];
							argumentName = arg[arg.length-1];
						}
						else
							throw new RuntimeException("Malformed function declaration");
						if(implementation.contains("<"+argumentType+">"))
							argumentType = "Variable";
						argumentEntity = new ASTEntity(argumentType, argumentName, "", "", -1);//TODO: better recognize @param here
						entity.addChild(argumentEntity);
					}
				}
			}
		}
		else if(node instanceof Comment) {
			int startPosition = node.getStartPosition();
			int endPosition = startPosition + node.getLength();
			String commentText = targetObject.getContent().substring(startPosition, endPosition);
			int specialJavadoc = commentText.lastIndexOf("@");
			while(specialJavadoc!=-1) {
				String tmpText = commentText.substring(specialJavadoc);
				if(tmpText.startsWith("@author") || tmpText.startsWith("@throws")) {
					commentText = commentText.substring(0, specialJavadoc);
				}
				else if(tmpText.startsWith("@return")) {
					commentText = commentText.substring(0, specialJavadoc);
				}
				else if(tmpText.startsWith("@param ")) {
					String paramComment =  commentText.substring(specialJavadoc).substring(7);
					int firstSpace = paramComment.indexOf(' ');
					if(firstSpace!=-1) {
						String name = paramComment.substring(0, firstSpace).trim();
						String comment = analysis.code.CleanComments.clean(paramComment.substring(firstSpace).trim());
						if(!comment.isEmpty()) {
							for(Node child : parent.getChildren()) {
								if(child.getName().equals(name)) {
									child.updateComments(child.getComments()+comment);
									comment = "";
									break;
								}
							}
							//if(!comment.isEmpty())
								//System.err.println("Could not find child "+name+" for declaration of "+parent.getStackTrace());//TODO: fix that type parameter does not match
						}
					}
					commentText = commentText.substring(0, specialJavadoc);
				}
				specialJavadoc = commentText.lastIndexOf(commentText, specialJavadoc-1);
			}
			
			
			commentText = analysis.code.CleanComments.clean(commentText);
			parent.updateComments(parent.getComments()+commentText);
		}
		if(entity!=null)
			entityNodes.put(entity, node);
		return entity;
	}
	
	@Override
	public String toString() {
		return rootEntity.toString();
	}
	
	/**
	 * <h1>extractStructure</h1>
	 * @return the generated root {@link ASTEntity} of the AST structure managed by the {@link ASTEntityBuilder} instance.
	 */
	public ASTEntity extractStructure() {
		generateAST();
		return rootEntity;
	}

	/**
	 * <h1>appendComments</h1>
	 * Appends given comments to a given entity's source code (the entity retain its own stored comments, since this
	 * only affects the source code). To remove overhead, due to multiple edits,
	 * appended comment changes are then reflected on the source code only after 
	 * {@link #applyCommentAppends} is called.
	 * @param target the target {@link ASTEntity} (if this entity is not managed by the {@link ASTEntityBuilder} instance,
	 * 	then the builder is searched for a suitable node to append to)
	 * @param comments the comments to be appended to entity's source
	 * @throws Exception if the builder does contains neither the target entity nor a similar one
	 * @see #applyCommentAppends()
	 */
	@SuppressWarnings("unchecked")
	public void appendComments(ASTEntity target, String comments) throws Exception {
		if(node==null)
			generateAST();
		//comments = target.checkGeneratedComments(comments);
		ASTNode entityNode = entityNodes.get(target);
		if(entityNode==null)
			for(ASTEntity similarNode : entityNodes.keySet()) 
				if(similarNode.getPositionalId()==target.getPositionalId() && target.getPositionalId()!=-1)
					entityNode = entityNodes.get(similarNode);
		if(entityNode==null)
			throw new RuntimeException("Builder does not contain target entity");
		//generate javadoc
		Javadoc docComment = ((CompilationUnit)node).getAST().newJavadoc();
		for(String line : comments.split("\\n")) {
			TagElement tag = docComment.getAST().newTagElement();
			TextElement text = docComment.getAST().newTextElement();
			tag.fragments().add(text);
			text.setText(line);
			docComment.tags().add(tag);
		}
		//
		((BodyDeclaration) entityNode).setJavadoc(docComment);
	}
	/**
	 * <h1>applyCommentAppends</h1>
	 * Changes the source code of the {@link ClassObject} managed by the {@link ASTEntityBuilder} instance
	 * to reflect comment changes suggested by {@link #appendComments}.<br/>
	 * <b>Calling this method does not affect comments stored in {@link ASTEntity} instances.</b>
	 * @see #appendComments(ASTEntity, String)
	 */
	public void applyCommentAppends() {
		targetObject.setContent(node.toString());
		//generateAST();
	}
	/**
	 * <h1>getAllComments</h1>
	 * @return builds a text containing all comments related to the source code
	 */
	public String getAllComments() {
		StringBuilder comments = new StringBuilder();
		for(Node entity : extractStructure().collapse())
			comments.append(entity.getComments()).append("\n");
		return comments.toString();
	}
}
