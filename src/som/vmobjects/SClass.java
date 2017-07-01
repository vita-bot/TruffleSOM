/**
 * Copyright (c) 2009 Michael Haupt, michael.haupt@hpi.uni-potsdam.de
 * Software Architecture Group, Hasso Plattner Institute, Potsdam, Germany
 * http://www.hpi.uni-potsdam.de/swa/
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package som.vmobjects;

import java.lang.reflect.Constructor;
import java.util.HashMap;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.ObjectType;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.ValueProfile;

import som.primitives.Primitives;
import som.vm.Universe;
import som.vm.constants.Nil;
import som.vmobjects.SInvokable.SPrimitive;


/**
 * Provides accessors for the objects encoded as DynamicObjects.
 */
public final class SClass {
  private final SSymbol  superclass;
  private final SSymbol  name;
  private final SSymbol  instanceFields;
  private final SSymbol  instanceInvokables;
  private final Universe universe;

  public SClass(final Universe universe) {
    this.universe = universe;
    this.superclass = universe.superclass;
    this.name = universe.name;
    this.instanceFields = universe.instanceFields;
    this.instanceInvokables = universe.instanceInvokables;

    initClassShape = createClassShape(null);
    initClassFactory = initClassShape.createFactory();
  }

  private static final SClassObjectType SCLASS_TYPE = new SClassObjectType();

  private static final Object INVOKABLES_TABLE = new Object();
  private static final Object OBJECT_FACTORY   = new Object();

  // class which get's its own class set only later (to break up cyclic dependencies)
  private final Shape                initClassShape;
  private final DynamicObjectFactory initClassFactory;

  private Shape createClassShape(final DynamicObject clazz) {
    return SObject.LAYOUT.createShape(SCLASS_TYPE, clazz)
                         .defineProperty(superclass, Nil.nilObject, 0)
                         .defineProperty(INVOKABLES_TABLE, new HashMap<SSymbol, SInvokable>(),
                             0)
                         .defineProperty(OBJECT_FACTORY, SObject.NIL_DUMMY_FACTORY, 0);
  }

  public DynamicObject createWithoutClass() {
    CompilerAsserts.neverPartOfCompilation("Class creation");
    DynamicObject clazz = initClassFactory.newInstance(
        Nil.nilObject, // SUPERCLASS
        new HashMap<SSymbol, SInvokable>(), // INVOKABLES_TABLE
        SObject.NIL_DUMMY_FACTORY); // OBJECT_FACTORY, temporary value
    return clazz;
  }

  public DynamicObject create(final DynamicObject clazzClazz) {
    CompilerAsserts.neverPartOfCompilation("Class creation");
    Shape clazzShape = createClassShape(clazzClazz);
    DynamicObjectFactory clazzFactory = clazzShape.createFactory();

    internalSetObjectFactory(clazzClazz, clazzFactory);

    DynamicObject clazz = clazzFactory.newInstance(
        Nil.nilObject, // SUPERCLASS
        new HashMap<SSymbol, SInvokable>(), // INVOKABLES_TABLE
        SObject.NIL_DUMMY_FACTORY); // OBJECT_FACTORY, temporary value

    Shape objectShape = SObject.createObjectShapeForClass(clazz);
    internalSetObjectFactory(clazz, objectShape.createFactory());

    return clazz;
  }

  private static void internalSetObjectFactory(final DynamicObject clazz,
      final DynamicObjectFactory factory) {
    Shape shape = clazz.getShape();
    Property property = shape.getProperty(OBJECT_FACTORY);
    property.setInternal(clazz, factory);
    assert shape == clazz.getShape();
  }

  public static void internalSetClass(final DynamicObject obj,
      final DynamicObject clazzClazz) {
    assert obj.getShape().getObjectType() == SCLASS_TYPE;
    CompilerAsserts.neverPartOfCompilation("SObject.setClass");
    assert obj != null;
    assert clazzClazz != null;

    // It is too expensive to support this at the moment
    // assert !Universe.current().objectSystemInitialized : "This should really only be used
    // during initialization of object system";

    Shape withoutClass = obj.getShape();
    Shape withClass = withoutClass.createSeparateShape(clazzClazz);

    obj.setShapeAndGrow(withoutClass, withClass);
    DynamicObjectFactory clazzFactory = withClass.createFactory();
    internalSetObjectFactory(clazzClazz, clazzFactory);
  }

  public static DynamicObjectFactory getFactory(final DynamicObject clazz) {
    assert clazz.getShape().getObjectType() == SCLASS_TYPE;
    return (DynamicObjectFactory) clazz.get(OBJECT_FACTORY);
  }

  public static boolean isSClass(final DynamicObject obj) {
    return obj.getShape().getObjectType() == SCLASS_TYPE;
  }

  // TODO: figure out whether this is really the best way for doing guards
  // there is the tradeoff with having two different hierarchies of shapes
  // for SClass and SObject. But, might not be performance critical in
  // either case
  private static final class SClassObjectType extends ObjectType {
    @Override
    public String toString() {
      return "SClass";
    }

    @Override
    public ForeignAccess getForeignAccessFactory(final DynamicObject obj) {
      return ForeignAccess.create(new ForeignAccess.Factory() {

        @Override
        public boolean canHandle(final TruffleObject obj) {
          return true;
        }

        @Override
        public CallTarget accessMessage(final Message tree) {
          throw UnsupportedMessageException.raise(tree);
        }
      });
    }
  }

  // TODO: combine setters that are only used for initialization

  public Object getSuperClass(final DynamicObject classObj) {
    CompilerAsserts.neverPartOfCompilation("optimize caller");
    return classObj.get(superclass);
  }

  public void setSuperClass(final DynamicObject classObj, final DynamicObject value) {
    CompilerAsserts.neverPartOfCompilation("should only be used during class initialization");
    // classObj.set(SUPERCLASS, value);
    classObj.define(superclass, value);
  }

  public boolean hasSuperClass(final DynamicObject classObj) {
    return getSuperClass(classObj) != Nil.nilObject;
  }

  public SSymbol getName(final DynamicObject classObj) {
    CompilerAsserts.neverPartOfCompilation("optimize caller");
    return (SSymbol) classObj.get(name);
  }

  public void setName(final DynamicObject classObj, final SSymbol value) {
    CompilerAsserts.neverPartOfCompilation("should only be used during class initialization");
    // classObj.set(NAME, value);
    classObj.define(name, value);
  }

  public SArray getInstanceFields(final DynamicObject classObj) {
    return (SArray) classObj.get(instanceFields);
  }

  public void setInstanceFields(final DynamicObject classObj, final SArray fields) {
    CompilerAsserts.neverPartOfCompilation("should only be used during class initialization");
    // classObj.set(INSTANCE_FIELDS, fields);
    classObj.define(instanceFields, fields);
  }

  public SArray getInstanceInvokables(final DynamicObject classObj) {
    return (SArray) classObj.get(instanceInvokables);
  }

  public void setInstanceInvokables(final DynamicObject classObj, final SArray value) {
    CompilerAsserts.neverPartOfCompilation("should only be used during class initialization");
    // classObj.set(INSTANCE_INVOKABLES, value);
    classObj.define(instanceInvokables, value);

    // Make sure this class is the holder of all invokables in the array
    for (int i = 0; i < getNumberOfInstanceInvokables(classObj); i++) {
      getInstanceInvokable(classObj, i).setHolder(classObj);
    }
  }

  private static final ValueProfile storageType = ValueProfile.createClassProfile();

  public int getNumberOfInstanceInvokables(final DynamicObject classObj) {
    // Return the number of instance invokables in this class
    return getInstanceInvokables(classObj).getObjectStorage(storageType).length;
  }

  public SInvokable getInstanceInvokable(final DynamicObject classObj, final int index) {
    return (SInvokable) getInstanceInvokables(classObj).getObjectStorage(storageType)[index];
  }

  public void setInstanceInvokable(final DynamicObject classObj, final int index,
      final SInvokable value) {
    CompilerAsserts.neverPartOfCompilation();
    // Set this class as the holder of the given invokable
    value.setHolder(classObj);

    getInstanceInvokables(classObj).getObjectStorage(storageType)[index] = value;

    HashMap<SSymbol, SInvokable> invokablesTable = getInvokablesTable(classObj);

    if (invokablesTable.containsKey(value.getSignature())) {
      invokablesTable.put(value.getSignature(), value);
    }
  }

  @SuppressWarnings("unchecked")
  private static HashMap<SSymbol, SInvokable> getInvokablesTable(
      final DynamicObject classObj) {
    return (HashMap<SSymbol, SInvokable>) classObj.get(INVOKABLES_TABLE);
  }

  @TruffleBoundary
  public SInvokable lookupInvokable(final DynamicObject classObj, final SSymbol selector) {
    SInvokable invokable;
    HashMap<SSymbol, SInvokable> invokablesTable = getInvokablesTable(classObj);

    // Lookup invokable and return if found
    invokable = invokablesTable.get(selector);
    if (invokable != null) {
      return invokable;
    }

    // Lookup invokable with given signature in array of instance invokables
    for (int i = 0; i < getNumberOfInstanceInvokables(classObj); i++) {
      // Get the next invokable in the instance invokable array
      invokable = getInstanceInvokable(classObj, i);

      // Return the invokable if the signature matches
      if (invokable.getSignature() == selector) {
        invokablesTable.put(selector, invokable);
        return invokable;
      }
    }

    // Traverse the super class chain by calling lookup on the super class
    if (hasSuperClass(classObj)) {
      invokable = lookupInvokable((DynamicObject) getSuperClass(classObj), selector);
      if (invokable != null) {
        invokablesTable.put(selector, invokable);
        return invokable;
      }
    }

    // Invokable not found
    return null;
  }

  public int lookupFieldIndex(final DynamicObject classObj, final SSymbol fieldName) {
    // Lookup field with given name in array of instance fields
    for (int i = getNumberOfInstanceFields(classObj) - 1; i >= 0; i--) {
      // Return the current index if the name matches
      if (fieldName == getInstanceFieldName(classObj, i)) {
        return i;
      }
    }

    // Field not found
    return -1;
  }

  private boolean addInstanceInvokable(final DynamicObject classObj, final SInvokable value) {
    CompilerAsserts.neverPartOfCompilation("SClass.addInstanceInvokable(.)");

    // Add the given invokable to the array of instance invokables
    for (int i = 0; i < getNumberOfInstanceInvokables(classObj); i++) {
      // Get the next invokable in the instance invokable array
      SInvokable invokable = getInstanceInvokable(classObj, i);

      // Replace the invokable with the given one if the signature matches
      if (invokable.getSignature() == value.getSignature()) {
        setInstanceInvokable(classObj, i, value);
        return false;
      }
    }

    // Append the given method to the array of instance methods
    setInstanceInvokables(
        classObj, getInstanceInvokables(classObj).copyAndExtendWith(value));
    return true;
  }

  public void addInstancePrimitive(final DynamicObject classObj, final SInvokable value,
      final boolean displayWarning) {
    if (addInstanceInvokable(classObj, value) && displayWarning) {
      Universe.print("Warning: Primitive " + value.getSignature().getString());
      Universe.println(" is not in class definition for class "
          + getName(classObj).getString());
    }
  }

  public SSymbol getInstanceFieldName(final DynamicObject classObj, final int index) {
    return (SSymbol) getInstanceFields(classObj).getObjectStorage(storageType)[index];
  }

  public int getNumberOfInstanceFields(final DynamicObject classObj) {
    return getInstanceFields(classObj).getObjectStorage(storageType).length;
  }

  private boolean includesPrimitives(final DynamicObject clazz) {
    CompilerAsserts.neverPartOfCompilation("SClass.includesPrimitives(.)");
    // Lookup invokable with given signature in array of instance invokables
    for (int i = 0; i < getNumberOfInstanceInvokables(clazz); i++) {
      // Get the next invokable in the instance invokable array
      if (getInstanceInvokable(clazz, i) instanceof SPrimitive) {
        return true;
      }
    }
    return false;
  }

  public boolean hasPrimitives(final DynamicObject classObj) {
    return includesPrimitives(classObj) || includesPrimitives(SObject.getSOMClass(classObj));
  }

  public void loadPrimitives(final DynamicObject classObj, final boolean displayWarning) {
    CompilerAsserts.neverPartOfCompilation();

    // Compute the class name of the Java(TM) class containing the
    // primitives
    String className = "som.primitives." + getName(classObj).getString() + "Primitives";

    // Try loading the primitives
    try {
      Class<?> primitivesClass = Class.forName(className);
      try {
        Constructor<?> ctor = primitivesClass.getConstructor(boolean.class, Universe.class);
        ((Primitives) ctor.newInstance(displayWarning, universe)).installPrimitivesIn(
            classObj);
      } catch (Exception e) {
        Universe.errorExit("Primitives class " + className
            + " cannot be instantiated");
      }
    } catch (ClassNotFoundException e) {
      if (displayWarning) {
        Universe.println("Primitives class " + className + " not found");
      }
    }
  }

  //
  // @Override
  // public String toString() {
  // return "Class(" + getName().getString() + ")";
  // }

}
