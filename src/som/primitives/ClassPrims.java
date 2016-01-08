package som.primitives;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;

import som.primitives.SystemPrims.UnarySystemNode;
import som.vm.Universe;
import som.vmobjects.SAbstractObject;
import som.vmobjects.SArray;
import som.vmobjects.SClass;


public class ClassPrims {

  @GenerateNodeFactory
  @ImportStatic(SClass.class)
  public abstract static class NamePrim extends UnarySystemNode {
    public NamePrim(final Universe universe) {
      super(universe);
    }

    @Specialization(guards = "isSClass(receiver)")
    public final SAbstractObject doSClass(final DynamicObject receiver) {
      CompilerAsserts.neverPartOfCompilation();
      return universe.sclass.getName(receiver);
    }
  }

  @GenerateNodeFactory
  @ImportStatic(SClass.class)
  public abstract static class SuperClassPrim extends UnarySystemNode {
    public SuperClassPrim(final Universe universe) {
      super(universe);
    }

    @Specialization(guards = "isSClass(receiver)")
    public final Object doSClass(final DynamicObject receiver) {
      CompilerAsserts.neverPartOfCompilation();
      return universe.sclass.getSuperClass(receiver);
    }
  }

  @GenerateNodeFactory
  @ImportStatic(SClass.class)
  public abstract static class InstanceInvokablesPrim extends UnarySystemNode {
    public InstanceInvokablesPrim(final Universe universe) {
      super(universe);
    }

    @Specialization(guards = "isSClass(receiver)")
    public final SArray doSClass(final DynamicObject receiver) {
      CompilerAsserts.neverPartOfCompilation();
      return universe.sclass.getInstanceInvokables(receiver);
    }
  }

  @GenerateNodeFactory
  @ImportStatic(SClass.class)
  public abstract static class InstanceFieldsPrim extends UnarySystemNode {
    public InstanceFieldsPrim(final Universe universe) {
      super(universe);
    }

    @Specialization(guards = "isSClass(receiver)")
    public final SArray doSClass(final DynamicObject receiver) {
      CompilerAsserts.neverPartOfCompilation();
      return universe.sclass.getInstanceFields(receiver);
    }
  }
}
