package som.primitives.reflection;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;

import som.interpreter.nodes.nary.TernaryExpressionNode;
import som.vm.Universe;
import som.vmobjects.SInvokable;
import som.vmobjects.SSymbol;


@GenerateNodeFactory
public abstract class PerformInSuperclassPrim extends TernaryExpressionNode {
  @Child private IndirectCallNode call = Truffle.getRuntime().createIndirectCallNode();
  private final Universe          universe;

  public PerformInSuperclassPrim(final Universe universe) {
    this.universe = universe;
  }

  @Specialization
  public final Object doSAbstractObject(final Object receiver, final SSymbol selector,
      final DynamicObject clazz) {
    CompilerAsserts.neverPartOfCompilation("PerformInSuperclassPrim");
    SInvokable invokable = universe.sclass.lookupInvokable(clazz, selector);
    return call.call(invokable.getCallTarget(), new Object[] {receiver});
  }
}
