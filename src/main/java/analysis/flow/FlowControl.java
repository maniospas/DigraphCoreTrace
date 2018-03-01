package analysis.flow;

import java.util.ArrayList;

import analysis.code.CodeManipulation;

/**
 * <h1>FlowControl</h1>
 * This class can be used to explore logical flow into a body segment.
 * In particular, it can be used to decompose a series of statements into further bodies,
 * of simple, repetition and conditional statements.
 * @author Manios Krasanakis
 */
public class FlowControl {
	private String contents;
	private FlowControl condition;
	private String type;
	private ArrayList<FlowControl> children = null;
	public FlowControl(String contents) {
		this("", "", contents);
	}
	public FlowControl(String type, String condition, String contents) {
		this.contents = contents.trim();
		this.type = type;
		if(this.contents.startsWith("{"))
			this.contents = this.contents.substring(1, contents.length()-1).trim();
		if(condition.isEmpty())
			this.condition = null;
		else 
			this.condition = new FlowControl("condition", "", condition);
	}
	public String getContents() {
		return contents;
	}
	public void addChild(FlowControl child) {
		children.add(child);
	}
	public String getType() {
		return type;
	}
	public boolean isStatement() {
		return type.isEmpty();
	}
	public boolean isCondition() {
		return type.equals("condition");
	}
	public FlowControl getCondition() {
		return condition;
	}
	public ArrayList<FlowControl> getChildren() {
		if(children==null) {
			children = new ArrayList<FlowControl>();
			detectChildren(contents);
		}
		return children;
	}
	private void detectChildren(String contents) {
		int textPosition = 0;
		String currentParsedText = "";
		while(textPosition<contents.length()) {
			char c = contents.charAt(textPosition);
			if(c=='\n') 
				c = ' ';
			int offset = 1;
			if(c=='(') {
				if(currentParsedText.trim().equals("if") || currentParsedText.trim().equals("while") || currentParsedText.trim().equals("for")) {
					int conditionEnd = CodeManipulation.topLevelIndexOf(contents, ')', textPosition-1);
					int contentsEnd = CodeManipulation.min(CodeManipulation.topLevelIndexOf(contents, '}', conditionEnd+1),CodeManipulation.topLevelIndexOf(contents, ';', conditionEnd));
					if(contentsEnd == -1) {
						contentsEnd = contents.length();
						//System.err.println(contents.substring(textPosition));
						//System.err.println(contents.substring(conditionEnd+1));
					}
					else
						children.add(new FlowControl(currentParsedText.trim(), contents.substring(textPosition+1,conditionEnd), contents.substring(conditionEnd+1, contentsEnd+1)));
					offset = contentsEnd-textPosition+1;
					currentParsedText = "";
				}
				else {
					int parenthesisEnd = CodeManipulation.topLevelIndexOf(contents, ')', textPosition);
					System.out.println(parenthesisEnd+" : "+contents.substring(textPosition));
					currentParsedText += contents.substring(textPosition, parenthesisEnd+1);
					offset = parenthesisEnd-textPosition+1;
				}
			}
			if(c==';') {
				children.add(new FlowControl("","",currentParsedText.trim()));
				currentParsedText = "";
			}
			else if(offset==1) 
				currentParsedText += c;
			if(currentParsedText.trim().equals("try") || currentParsedText.trim().equals("finally")) {
				int blockStart = CodeManipulation.topLevelIndexOf(contents, '{', textPosition);
				int blockEnd = CodeManipulation.topLevelIndexOf(contents, '}', textPosition);
				currentParsedText = "";
				offset = blockEnd-textPosition+1;
				detectChildren(contents.substring(blockStart+1,blockEnd));
			}
			else if(currentParsedText.trim().equals("catch")) {
				int blockEnd = CodeManipulation.topLevelIndexOf(contents, '}', textPosition);
				currentParsedText = "";
				offset = blockEnd-textPosition+1;
			}
			textPosition += offset;
		}
	}
}
