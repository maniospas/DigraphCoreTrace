package analysis.compiled;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.*;

public class MethodVisitor extends EmptyVisitor {

    JavaClass visitedClass;
    private MethodGen mg;
    private ConstantPoolGen cp;
    private String format;
    private String formatCall;
    private CompiledProjectImporter project;

    public MethodVisitor(MethodGen m, JavaClass jc, CompiledProjectImporter project) {
    	this.project = project;
        visitedClass = jc;
        mg = m;
        cp = mg.getConstantPool();
        //format = visitedClass.getClassName() + "." + mg.getName() + "(" + argumentList(mg.getArgumentTypes()) + ")";
        formatCall = "%s.%s(%s)";
        format = String.format(formatCall, visitedClass.getClassName(), mg.getName(), argumentList(mg.getArgumentTypes()));
    }

    private String argumentList(Type[] arguments) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arguments.length; i++) {
            if (i != 0) {
                sb.append(",");
            }
            sb.append(arguments[i].toString());
        }
        return sb.toString();
    }

    public void start() {
        if (mg.isAbstract() || mg.isNative())
            return;
        for (InstructionHandle ih = mg.getInstructionList().getStart(); ih != null; ih = ih.getNext()) {
            Instruction i = ih.getInstruction();
            if (!visitInstruction(i))
                i.accept(this);
        }
    }

    private boolean visitInstruction(Instruction i) {
        short opcode = i.getOpcode();
        return ((InstructionConst.getInstruction(opcode) != null)
                && !(i instanceof ConstantPushInstruction) 
                && !(i instanceof ReturnInstruction));
    }

    @Override
    public void visitINVOKEVIRTUAL(INVOKEVIRTUAL i) {
        project.registerVisit(format,String.format(formatCall, i.getReferenceType(cp), i.getMethodName(cp),argumentList(i.getArgumentTypes(cp))));
    }

    @Override
    public void visitINVOKEINTERFACE(INVOKEINTERFACE i) {
        project.registerVisit(format,String.format(formatCall, i.getReferenceType(cp), i.getMethodName(cp),argumentList(i.getArgumentTypes(cp))));
    }

    @Override
    public void visitINVOKESPECIAL(INVOKESPECIAL i) {
        project.registerVisit(format,String.format(formatCall, i.getReferenceType(cp), i.getMethodName(cp),argumentList(i.getArgumentTypes(cp))));
    }

    @Override
    public void visitINVOKESTATIC(INVOKESTATIC i) {
        project.registerVisit(format,String.format(formatCall, i.getReferenceType(cp), i.getMethodName(cp),argumentList(i.getArgumentTypes(cp))));
    }

    @Override
    public void visitINVOKEDYNAMIC(INVOKEDYNAMIC i) {
        project.registerVisit(format,String.format(formatCall, i.getReferenceType(cp), i.getMethodName(cp),argumentList(i.getArgumentTypes(cp))));
    }
}
