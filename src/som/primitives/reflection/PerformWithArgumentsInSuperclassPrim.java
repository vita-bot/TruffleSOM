package som.primitives.reflection;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;

import som.interpreter.nodes.nary.QuaternaryExpressionNode;
import som.vm.Universe;
import som.vmobjects.SInvokable;
import som.vmobjects.SSymbol;


@GenerateNodeFactory
public abstract class PerformWithArgumentsInSuperclassPrim extends QuaternaryExpressionNode {
  @Child private IndirectCallNode call;
  private final Universe          universe;

  public PerformWithArgumentsInSuperclassPrim(final Universe universe) {
    super(null);
    this.universe = universe;
    call = Truffle.getRuntime().createIndirectCallNode();
  }

  @Specialization
  public final Object doSAbstractObject(final Object receiver, final SSymbol selector,
      final Object[] argArr, final DynamicObject clazz) {
    CompilerAsserts.neverPartOfCompilation(
        "PerformWithArgumentsInSuperclassPrim.doSAbstractObject()");
    SInvokable invokable = universe.sclass.lookupInvokable(clazz, selector);
    return call.call(invokable.getCallTarget(), mergeReceiverWithArguments(receiver, argArr));
  }

  // TODO: remove duplicated code, also in symbol dispatch, ideally removing by optimizing this
  // implementation...
  @ExplodeLoop
  private static Object[] mergeReceiverWithArguments(final Object receiver,
      final Object[] argsArray) {
    Object[] arguments = new Object[argsArray.length + 1];
    arguments[0] = receiver;
    for (int i = 0; i < argsArray.length; i++) {
      arguments[i + 1] = argsArray[i];
    }
    return arguments;
  }
}
