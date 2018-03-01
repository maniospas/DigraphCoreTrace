package analysis.compiled;

import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.EmptyVisitor;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.MethodGen;

/**
 * The simplest of class visitors, invokes the method visitor class for each
 * method found.
 */
public class ClassVisitor extends EmptyVisitor {
    private JavaClass clazz;
    private ConstantPoolGen constants;
    //private String classReferenceFormat;
    private CompiledProjectImporter project;
    
    public ClassVisitor(JavaClass jc, CompiledProjectImporter project) {
    	this.project = project;
        clazz = jc;
        constants = new ConstantPoolGen(clazz.getConstantPool());
        //classReferenceFormat = "class " + clazz.getClassName() + " %s";
    }

    public void visitJavaClass(JavaClass jc) {
    	if(!project.singleTimeVisit(jc.getFileName()))
    		return;
        jc.getConstantPool().accept(this);
        Method[] methods = jc.getMethods();
        for (int i = 0; i < methods.length; i++)
            methods[i].accept(this);
    }

    public void visitConstantPool(ConstantPool constantPool) {
        for (int i = 0; i < constantPool.getLength(); i++) {
            Constant constant = constantPool.getConstant(i);
            if (constant == null)
                continue;
            /*if (constant.getTag() == 7) {
                String referencedClass = constantPool.constantToString(constant);
                System.out.println(String.format(classReferenceFormat, referencedClass));
            }*/
        }
    }

    public void visitMethod(Method method) {
        MethodGen mg = new MethodGen(method, clazz.getClassName(), constants);
        MethodVisitor visitor = new MethodVisitor(mg, clazz, project);
        visitor.start(); 
    }

    public void start() {
        visitJavaClass(clazz);
    }
}
